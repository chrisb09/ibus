package de.christian.f.brinkmann.ibus;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Encoder {

	static void encodeFile(File source, File targetDir, int recursionDepth) {
		if (source.exists()) {
			Main.sizeInBytes += source.length();
			if (source.length() > 4 * 4000 * 4000) {
				try {
					FileInputStream fin = new FileInputStream(source);
					for (int index = 0; index < source.length(); index += 4 * 4000 * 4000) {
						byte[] fileContent;
						int overheadBytes;
						int size;
						if (source.length() - index == 4 * 4000 * 4000) {
							size = 4000;
							overheadBytes = -1;
						} else if (source.length() - index > 4 * 4000 * 4000) {
							size = 4000;
							overheadBytes = 0;
						} else {
							size = (int) Math.sqrt((source.length() - index) / 4);
							if (size * size * 4 < source.length() - index) {
								size++;
							}
							if (size < Main.minSize) {
								size = Main.minSize;
							}
							overheadBytes = (size * size * 4) - (int) (source.length() - index);
						}
						fileContent = new byte[4 * size * size];
						fin.read(fileContent);

						String path = "";
						File f = source.getParentFile();
						for (int i = 0; i < recursionDepth; i++) {
							path = "." + f.getName() + path;
							f = f.getParentFile();
						}
						File t = new File(targetDir, source.getName() + path + "." + recursionDepth + "."
								+ (overheadBytes) + "." + (index / (4 * 4000 * 4000)));
						BufferedImage image = ImageCreator.createImage(size, fileContent);
						FileIO.writeImageToPNG(image, t);
					}
					fin.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				byte[] data = FileIO.readFileAsBytes(source);
				int size = (int) Math.sqrt(data.length / 4);
				byte[] dataCopy;
				if (size * size * 4 < data.length) {
					size++;
				}
				if (size < Main.minSize) {
					size = Main.minSize;
				}
				if (size * size * 4 == data.length) {
					dataCopy = data;
				} else {
					dataCopy = new byte[size * size * 4];
					for (int i = 0; i < data.length; i++) {
						dataCopy[i] = data[i];
					}
				}
				String path = "";
				File f = source.getParentFile();
				for (int i = 0; i < recursionDepth; i++) {
					path = "." + f.getName() + path;
					f = f.getParentFile();
				}
				File t = new File(targetDir, source.getName() + path + "." + recursionDepth + "."
						+ ((size * size * 4) - data.length) + "._");
				BufferedImage image = ImageCreator.createImage(size, dataCopy);
				FileIO.writeImageToPNG(image, t);
			}
		} else {
			System.out.println("Doesn't exist: " + source.getAbsolutePath());
		}
	}

	static void encodeDirectoryAlpha(File sourceDir, File targetDir) {
		encodeDirectory(sourceDir, targetDir, 0);
	}

	private static void encodeDirectory(File currentDir, File targetDir, int depth) {
		if (currentDir.exists()) {
			for (File f : currentDir.listFiles()) {
				if (f.isDirectory()) {
					encodeDirectory(f, targetDir, depth + 1);
					if (Main.delete) {
						f.delete();
					}
				} else {
					encodeFile(f, targetDir, depth);
					if (Main.delete) {
						f.delete();
					}
				}
			}
		}
	}

}
