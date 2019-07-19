package de.christian.f.brinkmann.ibus;

import java.text.DecimalFormat;

public class Tool {

	public static String readableFileSize(long size) {
		if (size <= 0)
			return "0";
		final String[] units = { "B", "kB", "MB", "GB", "TB", "PB", "EB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
	
	public static String readableNanoTime(long size) {
		if (size <= 0)
			return "0";
		final String[] units = { "ns", "microseconds", "ms", "s", "s^1000" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1000));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1000, digitGroups)) + " " + units[digitGroups];
	}
	
	public static String reduceFactor(long input, int factor) {
		return new DecimalFormat("#,###,##0.000").format(input / Math.pow(10.0, factor));
	}

}
