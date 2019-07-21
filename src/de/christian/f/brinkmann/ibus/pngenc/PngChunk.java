package de.christian.f.brinkmann.ibus.pngenc;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public final class PngChunk {
	public static final byte[] SIGNATURE = { -119, 80, 78, 71, 13, 10, 26, 10 };
	private static final byte[] IHDR = { 73, 72, 68, 82 };
	private static final byte[] PLTE = { 80, 76, 84, 69 };
	private static final byte[] tRNS = { 116, 82, 78, 83 };
	private static final byte[] IDAT = { 73, 68, 65, 84 };
	private static final byte[] IEND = { 73, 69, 78, 68 };
	private final byte[] length;
	private final byte[] name;
	private final byte[] data;

	private PngChunk(byte[] length, byte[] name, byte[] data) {
		this.length = length;
		this.name = name;
		this.data = data;
	}

	public static PngChunk createHeaderChunk(int width, int height, byte bitDepth, byte colorType, byte compression, byte filter, byte interlace) {
		ByteBuffer buff = ByteBuffer.allocate(13);
		buff.putInt(width);
		buff.putInt(height);
		buff.put(bitDepth);
		buff.put(colorType);
		buff.put(compression);
		buff.put(filter);
		buff.put(interlace);
		byte[] data = buff.array();
		return new PngChunk(intToBytes(13), IHDR, data);
	}

	public static PngChunk createPaleteChunk(byte[] palBytes) {
		return new PngChunk(intToBytes(palBytes.length), PLTE, palBytes);
	}

	public static PngChunk createTrnsChunk(byte[] trnsBytes) {
		return new PngChunk(intToBytes(trnsBytes.length), tRNS, trnsBytes);
	}

	public static PngChunk createDataChunk(byte[] zLibBytes) {
		return new PngChunk(intToBytes(zLibBytes.length), IDAT, zLibBytes);
	}

	public static PngChunk createEndChunk() {
		return new PngChunk(intToBytes(0), IEND, new byte[0]);
	}

	public byte[] getCRCValue() {
		CRC32 crc32 = new CRC32();
		crc32.update(name);
		crc32.update(data);
		byte[] temp = longToBytes(crc32.getValue());
		return new byte[] { temp[4], temp[5], temp[6], temp[7] };
	}

	public static byte[] intToBytes(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	private static byte[] longToBytes(long value) {
		return new byte[] { (byte) (int) (value >> 56), (byte) (int) (value >> 48), (byte) (int) (value >> 40), (byte) (int) (value >> 32),
				(byte) (int) (value >> 24), (byte) (int) (value >> 16), (byte) (int) (value >> 8), (byte) (int) value };
	}

	public byte[] getLength() {
		return length;
	}

	public byte[] getName() {
		return name;
	}

	public byte[] getData() {
		return data;
	}
}
