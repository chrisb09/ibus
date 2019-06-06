package de.christian.f.brinkmann.ibus;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

public class Main {

	public static void main(String[] args) {

		// BufferedImage image = ImageCreator.createImage(null, null);
		// FileIO.writeImageToPNG(image, new File("test"));

		// EncodingGeneration.showEnc16AsImg();
		// EncodingGeneration.showEnc256AsImg();
		// EncodingGeneration enc2 = new EncodingGeneration(256);
		// enc2.generate();

		// FileIO.writeFileAsBytes(new File("enc16_copy.png"),
		// FileIO.readFileAsBytes(new File("enc16.png")));

		//Encoder.encodeFile(new File("enc256.png"), new File("test.txt").getParentFile());
		
		Encoder.encodeDirectoryAlpha(new File("source"), new File("target"));
		Decoder.decodeDirectoryAlpha(new File("target"), new File("result"));
		
		//Encoder.encodeFileAlpha(new File("enc256.png"), new File("test.txt").getParentFile(),0);
		
		//Decoder.decodeFile(new File("enc256_copy.png.41._.png"), new File("test.txt").getParentFile());

		System.out.println("Created.");
	}
	
	@SuppressWarnings("unused")
	private void test() {
		byte[] testData = FileIO.readFileAsBytes(new File("enc256.png"));
		int size = (int) Math.sqrt(testData.length/3);
		if (size*size*3<testData.length)
			size++;
		byte[] dataCopy = new byte[size*size*3];
		for (int i=0;i<testData.length;i++){
			dataCopy[i] = testData[i];
		}
		BufferedImage tmp = ImageCreator.createImage(size, dataCopy);
		byte[] read = ImageReader.readImage(tmp);

		System.out.println("Orig: "+Arrays.toString(testData));
		System.out.println("Real: "+Arrays.toString(read));
		
		boolean eq = true;
		for (int i=0;i<dataCopy.length;i++){
			if (dataCopy[i]!=read[i]){
				eq = false;
			}
		}
		System.out.println("Equal: "+eq);
	}

}
