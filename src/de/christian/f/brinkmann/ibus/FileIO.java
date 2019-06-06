package de.christian.f.brinkmann.ibus;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import javax.imageio.ImageIO;

public class FileIO {

	static byte[] readFileAsBytes(File file) {
		try {
			return Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	static boolean writeFileAsBytes(File file, byte[] data) {
		try {
			Files.write(file.toPath(), data, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	static boolean appendFileAsBytes(File file, byte[] data) {
		try {
			Files.write(file.toPath(), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void writeImageToPNG(RenderedImage image, File file) {
		File f = new File(file.getAbsolutePath() + ".png");
		try {
			ImageIO.write(image, "png", f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
