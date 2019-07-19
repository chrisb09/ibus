package de.christian.f.brinkmann.ibus.indexing;

public class IndexingFile extends IndexingEntry {

	private int hashId;
	private int collsionCount;
	private int[] paddingAndOverhead;
	private long size;

	public IndexingFile(IndexingDir parent, String name, int hashId, int collisionCount, int[] paddingAndOverhead, long size) {
		super(parent, name);
		this.hashId = hashId;
		this.collsionCount = collisionCount;
		this.paddingAndOverhead = paddingAndOverhead;
		this.size = size;
	}
	
	public long getSize() {
		return size;
	}

	public int[] getPaddingAndOverhead() {
		return paddingAndOverhead;
	}

	public int getHashId() {
		return hashId;
	}

	public int getCollsionCount() {
		return collsionCount;
	}

	public void setPaddingAndOverhead(int[] clone) {
		paddingAndOverhead = clone;
	}

	public void setSize(long size2) {
		size = size2;
	}

}
