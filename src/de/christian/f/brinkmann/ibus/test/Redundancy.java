package de.christian.f.brinkmann.ibus.test;

public enum Redundancy {

	NONE, HAMMING;

	int getDataSize(int rawSize, int redundancyLevel) {
		if (this == NONE) {
			return rawSize;
		} else if (this == HAMMING) {
			int k = 2;
			int total = 3;
			while (total < rawSize) {
				k++;
				total = (1 << k) - 1;
			}
			k--;
			return (1 << k) - k - 1;
		}
		return -1;
	}
}
