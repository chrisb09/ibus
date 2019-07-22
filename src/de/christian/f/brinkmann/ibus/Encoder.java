package de.christian.f.brinkmann.ibus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
						if (Metric.active != null) {
							Metric.active.addSize(fileContent.length);
						}
						Main.encoder.createImage(t, size, data);
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
				Main.encoder.createImage(t, size, dataCopy);
				if (Metric.active != null) {
					Metric.active.addSize(data.length);
				}
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

	public static void startEncodeFile(File sPath, File sourcePath, IndexingDir local) {

		Metric.startMetric();
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				System.out.println("Metric: " + Metric.active.getTempInfo());
			}
		}, 8, 8, TimeUnit.SECONDS);

		encodeFile(sPath, sourcePath, local);

		scheduler.shutdown();
		System.out.println(Metric.stopMetric());

	}

}
