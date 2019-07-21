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
		final String[] units = { "ns", "microseconds", "ms", "s", "min", "h", "d", "w", "mo", "y", "cent", "mil" };
		final long[] unitsizerel = { 1l, 1000l, 1000l, 1000l, 60l, 60l, 24l, 7l, 4l, 52l, 100l, 10l , Long.MAX_VALUE};
		int f = 0;
		long g = 1l;
		while (g*unitsizerel[f+1]<size){
			f++;
			g *= unitsizerel[f];
		}
		return new DecimalFormat("#,##0.#").format(size / (double) g) + "" + units[f];
	}

	public static String reduceFactor(long input, int factor) {
		return new DecimalFormat("#,###,##0.000").format(input / Math.pow(10.0, factor));
	}

}
