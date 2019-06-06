package de.christian.f.brinkmann.ibus;

import java.io.File;

public class Main {

	static boolean delete = false;
	static long sizeInBytes = 0l;

	public static void main(String[] args) {

		int mode = 0; // 1 encode, 2 decode

		if (args.length < 3) {
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

		if (args.length >= 4 && args[3].equalsIgnoreCase("--delete")) {
			delete = true;
		}

		long start = System.currentTimeMillis();

		if (mode == 1) {
			Encoder.encodeDirectoryAlpha(new File(source), new File(target));
		} else if (mode == 2) {
			Decoder.decodeDirectory(new File("target"), new File("result"));
		}

		System.out.println("Operation completed.");
		System.out.println("Time: " + ((System.currentTimeMillis() - start) / 1000l) + "s");
		System.out.println("Data: " + (sizeInBytes / 1000000l) + "Mbyte");
		System.out.println("Rate: " + ((sizeInBytes * 8 / 1000000l) / ((System.currentTimeMillis() - start) / 1000l)) + " Mbit/s");
	}

	private static void printHelp() {
		System.out.println("Parameter: <sourceDir> <targetDir> -encode/-decode [--delete]");
	}

}
