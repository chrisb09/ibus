package de.christian.f.brinkmann.ibus.indexing;

import java.util.ArrayList;

public class IndexingDir extends IndexingEntry {

	private ArrayList<IndexingEntry> subFiles;
	private ArrayList<Integer> ids;

	public IndexingDir(IndexingDir parent, String name, int id) {
		super(parent, name);
		subFiles = new ArrayList<IndexingEntry>();
		ids = new ArrayList<Integer>();
		ids.add(id);
	}

	public void addId(int id) {
		ids.add(id);
	}

	public ArrayList<IndexingEntry> getSubFiles() {
		return subFiles;
	}
}
