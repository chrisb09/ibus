package de.christian.f.brinkmann.ibus;

import java.io.File;

public class Main {

	public static void main(String[] args) {
		Encoder.encodeDirectoryAlpha(new File("source"), new File("target"));
		Decoder.decodeDirectory(new File("target"), new File("result"));

		System.out.println("Created.");
	}

}
