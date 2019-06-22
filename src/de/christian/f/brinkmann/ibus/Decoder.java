package de.christian.f.brinkmann.ibus;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import de.christian.f.brinkmann.ibus.indexing.IndexingDir;
import de.christian.f.brinkmann.ibus.indexing.IndexingEntry;
import de.christian.f.brinkmann.ibus.indexing.IndexingFile;

public class Decoder {

	static File[] decodeFile(File source, File targetDir) {
		Main.sizeInBytes += source.length();
		String name = source.getName();
		String[] parts = name.split(Pattern.quote("."));
		String packetNr = parts[parts.length - 2];
		String emptyBytes = parts[parts.length - 3];
		String paddingByteString = parts[parts.length - 4];
		String recursionDepthString = parts[parts.length - 5];
		int recursionDepth = Integer.parseInt(recursionDepthString);
		String[] paths = new String[recursionDepth];
		for (int i = 0; i < recursionDepth; i++) {
			paths[i] = parts[parts.length - (5 + recursionDepth - i)];
		}
		File targetPath = targetDir;
		if (!targetPath.exists()) {
			targetPath.mkdirs();
		}
		for (String p : paths) {
			targetPath = new File(targetPath, p);
			if (!targetPath.exists()) {
				targetPath.mkdirs();
			}
		}
		String origName = "";
		for (int i = 0; i < parts.length - (recursionDepth + 5); i++) {
			if (i == 0) {
				origName = parts[i];
			} else {
				origName += "." + parts[i];
			}
		}
		int emptyB = Integer.parseInt(emptyBytes);
		int paddingBytes = Integer.parseInt(paddingByteString);
		if (packetNr.equals("_")) {
			// One packet
			try {
				byte[] data = ImageReader.readImage(ImageIO.read(source));
				byte[] dataCopy = new byte[data.length - emptyB];
				for (int i = 0; i < dataCopy.length; i++) {
					dataCopy[i] = data[i];
				}
				if (Crypto.isEncryptionActivated()) {
					try {
						dataCopy = Crypto.decrypt(dataCopy);
						if (paddingBytes != 0) {
							byte[] n = new byte[dataCopy.length - paddingBytes];
							for (int i = 0; i < dataCopy.length - paddingBytes; i++) {
								n[i] = dataCopy[i];
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				File t = new File(targetPath, origName);
				FileIO.writeFileAsBytes(t, dataCopy);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// Multiple packets
			int pId = Integer.parseInt(packetNr);
			if (emptyB != 0) {
				File[] files = new File[pId + 1];
				files[pId] = source;
				int id = pId - 1;
				while (id >= 0) {
					File file = getFile(source, parts, id);
					files[id] = file;
					id--;
				}

				try {
					File t = new File(targetPath, origName);
					for (int i = 0; i < files.length; i++) {
						if (files[i].exists() == false) {
							if (Crypto.isEncryptionActivated()) {
								System.out
										.println("ERROR! Linking of splitted files not possible. Is it possible that you are trying to decrypt unencrypted files?");
								System.out.println("Please remove --key=X parameter.");
							} else {
								System.out
										.println("ERROR! Linking of splitted files not possible. Is it possible that you are trying to decode encrypted files without using a decryption key?");
								System.out.println("Please add --key=X parameter.");
							}
						}
						byte[] data = ImageReader.readImage(ImageIO.read(files[i]));
						byte[] dataCopy;
						if (i != files.length - 1) {
							dataCopy = data;
						} else {
							int ignoreBytes = emptyB;
							if (ignoreBytes == -1) {
								ignoreBytes = 0;
							}
							dataCopy = new byte[data.length - ignoreBytes];
							for (int j = 0; j < dataCopy.length; j++) {
								dataCopy[j] = data[j];
							}
						}
						if (Crypto.isEncryptionActivated()) {
							try {
								dataCopy = Crypto.decrypt(dataCopy);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						if (paddingBytes != 0) {
							byte[] n = new byte[dataCopy.length - paddingBytes];
							for (int j = 0; j < dataCopy.length - paddingBytes; j++) {
								n[j] = dataCopy[j];
							}
						}
						FileIO.appendFileAsBytes(t, dataCopy);
					}
					return files;
				} catch (IOException e) {
					;
					e.printStackTrace();
					return null;
				}

			} else {
				return null;
			}
		}
		File[] array = { source };
		return array;
	}

	static File[] decodeFileWithIndexing(File sourceDir, File targetDir, String origName, int hash, int collisionCount, int[] paddingAndOverhead,
			String... path) {
		File sourceFile = new File(sourceDir, hash + "." + collisionCount + "." + (paddingAndOverhead.length == 2 ? "_" : "0") + ".png");
		Main.sizeInBytes += sourceFile.length();

		File targetPath = targetDir;
		if (!targetPath.exists()) {
			targetPath.mkdirs();
		}
		for (String p : path) {
			targetPath = new File(targetPath, p);
			if (!targetPath.exists()) {
				targetPath.mkdirs();
			}
		}
		if (paddingAndOverhead.length == 2) {
			// One packet
			int paddingBytes = paddingAndOverhead[0];
			int emptyB = paddingAndOverhead[1];
			try {
				byte[] data = ImageReader.readImage(ImageIO.read(sourceFile));
				byte[] dataCopy = new byte[data.length - emptyB];
				for (int i = 0; i < dataCopy.length; i++) {
					dataCopy[i] = data[i];
				}
				if (Crypto.isEncryptionActivated()) {
					try {
						dataCopy = Crypto.decrypt(dataCopy);
						if (paddingBytes != 0) {
							byte[] n = new byte[dataCopy.length - paddingBytes];
							for (int i = 0; i < dataCopy.length - paddingBytes; i++) {
								n[i] = dataCopy[i];
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				File t = new File(targetPath, origName);
				FileIO.writeFileAsBytes(t, dataCopy);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// Multiple packet
			try {
				File t = new File(targetPath, origName);
				File[] files = new File[paddingAndOverhead.length / 2];
				for (int i = 0; i < paddingAndOverhead.length / 2; i++) {
					File current = new File(sourceDir, hash + "." + collisionCount + "." + i + ".png");
					files[i] = current;
					if (current.exists() == false) {
						if (Crypto.isEncryptionActivated()) {
							System.out
									.println("ERROR! Linking of splitted files not possible. Is it possible that you are trying to decrypt unencrypted files?");
							System.out.println("Please remove --key=X parameter.");
						} else {
							System.out
									.println("ERROR! Linking of splitted files not possible. Is it possible that you are trying to decode encrypted files without using a decryption key?");
							System.out.println("Please add --key=X parameter.");
						}
					}
					byte[] data = ImageReader.readImage(ImageIO.read(current));
					byte[] dataCopy;

					int paddingBytes = paddingAndOverhead[i * 2];
					int emptyB = paddingAndOverhead[i * 2 + 1];

					int ignoreBytes = emptyB;
					if (ignoreBytes == -1) {
						ignoreBytes = 0;
					}
					dataCopy = new byte[data.length - ignoreBytes];
					for (int j = 0; j < dataCopy.length; j++) {
						dataCopy[j] = data[j];
					}
					if (Crypto.isEncryptionActivated()) {
						try {
							dataCopy = Crypto.decrypt(dataCopy);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if (paddingBytes != 0) {
						byte[] n = new byte[dataCopy.length - paddingBytes];
						for (int j = 0; j < dataCopy.length - paddingBytes; j++) {
							n[j] = dataCopy[j];
						}
					}
					FileIO.appendFileAsBytes(t, dataCopy);
				}
				return files;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}

		}
		File[] array = { sourceFile };
		return array;
	}

	private static File getFile(File source, String[] parts, int nr) {
		String s = parts[0] + "";
		for (int i = 1; i < parts.length; i++) {
			if (i == parts.length - 2) {
				s += "." + nr;
			} else if (i == parts.length - 3) {
				s += ".0";
			} else if (i == parts.length - 4) {
				if (Crypto.isEncryptionActivated()) {
					s += ".4";
				} else {
					s += ".0";
				}
			} else {
				s += "." + parts[i];
			}
		}
		File f = new File(source.getParentFile(), s);
		return f;
	}

	static void decodeDirectory(File sourceDir, File targetDir) {
		if (Main.indexing) {
			decodeDirWithIndexing(sourceDir, targetDir);
		} else {
			decodeDirWithoutIndexing(sourceDir, targetDir);
		}

	}

	private static void decodeDirWithIndexing(File sourceDir, File targetDir) {
		if (sourceDir.exists()) {
			IndexingDir rootDir = loadIndexing(sourceDir);

			// For now: Print file/folder structure;
			if (Main.debug) {
				printIndexingToLog(rootDir);
			}

			// Actual file decode
			decodeDirWithIndexing(sourceDir, targetDir, rootDir, new String[0]);
		}
	}

	private static void decodeDirWithIndexing(File sourceDir, File targetDir, IndexingDir dir, String[] path) {

		for (IndexingEntry en : dir.getSubFiles()) {
			if (en instanceof IndexingFile) {
				IndexingFile f = (IndexingFile) en;
				File[] files = decodeFileWithIndexing(sourceDir, targetDir, f.getName(), f.getHashId(), f.getCollsionCount(),
						f.getPaddingAndOverhead(), path);
				if (Main.delete) {
					for (File file : files) {
						file.delete();
					}
				}
			} else if (en instanceof IndexingDir) {
				IndexingDir nextDir = (IndexingDir) en;
				String[] nextPath = new String[path.length + 1];
				for (int i = 0; i < path.length; i++) {
					nextPath[i] = path[i];
				}
				nextPath[path.length] = nextDir.getName();
				decodeDirWithIndexing(sourceDir, targetDir, nextDir, nextPath);
				if (Main.delete) {
					(new File(sourceDir, nextDir.getName())).delete();
				}
			}
		}

	}

	private static void printIndexingToLog(IndexingDir rootDir) {
		System.out.println("\nroot: ");
		for (String s : printIndexingStructure(rootDir, 1)) {
			System.out.println(s);
		}
	}

	private static ArrayList<String> printIndexingStructure(IndexingDir dir, int depth) {
		ArrayList<String> text = new ArrayList<String>();

		String pre = "";
		int w = 5;
		for (int i = 0; i < depth * w; i++) {
			if (i % w == 0) {
				pre += "|";
			} else {
				pre += " ";
			}
		}

		for (IndexingEntry en : dir.getSubFiles()) {
			if (en instanceof IndexingFile) {
				IndexingFile f = (IndexingFile) en;
				text.add(pre + en.getName() + " <" + f.getHashId() + ":" + f.getCollsionCount() + "> " + Arrays.toString(f.getPaddingAndOverhead())
						+ "");
			} else if (en instanceof IndexingDir) {
				IndexingDir d = (IndexingDir) en;
				text.add(pre + en.getName() + ":");
				text.addAll(printIndexingStructure(d, depth + 1));
			} else {
				text.add(pre + en.getName() + "!!!");
			}
		}

		return text;
	}

	private static IndexingDir loadIndexing(File sourcePath) {
		IndexingDir root = loadIndexing(sourcePath, 0, null, null);
		return root;
	}

	private static IndexingDir loadIndexing(File sourceDir, int index, IndexingDir parent, IndexingDir originPart) {
		try {
			File indexFile = new File(sourceDir, "index." + index + ".png");
			byte[] rawData = ImageReader.readImage(ImageIO.read(indexFile));
			if (Main.delete) {
				indexFile.delete();
			}
			ByteBuffer buffer = ByteBuffer.wrap(rawData);
			@SuppressWarnings("unused")
			int emptyBytes = buffer.getInt();
			int amountOfEntries = buffer.getInt();
			int nameSize = buffer.getInt();
			String name = new String(getByteArray(buffer, nameSize));
			if (Crypto.isEncryptionActivated()) {
				try {
					name = Crypto.decryptString(name);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			int nextIndex = buffer.getInt();

			IndexingDir dir;
			if (originPart == null) {
				// Either index file is not split or first part
				dir = new IndexingDir(parent, name, index);
			} else {
				// index file is part of multiple index files that describe one
				// dir
				dir = originPart;
				dir.addId(index);
			}

			for (int i = 0; i < amountOfEntries; i++) {
				int entryNameLength = buffer.getInt();
				String entryName = new String(getByteArray(buffer, entryNameLength));
				if (Crypto.isEncryptionActivated()) {
					entryName = Crypto.decryptString(entryName);
				}
				int entryId = buffer.getInt();
				int entryCollisionCount = buffer.getInt();

				int amountOfFileParts = buffer.getInt();
				int[] paddingAndOverhead = new int[2 * amountOfFileParts];

				for (int j = 0; j < paddingAndOverhead.length; j++) {
					paddingAndOverhead[j] = buffer.getInt();
				}

				if (entryCollisionCount == -1) {
					// Directory
					dir.getSubFiles().add(loadIndexing(sourceDir, entryId, dir, null));
				} else {
					// File
					dir.getSubFiles().add(new IndexingFile(dir, entryName, entryId, entryCollisionCount, paddingAndOverhead));
				}
			}

			if (nextIndex != -1) {
				loadIndexing(sourceDir, nextIndex, parent, dir);
			}

			return dir;

		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private static byte[] getByteArray(ByteBuffer buffer, int nameSize) {
		byte[] array = new byte[nameSize];
		for (int i = 0; i < nameSize; i++) {
			array[i] = buffer.get();
		}
		return array;
	}

	private static void decodeDirWithoutIndexing(File sourceDir, File targetDir) {
		if (sourceDir.exists()) {
			for (File f : sourceDir.listFiles()) {
				if (f.isDirectory() == false) {
					try {
						File[] files = decodeFile(f, targetDir);
						if (Main.delete && files != null) {
							for (File file : files) {
								file.delete();
							}
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}

}
