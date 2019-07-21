package de.christian.f.brinkmann.ibus;

import java.io.File;

import de.christian.f.brinkmann.ibus.indexing.IndexingDir;
import de.christian.f.brinkmann.ibus.indexing.IndexingFile;

public class EncoderQueueEntry {

	IndexingFile indf;
	IndexingDir parent;
	File source;
	File targetDir;
	int collisions;

	public EncoderQueueEntry(IndexingFile indf, IndexingDir parent, File source, File targetDir, int collisions) {
		this.indf = indf;
		this.parent = parent;
		this.source = source;
		this.targetDir = targetDir;
		this.collisions = collisions;
	}

	public IndexingFile getIndf() {
		return indf;
	}

	public IndexingDir getParent() {
		return parent;
	}

	public File getSource() {
		return source;
	}

	public File getTargetDir() {
		return targetDir;
	}

	public int getCollisions() {
		return collisions;
	}

}