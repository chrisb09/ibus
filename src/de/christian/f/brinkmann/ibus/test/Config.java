package de.christian.f.brinkmann.ibus.test;


public class Config {

	private int maxImageSize = 1024;
	private int spotSize = 8;
	private int encodingLevel = 1;
	private Redundancy redundancy = Redundancy.NONE;
	private int redundancyParts = 0;
	private int redundancyLevel = 1;

	public int getMaxImageSize() {
		return maxImageSize;
	}

	public int getSpotSize() {
		return spotSize;
	}

	public int getEncodingLevel() {
		return encodingLevel;
	}

	public Redundancy getRedundancy() {
		return redundancy;
	}

	public int getRedundancyParts() {
		return redundancyParts;
	}

	public int getRedundancyLevel() {
		return redundancyLevel;
	}

}
