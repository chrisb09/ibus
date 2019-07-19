package de.christian.f.brinkmann.ibus;

import java.io.File;

import javax.imageio.ImageIO;

import de.christian.f.brinkmann.ibus.indexing.IndexingDir;
import de.christian.f.brinkmann.ibus.indexing.IndexingEntry;
import de.christian.f.brinkmann.ibus.indexing.IndexingFile;

public class Decoder {

	static void decodeEntry(IndexingEntry toDecode, File targetDir, File sourceDir) {
		if (toDecode instanceof IndexingFile) {
			IndexingFile file = (IndexingFile) toDecode;
			Main.sizeInBytes += file.getSize();
			if (Metric.active != null) {
				Metric.active.addSize(file.getSize());
			}
			File t = new File(targetDir, file.getName());
			if (file.getPaddingAndOverhead().length == 2) {
				// one part
				try {
					int emptyB = file.getPaddingAndOverhead()[1];
					File f = new File(sourceDir, file.getHashId() + "." + file.getCollsionCount() + "._.png");
					byte[] data = ImageReader.readImage(ImageIO.read(f));
					if (Metric.active != null) {
						Metric.active.startCopying();
					}
					byte[] dataCopy = new byte[data.length - emptyB];
					for (int i = 0; i < dataCopy.length; i++) {
						dataCopy[i] = data[i];
					}
					if (Metric.active != null) {
						Metric.active.endCopying();
					}
					if (!Crypto.isEncryptionActivated()) {
						System.out.println("Can't work without encryption.");
						return;
					}
					dataCopy = Crypto.decrypt(dataCopy);
					FileIO.writeFileAsBytes(t, dataCopy);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				// multiple parts
				try {
					for (int i = 0; i < file.getPaddingAndOverhead().length / 2; i++) {
						File f = new File(sourceDir, file.getHashId() + "." + file.getCollsionCount() + "." + i + ".png");
						byte[] data = ImageReader.readImage(ImageIO.read(f));
						int emptyB = file.getPaddingAndOverhead()[i * 2 + 1];
						byte[] dataCopy;
						if (i != file.getPaddingAndOverhead().length / 2 - 1) {
							dataCopy = data;
						} else {
							int ignoreBytes = emptyB;
							if (ignoreBytes == -1) {
								ignoreBytes = 0;
							}
							if (Metric.active != null) {
								Metric.active.startCopying();
							}
							dataCopy = new byte[data.length - ignoreBytes];
							for (int j = 0; j < dataCopy.length; j++) {
								dataCopy[j] = data[j];
							}
							if (Metric.active != null) {
								Metric.active.endCopying();
							}
						}
						if (!Crypto.isEncryptionActivated()) {
							System.out.println("Can't work without encryption.");
							return;
						}
						try {
							dataCopy = Crypto.decrypt(dataCopy);
						} catch (Exception e) {
							e.printStackTrace();
							return;
						}
						FileIO.appendFileAsBytes(t, dataCopy);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			IndexingDir dir = (IndexingDir) toDecode;
			File file = new File(targetDir, dir.getName());
			if (!file.exists()) {
				file.mkdirs();
			}
			for (IndexingEntry en : dir.getSubFiles()) {
				decodeEntry(en, file, sourceDir);
			}
		}
	}

}
