package de.christian.f.brinkmann.ibus.indexing;

public abstract class IndexingEntry {

	private String name;
	private IndexingDir parent;

	public IndexingEntry(IndexingDir parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public IndexingDir getParent() {
		return parent;
	}

	public void setParent(IndexingDir parent) {
		this.parent = parent;
	}

}
