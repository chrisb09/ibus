package de.christian.f.brinkmann.ibus;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import javax.imageio.ImageIO;

public class FileIO {

	static byte[] readFileAsBytes(File file) {
		if (Metric.active != null){
			Metric.active.startLoading();
		}
		byte[] data = null;
		try {
			data = Files.readAllBytes(file.toPath());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (Metric.active != null){
			Metric.active.endLoading();
		}
		return data;
	}

	static boolean writeFileAsBytes(File file, byte[] data) {
		if (Metric.active != null){
			Metric.active.startWriting();
		}
		try {
			Files.write(file.toPath(), data, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			if (Metric.active != null){
				Metric.active.endWriting();
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			if (Metric.active != null){
				Metric.active.endWriting();
			}
			return false;
		}
	}

	static boolean appendFileAsBytes(File file, byte[] data) {
		if (Metric.active != null){
			Metric.active.startWriting();
		}
		try {
			Files.write(file.toPath(), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			if (Metric.active != null){
				Metric.active.endWriting();
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			if (Metric.active != null){
				Metric.active.endWriting();
			}
			return false;
		}
	}

	public static void writeImageToPNG(RenderedImage image, File file) {
		File f = new File(file.getAbsolutePath() + ".png");
		if (Metric.active != null){
			Metric.active.startWriting();
		}
		try {
			ImageIO.write(image, "png", f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (Metric.active != null){
			Metric.active.endWriting();
		}
	}

}
