package de.christian.f.brinkmann.ibus;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;

import javax.imageio.ImageIO;

import de.christian.f.brinkmann.ibus.indexing.IndexingDir;
import de.christian.f.brinkmann.ibus.indexing.IndexingEntry;
import de.christian.f.brinkmann.ibus.indexing.IndexingFile;

public class FileSystemFunctions {

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
		// [8 Byte: File size]
		// [4 Byte: amount of file parts, 0 for folder]
		// [4 Byte: x_j file j padding] [4 Byte: x_j file j overhead]
		// @formatter:on

	// int setSize = 4+4+4+a+b+ n*(4+x+4+4+4+ m*(4+4));

	static void saveIndexing(File sourceDir, IndexingDir dir) {
		String name = dir.getName();
		if (Crypto.isEncryptionActivated()) {
			try {
				name = Crypto.encryptString(name);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}

		byte[] nameAsBytes = name.getBytes();
		byte[] header = new byte[4 + 4 + 4 + nameAsBytes.length + 4];
		writeIntAt(nameAsBytes.length, header, 8);
		writeByteArrayAt(nameAsBytes, header, 12);
		byte[] buffer = new byte[0];
		ArrayList<byte[]> toSave = new ArrayList<byte[]>();
		int lastSaved = 0;
		int index = 0;
		for (IndexingEntry en : dir.getSubFiles()) {
			byte[] entryName;
			if (Crypto.isEncryptionActivated()) {
				try {
					entryName = Crypto.encryptString(en.getName()).getBytes();
				} catch (Exception e) {
					entryName = en.getName().getBytes();
					e.printStackTrace();
				}
			} else {
				entryName = en.getName().getBytes();
			}

			byte[] t = new byte[4 + entryName.length + 4 + 4 + 8 + 4];
			writeIntAt(entryName.length, t, 0);
			writeByteArrayAt(entryName, t, 4);
			if (en instanceof IndexingDir) {
				int subIndex = ((IndexingDir) (en)).getIds().get(0);

				writeIntAt(subIndex, t, 4 + entryName.length);
				writeIntAt(-1, t, 8 + entryName.length);
				writeIntAt(0, t, 12 + entryName.length);
				writeIntAt(0, t, 16 + entryName.length);
				writeIntAt(0, t, 20 + entryName.length);

			} else if (en instanceof IndexingFile) {
				IndexingFile indf = (IndexingFile) en;
				int[] res = indf.getPaddingAndOverhead();

				writeIntAt(indf.getHashId(), t, 4 + entryName.length);
				writeIntAt(indf.getCollsionCount(), t, 8 + entryName.length);
				writeIntAt((int) (indf.getSize() >> 32), t, 12 + entryName.length);
				writeIntAt((int) (indf.getSize() & (0b11111111111111111111111111111111)), t, 16 + entryName.length);
				writeIntAt(res.length / 2, t, 20 + entryName.length);
				t = append(t, new byte[res.length * 4]);
				for (int j = 0; j < res.length; j++) {
					writeIntAt(res[j], t, 24 + entryName.length + j * 4);
				}
			}
			if (header.length + buffer.length + t.length > 4 * Main.maxSize * Main.maxSize) {
				// Save and next
				byte[] data = append(header, buffer);
				writeIntAt(index - lastSaved, data, 4);
				toSave.add(data);
				buffer = t;

				lastSaved = index;
			} else {
				buffer = append(buffer, t);
			}
			index++;
		}

		// add last data to toSave list
		byte[] data = append(header, buffer);
		writeIntAt(dir.getSubFiles().size() - lastSaved, data, 4);
		toSave.add(data);

		// connect index file parts
		// also write to disk
		int previousIndex = -1; // previous when going backwards
		for (int i = toSave.size() - 1; i >= 0; i--) {
			data = toSave.get(i);
			int currentIndex = 0;
			if (i < dir.getIds().size()) { // 1
				// use existing id
				currentIndex = dir.getIds().get(i);
			} else {
				// use new id
				currentIndex = Main.getAndUseNextFreeIndex();
				dir.getIds().add(currentIndex);
			}
			writeIntAt(previousIndex, data, 12 + nameAsBytes.length);
			File indexFile = new File(sourceDir, "index." + currentIndex + "");
			int size = Math.max((int) Math.ceil(Math.sqrt(data.length / 4)), 256);
			Main.encoder.createImage(indexFile, size, data);
			previousIndex = currentIndex;
		}

		Integer[] toRem = new Integer[dir.getIds().size() - toSave.size()];
		for (int i = toSave.size(); i < dir.getIds().size(); i++) {
			File indexFile = new File(sourceDir, "index." + dir.getIds().get(i) + ".png");
			indexFile.delete();
			toRem[dir.getIds().size() - i - 1] = dir.getIds().get(i);
			Main.setIndexUsed(dir.getIds().get(i), false);
		}

		for (Integer id : toRem) {
			dir.getIds().remove(id);
		}
	}

	static IndexingDir loadIndexing(File sourceDir, int index, IndexingDir parent, IndexingDir originPart) {
		try {
			File indexFile = new File(sourceDir, "index." + index + ".png");
			byte[] rawData = ImageReader.readImage(ImageIO.read(indexFile));
			if (Main.delete) {
				indexFile.delete();
			}
			ByteBuffer buffer = ByteBuffer.wrap(rawData);
			@SuppressWarnings("unused")
			int emptyBytes = buffer.getInt();
			int amountOfEntries = buffer.getInt();
			int nameSize = buffer.getInt();
			String name = new String(getByteArray(buffer, nameSize));
			if (Crypto.isEncryptionActivated()) {
				try {
					name = Crypto.decryptString(name);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			int nextIndex = buffer.getInt();

			IndexingDir dir;
			if (originPart == null) {
				// Either index file is not split or first part
				dir = new IndexingDir(parent, name, index);
			} else {
				// index file is part of multiple index files that describe one
				// dir
				dir = originPart;
				dir.getIds().add(index);
				Main.setIndexUsed(index, true);
			}

			for (int i = 0; i < amountOfEntries; i++) {
				int entryNameLength = buffer.getInt();
				String entryName = new String(getByteArray(buffer, entryNameLength));
				if (Crypto.isEncryptionActivated()) {
					entryName = Crypto.decryptString(entryName);
				}
				int entryId = buffer.getInt();
				int entryCollisionCount = buffer.getInt();

				long fileSize = buffer.getLong();

				int amountOfFileParts = buffer.getInt();
				int[] paddingAndOverhead = new int[2 * amountOfFileParts];

				for (int j = 0; j < paddingAndOverhead.length; j++) {
					paddingAndOverhead[j] = buffer.getInt();
				}

				if (entryCollisionCount == -1) {
					// Directory
					dir.getSubFiles().add(loadIndexing(sourceDir, entryId, dir, null));
				} else {
					// File
					Main.addHashCollisionNumber(entryId, entryCollisionCount);
					dir.getSubFiles().add(new IndexingFile(dir, entryName, entryId, entryCollisionCount, paddingAndOverhead, fileSize));
				}
			}

			if (nextIndex != -1) {
				loadIndexing(sourceDir, nextIndex, parent, dir);
			}

			return dir;

		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	static long[] getAmount(IndexingEntry en) {
		if (en instanceof IndexingDir) {
			IndexingDir dir = (IndexingDir) en;
			long[] amount = { 0, 1 };
			for (IndexingEntry e : dir.getSubFiles()) {
				long[] r = getAmount(e);
				amount[0] += r[0];
				amount[1] += r[1];
			}
			return amount;
		} else {
			long[] amount = { 1, 0 };
			return amount;
		}
	}

	static void copy(IndexingFile toCopy, IndexingFile target, File sourcePath) {
		IndexingFile indf = (IndexingFile) toCopy;
		if (indf.getPaddingAndOverhead().length == 2) {
			File f = new File(sourcePath, indf.getHashId() + "." + indf.getCollsionCount() + "._.png");
			if (f.exists()) {
				try {
					Files.copy(f.toPath(), new File(sourcePath, indf.getHashId() + "." + target.getCollsionCount() + "._.png").toPath(),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			for (int i = 0; i < indf.getPaddingAndOverhead().length / 2; i++) {
				File f = new File(sourcePath, indf.getHashId() + "." + indf.getCollsionCount() + "." + i + ".png");
				if (f.exists()) {
					try {
						Files.copy(f.toPath(), new File(sourcePath, indf.getHashId() + "." + target.getCollsionCount() + "." + i + ".png").toPath(),
								StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		target.getParent().getSubFiles().add(target);
		saveIndexing(sourcePath, target.getParent());
	}

	static void copy(IndexingDir toCopy, IndexingDir target, File sourcePath) {
		for (IndexingEntry en : toCopy.getSubFiles()) {
			if (en instanceof IndexingFile) {
				IndexingFile file = (IndexingFile) en;
				int collisions = Main.getFreeHashCollisionNumber(file.getHashId());
				Main.addHashCollisionNumber(file.getHashId(), collisions);
				IndexingFile newTargetFile = new IndexingFile(target, file.getName(), file.getHashId(), collisions, file.getPaddingAndOverhead()
						.clone(), file.getSize());
				copy(file, newTargetFile, sourcePath);
			} else {
				IndexingDir dir = (IndexingDir) en;
				IndexingDir newTarget = new IndexingDir(target, dir.getName(), Main.getAndUseNextFreeIndex());
				target.getSubFiles().add(newTarget);
				copy(dir, newTarget, sourcePath);
				saveIndexing(sourcePath, newTarget);
			}
		}
		saveIndexing(sourcePath, target);
	}

	static long getSize(IndexingEntry en) {
		if (en instanceof IndexingDir) {
			IndexingDir dir = (IndexingDir) en;
			long size = 0l;
			for (IndexingEntry e : dir.getSubFiles()) {
				size += getSize(e);
			}
			return size;
		} else {
			IndexingFile file = (IndexingFile) en;
			return file.getSize();
		}
	}

	private static byte[] getByteArray(ByteBuffer buffer, int nameSize) {
		byte[] array = new byte[nameSize];
		for (int i = 0; i < nameSize; i++) {
			array[i] = buffer.get();
		}
		return array;
	}

	static ArrayList<String> printIndexingStructure(IndexingDir dir, int depth, int maxDepth) {
		ArrayList<String> text = new ArrayList<String>();

		String pre = "";
		int w = 5;
		for (int i = 0; i < depth * w; i++) {
			if (i % w == 0) {
				pre += "|";
			} else {
				pre += " ";
			}
		}
		if (depth == 0) {
			pre = pre + "./";
		}

		if (dir == null) {
			text.add(pre + "No indexing found.");
			return text;
		}

		for (IndexingEntry en : dir.getSubFiles()) {
			if (en instanceof IndexingFile) {
				IndexingFile f = (IndexingFile) en;
				text.add(pre + en.getName() + " (" + Tool.readableFileSize(f.getSize()) + ") <" + f.getHashId() + "." + f.getCollsionCount()
						+ ".0.png> [" + (f.getPaddingAndOverhead().length / 2) + "]");
			} else if (en instanceof IndexingDir) {
				IndexingDir d = (IndexingDir) en;

				if (maxDepth > depth) {
					text.add(pre + en.getName() + "/");
					text.addAll(printIndexingStructure(d, depth + 1, maxDepth));
				} else {
					text.add(pre + en.getName() + "/");
				}
			} else {
				text.add(pre + en.getName() + "!!!");
			}
		}

		return text;
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

	public static void delete(IndexingEntry en, File sourcePath) {
		if (en.getParent() == null) {
			System.out.println("Can't delete root.");
			return;
		}
		en.getParent().getSubFiles().remove(en);
		saveIndexing(sourcePath, en.getParent());

		if (en instanceof IndexingFile) {
			IndexingFile indf = (IndexingFile) en;
			if (indf.getPaddingAndOverhead().length == 2) {
				File f = new File(sourcePath, indf.getHashId() + "." + indf.getCollsionCount() + "._.png");
				if (f.exists()) {
					f.delete();
				}
				Main.removeHashCollision(indf.getHashId(), indf.getCollsionCount());
			} else {
				for (int i = 0; i < indf.getPaddingAndOverhead().length / 2; i++) {
					File f = new File(sourcePath, indf.getHashId() + "." + indf.getCollsionCount() + "." + (i) + ".png");
					if (f.exists()) {
						f.delete();
					}
				}
				Main.removeHashCollision(indf.getHashId(), indf.getCollsionCount());
			}
		} else {
			IndexingDir dir = (IndexingDir) en;
			@SuppressWarnings("unchecked")
			HashSet<IndexingEntry> cl = (HashSet<IndexingEntry>) dir.getSubFiles().clone();
			for (IndexingEntry e : cl) {
				delete(e, sourcePath);
			}
			for (Integer i : dir.getIds()) {
				File f = new File(sourcePath, "index." + i + ".png");
				if (f.exists()) {
					f.delete();
				}
				Main.setIndexUsed(i, false);
			}
		}
	}

	public static void move(IndexingFile en, IndexingFile target, File sourcePath) {
		copy(en, target, sourcePath);
		delete(en, sourcePath);
	}

	public static void move(IndexingDir source, IndexingDir target, File sourcePath) {
		if (source.getParent() == null) {
			System.out.println("Can't move root");
			return;
		}
		if (isSubFileOf(target, source)) {
			System.out.println("Target is not allowed to be a subfile of source!");
			return;
		}
		for (IndexingEntry e : source.getSubFiles()) {
			target.getSubFiles().add(e);
			e.setParent(target);
		}
		source.getSubFiles().clear();
		delete(source, sourcePath);
		saveIndexing(sourcePath, target);
		return;
	}

	static boolean isSubFileOf(IndexingEntry file, IndexingDir hyperParent) {
		if (file == hyperParent) {
			return true;
		}
		IndexingDir dir = file.getParent();
		while (dir != null) {
			if (dir == hyperParent) {
				return true;
			}
			dir = dir.getParent();
		}
		return false;
	}

}
