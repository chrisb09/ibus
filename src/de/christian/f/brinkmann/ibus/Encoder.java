package de.christian.f.brinkmann.ibus;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import de.christian.f.brinkmann.ibus.indexing.IndexingDir;
import de.christian.f.brinkmann.ibus.indexing.IndexingFile;

public class Encoder {

	static int getMaxDataSize() {
		if (Crypto.isEncryptionActivated()) {
			return Main.maxSize * Main.maxSize * 4 - 4; // -4 for aes
		}
		return Main.maxSize * Main.maxSize * 4;
	}

	private static int getNextAESSize(long totalSize) {
		if (Crypto.isEncryptionActivated()) {
			return (int) (16 * (totalSize / 16 + 1));
		} else {
			return (int) totalSize;
		}
	}

	static IndexingFile encodeFile(IndexingDir parent, File source, File targetDir, int collisions) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		if (source.exists()) {
			Main.sizeInBytes += source.length();
			if (Metric.active != null) {
				Metric.active.addSize(source.length());
			}
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
							size = Main.maxSize;
							overheadBytes = -1;
							fileContent = new byte[getMaxDataSize()];
							paddingBytes = Main.maxSize * Main.maxSize * 4 - getMaxDataSize();
						} else if (source.length() - index > getMaxDataSize()) {
							size = Main.maxSize;
							overheadBytes = 0;
							fileContent = new byte[getMaxDataSize()];
							paddingBytes = Main.maxSize * Main.maxSize * 4 - getMaxDataSize();
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

						File t = new File(targetDir, source.getName().hashCode() + "." + collisions + "." + (index / getMaxDataSize()));
						res.add(paddingBytes);
						res.add(overheadBytes);
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
					if (Metric.active != null) {
						Metric.active.startCopying();
					}
					dataCopy = new byte[size * size * 4];
					for (int i = 0; i < encrypted.length; i++) {
						dataCopy[i] = encrypted[i];
					}
					if (Metric.active != null) {
						Metric.active.endCopying();
					}
				}
				int paddingBytes = encrypted.length - data.length;
				File t = new File(targetDir, source.getName().hashCode() + "." + collisions + "._");
				res.add(paddingBytes);
				int overheadBytes = (size * size * 4) - encrypted.length;
				res.add(overheadBytes);
				BufferedImage image = ImageCreator.createImage(size, dataCopy);
				FileIO.writeImageToPNG(image, t);
			}
			int[] paddingAndOverhead = new int[res.size()];
			for (int i = 0; i < res.size(); i++) {
				paddingAndOverhead[i] = res.get(i);
			}
			IndexingFile indf = new IndexingFile(parent, source.getName(), source.getName().hashCode(), collisions, paddingAndOverhead,
					source.length());
			return indf;
		} else {
			System.out.println("Doesn't exist: " + source.getAbsolutePath());
		}
		return null;
	}

	public static void encodeFile(File toEncode, File sourcePath, IndexingDir parent) {
		if (toEncode.isDirectory()) {
			IndexingDir sub = new IndexingDir(parent, toEncode.getName(), Main.getAndUseNextFreeIndex());
			FileSystemFunctions.saveIndexing(sourcePath, sub);
			parent.getSubFiles().add(sub);
			FileSystemFunctions.saveIndexing(sourcePath, parent);
			for (File f : toEncode.listFiles()) {
				encodeFile(f, sourcePath, sub);
			}
		} else {
			int collisions = Main.getFreeHashCollisionNumber(toEncode.getName().hashCode());
			Main.addHashCollisionNumber(toEncode.getName().hashCode(), collisions);
			parent.getSubFiles().add(Encoder.encodeFile(parent, toEncode, sourcePath, collisions));
			FileSystemFunctions.saveIndexing(sourcePath, parent);
		}
	}

	// @formatter:off
	/*
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
				
				byte[] t = new byte[4 + name.length + 4 + 4 + 8 + 4];

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
						writeIntAt(0, t, 16 + name.length);
						writeIntAt(0, t, 20 + name.length);
					}
					if (Main.delete) {
						f.delete();
					}
				} else {
					Integer[] res = encodeFile(f, targetDir, depth, indices.get(f.getName().hashCode()).size() - 1);
					if (Main.indexing) {
						writeIntAt(f.getName().hashCode(), t, 4 + name.length);
						writeIntAt(indices.get(f.getName().hashCode()).size() - 1, t, 8 + name.length);
						writeIntAt((int) (f.length() >>  32), t, 12 + name.length);
						writeIntAt((int) (f.length() & (0b11111111111111111111111111111111)), t, 16 + name.length);
						writeIntAt(res.length / 2, t, 20 + name.length);
						t = append(t, new byte[res.length * 4]);
						for (int i = 0; i < res.length; i++) {
							writeIntAt(res[i], t, 24 + name.length + i * 4);
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
				if (Main.debug) {
					System.out.print("Name=" + (depth != 0 ? currentDir.getName() : "") + " ");
				}
				byte[] n = writeCurrentIndexFile(-1, lastEntryWritten, indiceBytes, header, entries);
				writeIndexFilex(targetDir, localIndex, n);
				return firstIndex;
			}

		}
		return -1;
	}*/
	// @formatter:on

}
