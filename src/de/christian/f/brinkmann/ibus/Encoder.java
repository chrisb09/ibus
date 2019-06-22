package de.christian.f.brinkmann.ibus;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Encoder {

	// @formatter:off
		// [4 byte: empty Bytes] [4byte: amount of indicies]
		// [4 Byte: "a" Current folder name size][a Byte: Current folder name]
		// [4 Byte: next index file index number]
		// ----- [4 Byte: "b" Next Index file name size][b Byte: Next Index file name]
		// 
		// ----- [1 Byte: x_i is folder or file]
		// [4 Byte: "x_i" name length]
		// [x_i Bytes: Name of x_i][4 Byte: Name Hash Int(Java Impl)]
		// [4 Byte: Name Hash Number/Count (usually 0), but needed for collision, -1 for folder]
		// [4 Byte: amount of file parts, 0 for folder]
		// [4 Byte: x_j file j padding] [4 Byte: x_j file j overhead]
		// @formatter:on

	// int setSize = 4+4+4+a+b+ n*(4+x+4+4+4+ m*(4+4));

	private static int indexFileindex = 0;
	static final HashMap<Integer, List<String>> indices = new HashMap<Integer, List<String>>();

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

	static Integer[] encodeFile(File source, File targetDir, int recursionDepth, int collisions) {
		ArrayList<Integer> res = new ArrayList<Integer>();
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
						} else {
							data = fileContent;
						}

						String path = "";
						if (!Main.indexing) {
							File f = source.getParentFile();
							for (int i = 0; i < recursionDepth; i++) {
								path = "." + f.getName() + path;
								f = f.getParentFile();
							}
						}
						File t = null;
						if (Main.indexing) {
							t = new File(targetDir, source.getName().hashCode() + "." + collisions + "." + (index / getMaxDataSize()));
							res.add(paddingBytes);
							res.add(overheadBytes);
						} else {
							t = new File(targetDir, source.getName() + path + "." + recursionDepth + "." + (paddingBytes) + "." + (overheadBytes)
									+ "." + (index / getMaxDataSize()));
						}
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
				if (!Main.indexing) {
					File f = source.getParentFile();
					for (int i = 0; i < recursionDepth; i++) {
						path = "." + f.getName() + path;
						f = f.getParentFile();
					}
				}
				File t;
				if (Main.indexing) {
					t = new File(targetDir, source.getName().hashCode() + "." + collisions + "._");
					res.add(paddingBytes);
					int overheadBytes = (size * size * 4) - encrypted.length;
					res.add(overheadBytes);
				} else {
					t = new File(targetDir, source.getName() + path + "." + recursionDepth + "." + paddingBytes + "."
							+ ((size * size * 4) - encrypted.length) + "._");
				}
				BufferedImage image = ImageCreator.createImage(size, dataCopy);
				FileIO.writeImageToPNG(image, t);
			}
			return res.toArray(new Integer[res.size()]);
		} else {
			System.out.println("Doesn't exist: " + source.getAbsolutePath());
		}
		return null;
	}

	static void encodeDirectoryAlpha(File sourceDir, File targetDir) {
		encodeDirectory(sourceDir, targetDir, 0);
	}

	private static int encodeDirectory(File currentDir, File targetDir, int depth) {
		if (currentDir.exists()) {
			// final HashMap<String, Integer> indicesOfSubfolders = new
			// HashMap<String, Integer>();

			byte[] indiceBytes = new byte[0];
			byte[] a = null;
			try {
				a = (depth != 0 ? (Crypto.isEncryptionActivated() ? Crypto.encryptString(currentDir.getName()) : currentDir.getName()) : "").getBytes();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			byte[] header = new byte[4 + 4 + 4 + a.length];
			writeIntAt(a.length, header, 8);
			writeByteArrayAt(a, header, 12);
			int entries = 0;
			int lastEntryWritten = 0;

			int firstIndex = indexFileindex;
			int localIndex = indexFileindex;
			indexFileindex++;

			for (File f : currentDir.listFiles()) {
				int hash = f.getName().hashCode();
				if (indices.containsKey(hash)) {
					indices.get(hash).add(f.getName());
				} else {
					List<String> list = new ArrayList<String>();
					list.add(f.getName());
					indices.put(hash, list);
				}
				
				byte[] name;
				if (Crypto.isEncryptionActivated()) {
					try {
						name = Crypto.encryptString(f.getName()).getBytes();
					} catch (Exception e) {
						name = f.getName().getBytes();
						e.printStackTrace();
					}
				}else{
					name = f.getName().getBytes();
				}
				
				byte[] t = new byte[4 + name.length + 4 + 4 + 4];

				if (Main.indexing) {
					entries++;
					writeIntAt(name.length, t, 0);
					writeByteArrayAt(name, t, 4);
				}

				if (f.isDirectory()) {
					int subIndex = encodeDirectory(f, targetDir, depth + 1);

					if (Main.indexing) {
						writeIntAt(subIndex, t, 4 + name.length);
						writeIntAt(-1, t, 8 + name.length);
						writeIntAt(0, t, 12 + name.length);
					}
					if (Main.delete) {
						f.delete();
					}
				} else {
					Integer[] res = encodeFile(f, targetDir, depth, indices.get(f.getName().hashCode()).size() - 1);
					if (Main.indexing) {
						writeIntAt(f.getName().hashCode(), t, 4 + name.length);
						writeIntAt(indices.get(f.getName().hashCode()).size() - 1, t, 8 + name.length);
						writeIntAt(res.length / 2, t, 12 + name.length);
						t = append(t, new byte[res.length * 4]);
						for (int i = 0; i < res.length; i++) {
							writeIntAt(res[i], t, 16 + name.length + i * 4);
						}
					}
				}
				if (Main.delete) {
					f.delete();
				}

				if (Main.indexing) {
					if (indiceBytes.length + t.length > getMaxDataSize() - 256) {
						byte[] n = writeCurrentIndexFile(indexFileindex, lastEntryWritten, indiceBytes, header, entries);
						writeIndexFilex(targetDir, localIndex, n);

						indiceBytes = t;
						lastEntryWritten = entries;
						localIndex = indexFileindex;
						indexFileindex++;
					} else {
						indiceBytes = append(indiceBytes, t);
					}
				}
			}

			if (Main.indexing) {
				System.out.print("Name=" + (depth != 0 ? currentDir.getName() : "") + " ");
				byte[] n = writeCurrentIndexFile(-1, lastEntryWritten, indiceBytes, header, entries);
				writeIndexFilex(targetDir, localIndex, n);
				return firstIndex;
			}

		}
		return -1;
	}

	private static String byteToHumanString(byte[] array) {
		String text = "";
		for (int i = 0; i < array.length; i++) {
			text += " " + i + ":" + String.format("%03d", array[i]) + "=" + bytetoHumanReadable(array[i]);
		}
		return text;
	}

	private static String bytetoHumanReadable(byte b) {
		char[] chars = new char[8];
		for (int i = 0; i < 8; i++) {
			chars[i] = (char) ('0' + ((b & (0b1 << (7 - i))) >> (7 - i)));
		}
		return new String(chars);
	}

	private static byte[] writeCurrentIndexFile(int nextIndex, int lastEntryWritten, byte[] indiceBytes, byte[] header, int entries) {
		byte[] b = new byte[4];
		writeIntAt(nextIndex, b, 0);
		byte[] n = append(append(header, b), indiceBytes);
		writeIntAt(entries - lastEntryWritten, n, 4); // Amount of indices
		return n;
	}

	private static void writeIndexFilex(File targetDir, int index, byte[] content) {

		System.out.println("Print to File[" + index + "]: " + byteToHumanString(content));
		int size = (int) Math.sqrt(content.length / 4);
		byte[] dataCopy;
		if (size * size * 4 < content.length) {
			size++;
		}
		if (size < Main.minSize) {
			size = Main.minSize;
		}
		if (size * size * 4 == content.length) {
			dataCopy = content;
		} else {
			dataCopy = new byte[size * size * 4];
			for (int i = 0; i < content.length; i++) {
				dataCopy[i] = content[i];
			}
		}

		writeIntAt(dataCopy.length - content.length, dataCopy, 0);
		BufferedImage image = ImageCreator.createImage(size, dataCopy);
		FileIO.writeImageToPNG(image, new File(targetDir, "index." + index));

	}

	private static void writeByteArrayAt(byte[] toWrite, byte[] target, int targetIndex) {
		for (int i = 0; i < toWrite.length; i++) {
			target[targetIndex + i] = toWrite[i];
		}
	}

	private static void writeIntAt(int toWrite, byte[] target, int targetIndex) {
		target[targetIndex] = (byte) (toWrite >> 24);
		target[targetIndex + 1] = (byte) ((toWrite >> 16) & 0b11111111);
		target[targetIndex + 2] = (byte) ((toWrite >> 8) & 0b11111111);
		target[targetIndex + 3] = (byte) (toWrite & 0b11111111);
	}

	private static byte[] append(byte[] a, byte[] b) {
		byte[] res = new byte[a.length + b.length];
		for (int i = 0; i < a.length; i++) {
			res[i] = a[i];
		}
		for (int i = 0; i < b.length; i++) {
			res[a.length + i] = b[i];
		}
		return res;
	}

}
