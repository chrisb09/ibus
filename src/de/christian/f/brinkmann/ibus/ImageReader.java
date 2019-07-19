package de.christian.f.brinkmann.ibus;

import java.awt.image.BufferedImage;

public class ImageReader {

	public static byte[] readImage(BufferedImage image) {
		if (Metric.active!=null){
			Metric.active.startLoading();
		}
		byte[] data = new byte[4 * image.getWidth() * image.getHeight()];
		for (int i = 0; i < data.length; i += 4) {
			int rgb = image.getRGB((i / 4) % image.getWidth(), (i / 4) / image.getWidth());
			data[i] = (byte) ((rgb >> 24) & 0b11111111);
			data[i + 1] = (byte) ((rgb >> 16) & 0b11111111);
			data[i + 2] = (byte) ((rgb >> 8) & 0b11111111);
			data[i + 3] = (byte) (rgb & 0b11111111);
		}
		if (Metric.active!=null){
			Metric.active.endLoading();
		}
		return data;
	}

}
