package de.christian.f.brinkmann.ibus;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;

public class FileIO {

	private static JPEGImageWriteParam jpegParams;

	static {
		jpegParams = new JPEGImageWriteParam(null);
		setJPGCompression(1f);
	}
	
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

	static void writeImageToJPG(RenderedImage image, File file) {
		File f = new File(file.getAbsolutePath() + ".jpg");
		final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
		try {
			writer.setOutput(new FileImageOutputStream(f));
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			writer.write(null, new IIOImage(image, null, null), jpegParams);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void setJPGCompression(float compression) {
		jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpegParams.setCompressionQuality(1f);
	}

}
