package de.christian.f.brinkmann.ibus;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Encoder {

	private static int getMaxDataSize() {
		if (Crypto.isEncryptionActivated()) {
			return 4000 * 4000 * 4 - 4; // -4 for aes
		}
		return 4000 * 4000 * 4;
	}

	private static int getNextAESSize(long totalSize) {
		if (Crypto.isEncryptionActivated()) {
			return (int) (16 * (totalSize / 16 + 1));
		} else {
			return (int) totalSize;
		}
	}

	static void encodeFile(File source, File targetDir, int recursionDepth) {
		if (source.exists()) {
			Main.sizeInBytes += source.length();
			if (source.length() > getMaxDataSize()) {
				try {
					FileInputStream fin = new FileInputStream(source);
					for (int index = 0; index < source.length(); index += getMaxDataSize()) {
						byte[] fileContent;
						int overheadBytes;
						int paddingBytes;
						int size;
						int totalSize = getMaxDataSize();
						if (source.length() - index == getMaxDataSize()) {
							size = 4000;
							overheadBytes = -1;
							fileContent = new byte[getMaxDataSize()];
							paddingBytes = 4000 * 4000 * 4 - getMaxDataSize();
						} else if (source.length() - index > getMaxDataSize()) {
							size = 4000;
							overheadBytes = 0;
							fileContent = new byte[getMaxDataSize()];
							paddingBytes = 4000 * 4000 * 4 - getMaxDataSize();
						} else {
							totalSize = getNextAESSize(source.length() - index);
							size = (int) Math.sqrt(totalSize / 4);
							if (size * size * 4 < totalSize) {
								size++;
							}
							if (size < Main.minSize) {
								size = Main.minSize;
							}
							fileContent = new byte[(int) (source.length() - index)];
							overheadBytes = (size * size * 4) - (int) (totalSize);
							paddingBytes = totalSize - fileContent.length;
							index += fileContent.length; // In case 3996-3999
						}
						fin.read(fileContent);
						byte[] data;
						if (Crypto.isEncryptionActivated()) {
							data = Crypto.encrypt(fileContent);
							// System.out.println("Expected: " +
							// getNextAESSize(fileContent.length) + "   got: " +
							// data.length + " from: "
							// + fileContent.length);
						} else {
							data = fileContent;
						}

						String path = "";
						File f = source.getParentFile();
						for (int i = 0; i < recursionDepth; i++) {
							path = "." + f.getName() + path;
							f = f.getParentFile();
						}
						File t = new File(targetDir, source.getName() + path + "." + recursionDepth + "." + (paddingBytes) + "." + (overheadBytes)
								+ "." + (index / (4 * 4000 * 4000)));
						BufferedImage image = ImageCreator.createImage(size, data);
						FileIO.writeImageToPNG(image, t);
					}
					fin.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {

				// TODO: !!!!
				byte[] data = FileIO.readFileAsBytes(source);
				byte[] encrypted = null;
				if (Crypto.isEncryptionActivated()) {
					try {
						encrypted = Crypto.encrypt(data);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					encrypted = data;
				}
				int totalSize = encrypted.length;
				int size = (int) Math.sqrt(totalSize / 4);
				byte[] dataCopy;
				if (size * size * 4 < totalSize) {
					size++;
				}
				if (size < Main.minSize) {
					size = Main.minSize;
				}
				if (size * size * 4 == totalSize) {
					dataCopy = encrypted;
				} else {
					dataCopy = new byte[size * size * 4];
					for (int i = 0; i < encrypted.length; i++) {
						dataCopy[i] = encrypted[i];
					}
				}
				int paddingBytes = encrypted.length - data.length;
				String path = "";
				File f = source.getParentFile();
				for (int i = 0; i < recursionDepth; i++) {
					path = "." + f.getName() + path;
					f = f.getParentFile();
				}
				File t = new File(targetDir, source.getName() + path + "." + recursionDepth + "." + paddingBytes + "."
						+ ((size * size * 4) - encrypted.length) + "._");
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
