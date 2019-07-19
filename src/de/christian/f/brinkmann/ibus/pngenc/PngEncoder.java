package de.christian.f.brinkmann.ibus.pngenc;

import java.io.File;

public abstract class PngEncoder {

	public abstract void createImage(File file, int imageSize, byte[] pixelparts);

}
