package de.christian.f.brinkmann.ibus.indexing;

import java.util.ArrayList;
import java.util.HashSet;

import de.christian.f.brinkmann.ibus.Main;

public class IndexingDir extends IndexingEntry {

	private HashSet<IndexingEntry> subFiles;
	private ArrayList<Integer> ids;

	public IndexingDir(IndexingDir parent, String name, int id) {
		super(parent, name);
		subFiles = new HashSet<IndexingEntry>();
		ids = new ArrayList<Integer>();
		ids.add(id);
		Main.setIndexUsed(id,true);
	}
	
	public ArrayList<Integer> getIds() {
		return ids;
	}

	public HashSet<IndexingEntry> getSubFiles() {
		return subFiles;
	}
}
