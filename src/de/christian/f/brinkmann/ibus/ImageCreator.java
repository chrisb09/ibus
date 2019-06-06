package de.christian.f.brinkmann.ibus;

import java.awt.image.BufferedImage;

public class ImageCreator {

	private static int toInt(byte b) {
		return (b < 0 ? 256 + b : (int) b);
	}

	public static BufferedImage createImage(int imageSize, byte[] pixelparts) {
		BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < pixelparts.length; i += 4) {
			int color = (toInt(pixelparts[i]) << 24) | (toInt(pixelparts[i + 1]) << 16) | (toInt(pixelparts[i + 2]) << 8) | toInt(pixelparts[i + 3]);
			image.setRGB((i / 4) % imageSize, (i / 4) / imageSize, color);
		}
		return image;
	}

}
