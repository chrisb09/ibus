package de.christian.f.brinkmann.ibus;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class Decoder {

	static File[] decodeFile(File source, File targetDir) {
		Main.sizeInBytes += source.length();
		String name = source.getName();
		String[] parts = name.split(Pattern.quote("."));
		String packetNr = parts[parts.length - 2];
		String emptyBits = parts[parts.length - 3];
		String recursionDepthString = parts[parts.length - 4];
		int recursionDepth = Integer.parseInt(recursionDepthString);
		String[] paths = new String[recursionDepth];
		for (int i = 0; i < recursionDepth; i++) {
			paths[i] = parts[parts.length - (4 + recursionDepth - i)];
		}
		File targetPath = targetDir;
		if (!targetPath.exists()) {
			targetPath.mkdirs();
		}
		for (String p : paths) {
			targetPath = new File(targetPath, p);
			if (!targetPath.exists()) {
				targetPath.mkdirs();
			}
		}
		String origName = "";
		for (int i = 0; i < parts.length - (recursionDepth + 4); i++) {
			if (i == 0) {
				origName = parts[i];
			} else {
				origName += "." + parts[i];
			}
		}
		int emptyB = Integer.parseInt(emptyBits);
		if (packetNr.equals("_")) {
			// One packet
			try {
				byte[] data = ImageReader.readImage(ImageIO.read(source));
				byte[] dataCopy = new byte[data.length - emptyB];
				for (int i = 0; i < dataCopy.length; i++) {
					dataCopy[i] = data[i];
				}
				File t = new File(targetPath, origName);
				FileIO.writeFileAsBytes(t, dataCopy);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// Multiple packets
			int pId = Integer.parseInt(packetNr);
			if (emptyB != 0) {
				File[] files = new File[pId + 1];
				files[pId] = source;
				int id = pId - 1;
				while (id >= 0) {
					File file = getFile(source, parts, id);
					files[id] = file;
					id--;
				}

				try {
					File t = new File(targetPath, origName);
					for (int i = 0; i < files.length; i++) {
						byte[] data = ImageReader.readImage(ImageIO.read(files[i]));
						byte[] dataCopy;
						if (i != files.length - 1) {
							dataCopy = data;
						} else {
							int ignoreBytes = emptyB;
							if (ignoreBytes == -1) {
								ignoreBytes = 0;
							}
							dataCopy = new byte[data.length - ignoreBytes];
						}
						FileIO.appendFileAsBytes(t, dataCopy);
					}
					return files;
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}

			} else {
				return null;
			}
		}
		File[] array = { source };
		return array;
	}

	private static File getFile(File source, String[] parts, int nr) {
		String s = parts[0] + "";
		for (int i = 1; i < parts.length; i++) {
			if (i == parts.length - 2) {
				s += "." + nr;
			} else if (i == parts.length - 3) {
				s += ".0";
			} else {
				s += "." + parts[i];
			}
		}
		File f = new File(source.getParentFile(), s);
		return f;
	}

	static void decodeDirectory(File sourceDir, File targetDir) {
		if (sourceDir.exists()) {
			for (File f : sourceDir.listFiles()) {
				if (f.isDirectory() == false) {
					try {
						decodeFile(f, targetDir);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}

}
