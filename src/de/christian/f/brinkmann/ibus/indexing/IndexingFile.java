package de.christian.f.brinkmann.ibus.indexing;

public class IndexingFile extends IndexingEntry {

	private int hashId;
	private int collsionCount;
	private int[] paddingAndOverhead;

	public IndexingFile(IndexingDir parent, String name, int hashId, int collisionCount, int[] paddingAndOverhead) {
		super(parent, name);
		this.hashId = hashId;
		this.collsionCount = collisionCount;
		this.paddingAndOverhead = paddingAndOverhead;
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

}
