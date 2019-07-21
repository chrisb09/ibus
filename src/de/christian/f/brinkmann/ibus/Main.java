package de.christian.f.brinkmann.ibus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.christian.f.brinkmann.ibus.indexing.IndexingDir;
import de.christian.f.brinkmann.ibus.indexing.IndexingEntry;
import de.christian.f.brinkmann.ibus.indexing.IndexingFile;
import de.christian.f.brinkmann.ibus.pngenc.ClassicImageIOEncoder;
import de.christian.f.brinkmann.ibus.pngenc.KeypointPngEncoder;
import de.christian.f.brinkmann.ibus.pngenc.ObjectplanetPngEncoder;
import de.christian.f.brinkmann.ibus.pngenc.PngEncoder;

public class Main {

	static boolean delete = false;
	static long sizeInBytes = 0l;
	static int minSize = 256;
	static int maxSize = 4000;
	static boolean debug = false;
	// static String[] path = new String[0];
	static IndexingDir root;
	static IndexingDir current;
	static File sourcePath;
	private static boolean[] usedIndices;
	private static HashMap<Integer, Set<Integer>> hashCollisions = new HashMap<Integer, Set<Integer>>();
	public static PngEncoder encoder = new ClassicImageIOEncoder();

	public static void main(String[] args) {

		if (args.length < 1) {
			printHelp();
			return;
		}

		for (int i = 1; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("--delete")) {
				delete = true;
			}
			if (args[i].toLowerCase().startsWith("--minsize=")) {
				minSize = Integer.parseInt(args[i].substring(9, args[i].length()));
			}
			if (args[i].toLowerCase().startsWith("--maxsize=")) {
				maxSize = Integer.parseInt(args[i].substring(9, args[i].length()));
			}
			if (args[i].toLowerCase().startsWith("--debug")) {
				debug = true;
			}
			if (args[i].toLowerCase().startsWith("--key")) {
				setKey(args[i].substring(6, args[i].length()));
			}
			if (args[i].toLowerCase().startsWith("--enc")) {
				String n = args[i].substring(6, args[i].length());
				if (n.equalsIgnoreCase("classic")) {
					encoder = new ClassicImageIOEncoder();
					System.out.println("Use classic encoder");
				}
				if (n.equalsIgnoreCase("keypoint")) {
					encoder = new KeypointPngEncoder();
					System.out.println("Use keypoint encoder");
				}
				if (n.equalsIgnoreCase("objectplanet")) {
					encoder = new ObjectplanetPngEncoder();
					System.out.println("Use objectplanet encoder");
				}
			}
		}

		usedIndices = new boolean[0];

		String path = args[0];
		sourcePath = new File(path);
		System.out.println("Using " + sourcePath.getAbsolutePath() + " as file system directory.");
		File indexFile = new File(sourcePath, "index.0.png");
		if (indexFile.exists()) {
			System.out.println("Found existing file system.");
			root = FileSystemFunctions.loadIndexing(sourcePath, 0, null, null);
			if (root == null) {
				System.out.println("Error loading file system. The files might be damaged or require a key.");
				return;
			}
		} else {
			System.out.println("Creating new FS");
			root = new IndexingDir(null, "", 0);
			System.out.println("root:" + root.getSubFiles());
		}

		current = root;

		if (!Crypto.isEncryptionActivated()) {
			System.out.println("Please encrypt your files. Add the --key=X parameter.");
			return;
		}

		console();

		/*
		 * long start = System.currentTimeMillis();
		 * 
		 * System.out.println("Operation completed.");
		 * System.out.println("Time: " + ((System.currentTimeMillis() - start) /
		 * 1000l) + "s"); System.out.println("Data: " + (sizeInBytes / 1000000l)
		 * + "Mbyte"); System.out .println("Rate: " +
		 * (((System.currentTimeMillis() - start) / 1000l) != 0 ? ((sizeInBytes
		 * * 8 / 1000000l) / ((System.currentTimeMillis() - start) / 1000l)) +
		 * " Mbit/s" : "-"));
		 */
	}

	private static void console() {
		boolean loop = true;
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter a command:");
		while (loop) {
			String p = current == root ? "/" : "";
			IndexingDir cpy = current;
			while (cpy.getParent() != null) {
				p = cpy.getName() + "/" + p;
				cpy = cpy.getParent();
			}
			System.out.print(p + ": ");
			String line = sc.nextLine();
			String[] args = getArgs(line);
			String command = args[0];
			String[] tmp = new String[args.length - 1];
			for (int i = 0; i < tmp.length; i++) {
				tmp[i] = args[i + 1];
			}
			args = tmp;

			if (command.equalsIgnoreCase("help") || command.equals("?")) {
				printHelpInConsole();
				continue;
			}
			if (command.equalsIgnoreCase("list") || command.equalsIgnoreCase("ls")) {
				IndexingDir local = current;
				int depth = 0;
				if (args.length == 1) {
					if (args[0].equals("-a") || args[0].equals("-la")) {
						depth = Integer.MAX_VALUE;
					} else {
						local = getPath(local, args[0]);
					}
				} else if (args.length == 2) {
					if (args[0].equals("-n") || args[0].equals("-depth")) {
						depth = Integer.parseInt(args[1]);
					}
				} else if (args.length > 2) {
					System.out.println("ln [-a/-n X]");
					continue;
				}
				ArrayList<String> list = FileSystemFunctions.printIndexingStructure(local, 0, depth);
				for (String s : list) {
					System.out.println(s);
				}
				continue;
			}
			if (command.equalsIgnoreCase("mkdir")) {
				IndexingDir local = current;
				if (args.length == 1) {
					local = getPath(local, args[0]);
					System.out.println("Path '" + args[0] + "' created");
				} else {
					System.out.println("Use 'mkdir <path>'");
					continue;
				}
				continue;
			}
			if (command.equalsIgnoreCase("cd")) {
				if (args.length == 1) {
					current = getPath(current, args[0]);
					continue;
				} else {
					System.out.println("Use 'cd <path>'");
					continue;
				}
			}
			if (command.equalsIgnoreCase("rm") || command.equalsIgnoreCase("remove")) {
				String path = "";
				boolean recursive = false;
				if (args.length == 2) {
					if (!args[0].equals("-r")) {
						System.out.println("rm [-r] <path>");
						continue;
					}
					recursive = true;
					path = args[1];
				} else if (args.length != 1) {
					System.out.println("rm [-r] <path>");
					continue;
				} else {
					path = args[0];
				}
				Pattern pattern = Pattern.compile(path);
				@SuppressWarnings("unchecked")
				HashSet<IndexingEntry> cl = (HashSet<IndexingEntry>) current.getSubFiles().clone();
				for (IndexingEntry en : cl) {
					Matcher matcher = pattern.matcher(en.getName());
					if (matcher.find()) {
						if (en instanceof IndexingFile || recursive) {
							FileSystemFunctions.delete(en, sourcePath);
						}
					}
				}
				continue;
			}
			if (command.equals("mv") || command.equals("move")) {
				if (args.length != 2) {
					System.out.println("mv/move <pathA> <pathB>");
					continue;
				}
				for (IndexingEntry en : getEntries(current, args[0])) {
					IndexingEntry target = getEntry(current, args[1], (en instanceof IndexingFile));
					if (target instanceof IndexingFile && en instanceof IndexingFile) {
						FileSystemFunctions.move((IndexingFile) en, (IndexingFile) target, sourcePath);
					} else if (target instanceof IndexingDir && en instanceof IndexingDir) {
						FileSystemFunctions.move((IndexingDir) en, (IndexingDir) target, sourcePath);
					} else {
						System.out.println("Filetypes not aligned...");
					}
				}
				continue;
			}
			if (command.equals("cp") || command.equals("copy")) {
				if (args.length != 2) {
					System.out.println("cp/copy <pathA> <pathB>");
					continue;
				}
				for (IndexingEntry en : getEntries(current, args[0])) {
					IndexingEntry target = getEntry(current, args[1], (en instanceof IndexingFile));
					if (target instanceof IndexingFile && en instanceof IndexingFile) {
						FileSystemFunctions.copy((IndexingFile) en, (IndexingFile) target, sourcePath);
					} else if (target instanceof IndexingDir && en instanceof IndexingDir) {
						FileSystemFunctions.copy((IndexingDir) en, (IndexingDir) target, sourcePath);
					} else {
						System.out.println("Filetypes not aligned...");
					}
				}
				continue;
			}
			if (command.equals("c") || command.equals("count")) {
				IndexingDir local = current;
				if (args.length == 1) {
					local = getPath(local, args[0]);
				} else if (args.length > 0) {
					System.out.println("count/c [path]");
					continue;
				}
				long[] amount = FileSystemFunctions.getAmount(local);
				System.out.println("files: " + amount[0] + "   folders: " + amount[1]);
				continue;
			}
			if (command.equals("du") || command.equals("size")) {
				IndexingDir local = current;
				if (args.length == 1) {
					local = getPath(local, args[0]);
				} else if (args.length > 0) {
					System.out.println("du/size [path]");
					continue;
				}
				System.out.println("size: " + Tool.readableFileSize(FileSystemFunctions.getSize(local)));
				continue;
			}
			if (command.equals("decode")) {
				if (args.length != 2) {
					System.out.println("Please give a sourcePath and targetPath: decode <sourcePath> <targetPath>");
					continue;
				}
				File targetPath = new File(args[1]);
				if (!targetPath.exists()) {
					targetPath.mkdirs();
				}
				Metric.startMetric();
				for (IndexingEntry en : getEntries(current, args[0])) {
					Decoder.decodeEntry(en, targetPath, sourcePath);
				}
				System.out.println(Metric.stopMetric());
				System.out.println("Decoding done.");
				continue;
			}
			if (command.equals("encode")) {
				if (args.length < 1) {
					System.out.println("Please give a sourcePath: encode <sourcePath> [targetPath]");
					continue;
				}
				if (args.length > 2) {
					System.out.println("Too many arguments: encode <sourcePath> [targetPath]");
					continue;
				}
				File sPath = new File(args[0]);
				IndexingDir local = current;
				if (args.length == 2) {
					local = getPath(local, args[1]);
				}
				if (!sPath.exists()) {
					System.out.println("No file found: '" + sPath.getAbsolutePath() + "'");
					continue;
				}
				
				Encoder.startEncodeFile(sPath, sourcePath, local);
				
				System.out.println("Encoding done.");
				continue;
			}
			if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("quit") || command.equalsIgnoreCase("q")) {
				System.out.println("Exiting...");
				break;
			}

			System.out.println("Command '" + line + "' unknown. Please use \"help\" for information.");
		}
		sc.close();
		return;
	}

	private static String[] getArgs(String line) {
		ArrayList<String> list = new ArrayList<String>();
		char[] chars = line.toCharArray();
		String text = "";
		boolean inQuotes = false;
		boolean lastSuper = false;
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (c == '\\' && !lastSuper) {
				lastSuper = true;
				continue;
			} else if (c == '"' && !lastSuper) {
				if (!inQuotes) {
					inQuotes = true;
					continue;
				} else {
					inQuotes = false;
					list.add(text);
					text = "";
					continue;
				}
			} else if (c == ' ' && !lastSuper && !inQuotes) {
				if (text.length() == 0) {
					continue;
				}
				list.add(text);
				text = "";
				continue;
			}
			text += c;
			lastSuper = false;
		}
		if (text != "" || list.size() == 0) {
			list.add(text);
		}
		return list.toArray(new String[list.size()]);
	}

	private static IndexingEntry getEntry(IndexingDir current, String path, boolean file) {
		int a = -1;
		char[] chars = path.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == '/') {
				a = i;
			}
		}
		String p = "";
		String n = "";
		if (a != -1) {
			p = path.substring(0, a);
			if (a == chars.length - 1) {
				n = "";
			} else {
				n = path.substring(a + 1);
			}
		} else {
			n = path + "";
		}

		IndexingDir local = getPath(current, p);
		if (n.length() == 0) {
			return local;
		}
		for (IndexingEntry en : local.getSubFiles()) {
			if (en.getName().equals(n) && (en instanceof IndexingFile == file)) {
				return en;
			}
		}
		if (file) {
			int collisions = Main.getFreeHashCollisionNumber(n.hashCode());
			Main.addHashCollisionNumber(n.hashCode(), collisions);
			return new IndexingFile(local, n, n.hashCode(), collisions, new int[0], 0l);
		} else {
			IndexingDir dir = new IndexingDir(local, n, Main.getAndUseNextFreeIndex());
			local.getSubFiles().add(dir);
			FileSystemFunctions.saveIndexing(sourcePath, local);
			return dir;
		}
	}

	private static IndexingEntry[] getEntries(IndexingDir current, String path) {
		int a = -1;
		char[] chars = path.toCharArray();
		for (int i = 0; i < chars.length - 1; i++) {
			if (chars[i] == '/') {
				a = i;
			}
		}
		String p = "";
		String n = "";
		if (a != -1) {
			p = path.substring(0, a);
			n = path.substring(a + 1);
		} else {
			n = path + "";
		}
		ArrayList<IndexingEntry> entries = new ArrayList<IndexingEntry>();
		IndexingDir local = getPath(current, p);
		Pattern pattern = Pattern.compile(n);
		for (IndexingEntry en : local.getSubFiles()) {
			Matcher m = pattern.matcher(en.getName());
			if (m.find()) {
				entries.add(en);
			}
		}
		return entries.toArray(new IndexingEntry[entries.size()]);
	}

	private static IndexingDir getPath(IndexingDir current, String path) {
		String restPath = path + "";
		IndexingDir currentPath = current;
		if (restPath.startsWith("/")) {
			currentPath = Main.root;
			restPath = restPath.substring(1);
		} else if (restPath.startsWith("./")) {
			restPath = restPath.substring(2);
		}
		if (restPath.length() == 0) {
			return currentPath;
		}
		String[] parts = restPath.split("/");
		for (String s : parts) {
			if (s.equals("..")) {
				if (currentPath.getParent() != null) {
					currentPath = currentPath.getParent();
				}
			} else {
				boolean exist = false;
				for (IndexingEntry en : currentPath.getSubFiles()) {
					if (en.getName().equals(s) && en instanceof IndexingDir) {
						currentPath = (IndexingDir) en;
						exist = true;
						break;
					}
				}
				if (!exist) {
					int id = getAndUseNextFreeIndex();
					IndexingDir sub = new IndexingDir(currentPath, s, id);
					currentPath.getSubFiles().add(sub);
					FileSystemFunctions.saveIndexing(sourcePath, currentPath);
					FileSystemFunctions.saveIndexing(sourcePath, sub);
				}
			}
		}
		return currentPath;
	}

	private static void printHelpInConsole() {
		System.out.println("Commands:");
		System.out.println("   cd <path>: change working directory. Use .. to go to parent.");
		System.out.println("   list/ls [-a/-n X]: lists all files and directories in the current path/all subfiles/until depth X");
		System.out.println("   encode <sourcePath> [targetPath]: encode file(s) from the real file system and add them in the fake file system");
		System.out.println("       sourcePath is in the real file system, targetPath an optional subpath in the fake file system");
		System.out.println("   decode <sourcePath> <targetPath>: decode file(s) from fake file system into real file system");
		System.out.println("       targetPath is in the real file system, sourcePath is a subpath in the fake file system");
		System.out.println("       *sourcePath uses regex matching*");
		System.out.println("   mkdir <path>: make a directory");
		System.out.println("   rm/remove [-r] <path>: delete a file/folder");
		System.out.println("   mv/move <pathA> <pathB>: move a file or folder");
		System.out.println("   cp/copy <pathA> <pathB>: copy a file or folder");
		System.out.println("   count [path]: count all files in the current directory/path");
		System.out.println("   du/size [path]: calculates the size of the current path/given path");
		System.out.println("   exit/quit/q: leave program");
	}

	private static void printHelp() {
		System.out.println("Parameter: <path> [--minSize=X] [--key=Y]");
		System.out.println(" where X is an Integer and Y is a password");
	}

	private static void setKey(String key) {
		System.out.println("Using AES-" + (Math.min(key.length(), 16) * 8) + "-bit key");
		if (key.length() > 16) {
			System.out.println("[Warning] Only the first 16 characters are used for AES-128bit encryption!");
		}
		byte[] k = key.getBytes();
		byte[] n = new byte[16];
		for (int i = 0; i < Math.min(k.length, n.length); i++) {
			n[i] = k[i];
		}
		try {
			Crypto.setKey(n);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setIndexUsed(int index, boolean used) {
		if (index < 0) {
			return;
		}
		if (index >= usedIndices.length && used) {
			boolean[] tmp = new boolean[index + 1];
			for (int i = 0; i < usedIndices.length; i++) {
				tmp[i] = usedIndices[i];
			}
			usedIndices = tmp;
		}
		usedIndices[index] = used;
	}

	static boolean isIndexUsed(int index) {
		if (index < 0 || index >= usedIndices.length) {
			return false;
		}
		return usedIndices[index];
	}

	static int getAndUseNextFreeIndex() {
		for (int i = 0; i < usedIndices.length; i++) {
			if (!usedIndices[i]) {
				usedIndices[i] = true;
				return i;
			}
		}
		setIndexUsed(usedIndices.length, true);
		return usedIndices.length - 1;
	}

	static int getFreeHashCollisionNumber(int hash) {
		Set<Integer> set = hashCollisions.get(hash);
		if (set == null) {
			return 0;
		}
		for (int i = 0; i < set.size(); i++) {
			if (!set.contains(i)) {
				return i;
			}
		}
		return set.size();
	}

	static void addHashCollisionNumber(int hash, int number) {
		if (!hashCollisions.containsKey(hash)) {
			hashCollisions.put(hash, new TreeSet<Integer>());
		}
		hashCollisions.get(hash).add(number);
	}

	static void removeHashCollision(int hash, int number) {
		if (hashCollisions.containsKey(hash)) {
			hashCollisions.get(hash).remove(number);
		}
	}

}
