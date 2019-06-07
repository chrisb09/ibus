package de.christian.f.brinkmann.ibus;

import java.io.File;

public class Main {

	static boolean delete = false;
	static long sizeInBytes = 0l;
	static int minSize = 256;

	public static void main(String[] args) {

		int mode = 0; // 1 encode, 2 decode

		if (args.length < 3) {
			testAES();
			printHelp();
			return;
		}

		String source = args[0];
		String target = args[1];

		if (args[2].equalsIgnoreCase("-encode")) {
			mode = 1;
		} else if (args[2].equalsIgnoreCase("-decode")) {
			mode = 2;
		} else {
			System.out.println("Please select a valid operation mode: -encode or -decode");
			printHelp();
			return;
		}

		for (int i = 3; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("--delete")) {
				delete = true;
			}
			if (args[i].toLowerCase().startsWith("--minsize")) {
				minSize = Integer.parseInt(args[i].substring(9, args[i].length()));
			}
			if (args[i].toLowerCase().startsWith("--key")) {
				setKey(args[i].substring(5, args[i].length()));
			}
			if (args[i].equals("--cleartarget")) {
				System.out.println("Clearing target folder: " + new File(target).getAbsolutePath());
				deleteDir(new File(target));
			}
		}

		long start = System.currentTimeMillis();

		if (mode == 1) {
			Encoder.encodeDirectoryAlpha(new File(source), new File(target));
		} else if (mode == 2) {
			Decoder.decodeDirectory(new File(source), new File(target));
		}

		System.out.println("Operation completed.");
		System.out.println("Time: " + ((System.currentTimeMillis() - start) / 1000l) + "s");
		System.out.println("Data: " + (sizeInBytes / 1000000l) + "Mbyte");
		System.out
				.println("Rate: "
						+ (((System.currentTimeMillis() - start) / 1000l) != 0 ? ((sizeInBytes * 8 / 1000000l) / ((System.currentTimeMillis() - start) / 1000l))
								+ " Mbit/s"
								: "-"));
	}

	private static void deleteDir(File file) {
		for (File f : file.listFiles()) {
			if (f.isDirectory()) {
				deleteDir(f);
			}
			f.delete();
		}
	}

	private static void printHelp() {
		System.out.println("Parameter: <sourceDir> <targetDir> -encode/-decode [--delete] [--minSize=X] [--key=Y] [--cleartarget]");
		System.out.println(" where X is an Integer and Y is a password");
	}

	private static void setKey(String key) {
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

	private static void testAES() {
		setKey("test");
		byte[] k = new byte[4000 * 4000 * 4 - 4];
		byte[] c = new byte[0];
		try {
			c = Crypto.encrypt(k);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(c.length + " -> " + k.length + " (" + (c.length - k.length) + ")");
	}

}
