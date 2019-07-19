package de.christian.f.brinkmann.ibus;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

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
		Main.sizeInBytes += ((paddingAndOverhead.length / 2) - 1) * Encoder.getMaxDataSize() + sourceFile.length();

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
		decodeDirWithIndexing(sourceDir, targetDir);
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
		for (String s : FileSystemFunctions.printIndexingStructure(rootDir, 1, Integer.MAX_VALUE)) {
			System.out.println(s);
		}
	}

	private static IndexingDir loadIndexing(File sourcePath) {
		IndexingDir root = FileSystemFunctions.loadIndexing(sourcePath, 0, null, null);
		return root;
	}

}
