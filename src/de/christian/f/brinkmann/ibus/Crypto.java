package de.christian.f.brinkmann.ibus;

import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class Crypto {
	
	private static final String ALGO = "AES";
	private static byte[] keyValue = null; // 16 Byte
	private static Key key;
	
	public static boolean isEncryptionActivated() {
		return keyValue != null;
	}
	
	public static byte[] encrypt(byte[] data) throws Exception {
		Cipher c = Cipher.getInstance(ALGO);
		c.init(Cipher.ENCRYPT_MODE, key);
		return c.doFinal(data);
	}

	public static byte[] decrypt(byte[] encryptedData) throws Exception {
		Cipher c = Cipher.getInstance(ALGO);
		c.init(Cipher.DECRYPT_MODE, key);
		return c.doFinal(encryptedData);

	}

	static void setKey(byte[] newKey) throws Exception {
		keyValue = newKey;
		key = new SecretKeySpec(keyValue, ALGO);
	}

	public static String decryptString(String encryptedData) throws Exception {
		Cipher c = Cipher.getInstance(ALGO);
		c.init(Cipher.DECRYPT_MODE, key);
		byte[] decordedValue = new BASE64Decoder().decodeBuffer(encryptedData);
		byte[] decValue = c.doFinal(decordedValue);
		String decryptedValue = new String(decValue);
		return decryptedValue;
	}

	public static String encryptString(String Data) throws Exception {
		Cipher c = Cipher.getInstance(ALGO);
		c.init(Cipher.ENCRYPT_MODE, key);
		byte[] encVal = c.doFinal(Data.getBytes());
		String encryptedValue = new BASE64Encoder().encode(encVal);
		return encryptedValue;
	}
}
