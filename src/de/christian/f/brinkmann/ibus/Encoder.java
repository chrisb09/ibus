package de.christian.f.brinkmann.ibus;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Encoder {

	static void encodeFile(File source, File targetDir) {
		if (source.exists()) {
			if (source.length() > 3 * 4000 * 4000) {
				System.out.println("Huge file");
			} else {
				byte[] data = FileIO.readFileAsBytes(source);
				int size = (int) Math.sqrt(data.length / 3);
				if (size * size * 3 < data.length)
					size++;
				byte[] dataCopy = new byte[size * size * 3];
				for (int i = 0; i < data.length; i++) {
					dataCopy[i] = data[i];
				}
				File t = new File(targetDir, source.getName() + "." + ((size * size * 3) - data.length) + "._");
				System.out.println("t: " + t.getAbsolutePath());
				BufferedImage image = ImageCreator.createImage(size, dataCopy);
				FileIO.writeImageToPNG(image, t);
			}
		} else {
			System.out.println("No file: " + source.getAbsolutePath() + "@" + source.length());
		}
	}

	static void encodeFileAlpha(File source, File targetDir, int recursionDepth) {
		if (source.exists()) {
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
						}else if (source.length() - index > 4 * 4000 * 4000) {
							size = 4000;
							overheadBytes = 0;
						} else {
							size = (int) Math.sqrt((source.length() - index) / 4);
							if (size * size * 4 < source.length() - index) {
								size++;
							}
							overheadBytes = (size*size*4) - (int) (source.length() - index);
						}
						fileContent = new byte[4 * size * size];
						fin.read(fileContent);

						String path = "";
						File f = source.getParentFile();
						for (int i = 0; i < recursionDepth; i++) {
							path = "." + f.getName() + path;
							f = f.getParentFile();
						}
						File t = new File(targetDir, source.getName() + path + "." + recursionDepth + "." + (overheadBytes)
								+ "." + (index / (4 * 4000 * 4000)));
						BufferedImage image = ImageCreator.createImageAlpha(size, fileContent);
						FileIO.writeImageToPNG(image, t);
					}
					fin.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				byte[] data = FileIO.readFileAsBytes(source);
				int size = (int) Math.sqrt(data.length / 4);
				byte[] dataCopy;
				if (size * size * 4 < data.length) {
					size++;
					dataCopy = new byte[size * size * 4];
					for (int i = 0; i < data.length; i++) {
						dataCopy[i] = data[i];
					}
				} else {
					dataCopy = data;
				}
				String path = "";
				File f = source.getParentFile();
				for (int i = 0; i < recursionDepth; i++) {
					path = "." + f.getName() + path;
					f = f.getParentFile();
				}
				File t = new File(targetDir, source.getName() + path + "." + recursionDepth + "." + ((size * size * 4) - data.length) + "._");
				BufferedImage image = ImageCreator.createImageAlpha(size, dataCopy);
				FileIO.writeImageToPNG(image, t);
			}
		} else {
			System.out.println("No file: " + source.getAbsolutePath() + "@" + source.length());
		}
	}

	static void encodeDirectoryAlpha(File sourceDir, File targetDir) {
		encodeDirectoryAlpha(sourceDir, targetDir, 0);
	}

	private static void encodeDirectoryAlpha(File currentDir, File targetDir, int depth) {
		if (currentDir.exists()) {
			for (File f : currentDir.listFiles()) {
				if (f.isDirectory()) {
					encodeDirectoryAlpha(f, targetDir, depth + 1);
					// TODO: Delete f
				} else {
					encodeFileAlpha(f, targetDir, depth);
					// TODO: Delete f
				}
			}
		}
	}

}
