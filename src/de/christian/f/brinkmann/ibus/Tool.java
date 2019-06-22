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

}
