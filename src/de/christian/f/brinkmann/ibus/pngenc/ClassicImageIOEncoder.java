package de.christian.f.brinkmann.ibus.pngenc;

import java.io.File;

import de.christian.f.brinkmann.ibus.FileIO;
import de.christian.f.brinkmann.ibus.ImageCreator;

public class ClassicImageIOEncoder extends PngEncoder {

	@Override
	public void createImage(File file, int imageSize, byte[] pixelparts) {
		FileIO.writeImageToPNG(ImageCreator.createImage(imageSize, pixelparts), file);
	}

}
