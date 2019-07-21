package de.christian.f.brinkmann.ibus.pngenc;

import java.awt.Image;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import de.christian.f.brinkmann.ibus.ImageCreator;

public class ObjectplanetPngEncoder extends PngEncoder {
	
	private com.objectplanet.image.PngEncoder p;
	
	public ObjectplanetPngEncoder() {
		p = new com.objectplanet.image.PngEncoder();
		p.setCompression(com.objectplanet.image.PngEncoder.BEST_SPEED);
		p.setColorType(com.objectplanet.image.PngEncoder.COLOR_TRUECOLOR_ALPHA);
	}

	@Override
	public void createImage(File file, int imageSize, byte[] pixelparts) {
		File f = new File(file.getAbsolutePath() + ".png");
		Image image = ImageCreator.createImage(imageSize, pixelparts);
		try {
			FileOutputStream s = new FileOutputStream(f);
			p.encode(image, s);
			s.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
