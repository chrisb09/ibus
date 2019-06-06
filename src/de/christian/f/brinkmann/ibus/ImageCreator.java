package de.christian.f.brinkmann.ibus;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class ImageCreator {

	public static BufferedImage createImage(int imageSize, int spotSize, byte[] data, int[] encoding) {
		System.out.println("EncS: " + encoding.length);
		BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.createGraphics();
		Color[] colors = encodingToColor(encoding);
		int max = encoding.length == 256 ? data.length : data.length * 2;
		for (int i = 0; i < max; i++) {
			int encodIndex = getEncodingIndex(data[encoding.length == 256 ? i : i / 2], i % 2 == 0, encoding.length);
			if (encodIndex < 0)
				encodIndex = 256 + encodIndex;
			System.out.println("EncIn: " + encodIndex);
			g.setColor(colors[encodIndex]);
			g.fillRect((i * spotSize) % imageSize, spotSize * ((i * spotSize) / imageSize), spotSize, spotSize);
		}
		g.dispose();
		return image;
	}

	public static BufferedImage createImage(int imageSize, int[] pixels) {
		BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.createGraphics();
		final int spotSize = 1;
		for (int i = 0; i < pixels.length; i++) {
			g.setColor(new Color(pixels[i]));
			g.fillRect((i * spotSize) % imageSize, spotSize * ((i * spotSize) / imageSize), spotSize, spotSize);
		}
		g.dispose();
		return image;
	}

	public static BufferedImage createImage(int imageSize, byte[] pixelparts) {
		BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB);
		boolean other = false;
		for (int i = 0; i < pixelparts.length; i += 3) {
			int color = (toInt(pixelparts[i]) << 16) | (toInt(pixelparts[i + 1]) << 8) | toInt(pixelparts[i + 2]);
			if (color > 256) {
				other = true;
			}
			image.setRGB((i / 3) % imageSize, (i / 3) / imageSize, color);
		}
		if (other) {
			System.out.println("Klappt.");
		}
		int rgb = (pixelparts[0] << 16) | (pixelparts[1] << 8) | pixelparts[2];
		System.out.println("rgb: " + rgb + "(" + toInt(pixelparts[0]) + "," + toInt(pixelparts[1]) + "," + toInt(pixelparts[2]) + ")");
		System.out.println("real: " + image.getRGB(0, 0));
		return image;
	}

	private static int toInt(byte b) {
		return (b < 0 ? 256 + b : (int) b);
	}

	private static int getEncodingIndex(byte data, boolean even, int encodingSize) {
		if (encodingSize == 256) {
			return data;
		} else if (encodingSize == 16) {
			if (even) {
				return (data >> 4) & (byte) (0b00001111);
			} else {
				return data & (byte) (0b00001111);
			}
		}
		return data;
	}

	private static Color[] encodingToColor(int[] encoding) {
		Color[] colors = new Color[encoding.length];
		for (int i = 0; i < encoding.length; i++) {
			colors[i] = new Color(encoding[i]);
		}
		return colors;
	}

	public static BufferedImage createImageAlpha(int imageSize, byte[] pixelparts) {
		BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < pixelparts.length; i += 4) {
			int color = (toInt(pixelparts[i]) << 24) | (toInt(pixelparts[i + 1]) << 16) | (toInt(pixelparts[i + 2]) << 8) | toInt(pixelparts[i + 3]);
			image.setRGB((i / 4) % imageSize, (i / 4) / imageSize, color);
		}
		return image;
	}

}
