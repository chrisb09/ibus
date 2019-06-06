package de.christian.f.brinkmann.ibus.test;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import de.christian.f.brinkmann.ibus.FileIO;
import de.christian.f.brinkmann.ibus.ImageCreator;

public class EncodingGeneration {

	private static final int R = 1 << 16;
	private static final int G = 1 << 8;
	private static final int B = 1;

	//@formatter:off
	private static final int[] neighbors = {-R-G-B,-R-G,-R-G+B, -R-B,-R,-R+B, -R+G-B,-R+G,-R+G+B, 
											-G-B,-G,-G+B, -B,0,B, G-B,G,G+B, 
											R-G-B,R-G,R-G+B, R-B,R,R+B, R+G-B,R+G,R+G+B};
	//@formatter:on

	int index;
	int[] codings;

	EncodingGeneration(int amount) {
		int[] c = { 16711680, 65280, 255, 32639, 8355584, 8323199, 0, 16777215, 16711935, 65535, 16776960, 16777023, 16760640, 12582719, 4194269,
				4177919, 16728063, 16720063, 12525823, 63, 16128, 4128768, 524214, 13291986, 3010610, 987086, 13504271, 16760527, 3226323, 16757248,
				13745151, 1231622, 16724531, 4143103, 14221251, 12184320, 4509379, 12727091, 14304459, 3258932, 3932401, 3800847, 11599806, 4456447,
				1949152, 12779519, 16777151, 786234, 14530881, 16711742, 12386551, 16711870, 10631487, 4079014, 409343, 16726272, 966143, 8007,
				4267776, 2047519, 12434496, 14615740, 2097217, 12336834, 376610, 4783935, 4252422, 17123, 2293947, 12731135, 14856964, 14958336,
				14942276, 4128599, 12373432, 16761599, 4390851, 5111845, 4982231, 246999, 4539712, 4699832, 12658177, 255302, 2574082, 16762807,
				12174847, 84008, 534712, 10896827, 15154504, 2670336, 16730573, 12047370, 4638277, 12322088, 12695493, 1464, 12975881, 5065471,
				16729169, 1103287, 12124375, 177664, 12191232, 16762125, 849645, 3322295, 446792, 13698635, 13715713, 16731322, 5360639, 4803516,
				12400816, 13350834, 16731450, 13365302, 16756991, 11597810, 413392, 4902931, 4141889, 11749196, 12975952, 11483377, 5027313, 3408556,
				11381774, 5049087, 4698677, 5111569, 55477, 16758449, 11546126, 11783240, 4664835, 601675, 11599695, 11948799, 13369599, 15610619,
				15584077, 16777038, 1568511, 15600077, 16772810, 15593721, 13503673, 1158573, 701234, 1461759, 987641, 4273456, 11647407, 3649610,
				3082822, 12235343, 16715599, 46551, 5046016, 3407807, 3607312, 5046275, 13331, 15683251, 15681791, 16772630, 15659008, 16716782,
				65613, 1053696, 789264, 11864655, 997557, 1177935, 16714000, 16730638, 11883503, 15707201, 12172271, 3557953, 3731377, 11317581,
				4797888, 4976716, 3165456, 314367, 46591, 11996887, 16773301, 3862527, 3190785, 14240790, 11487799, 519954, 5000432, 11530442,
				347142, 15728465, 16773106, 5177397, 5183159, 4933454, 19711, 5097218, 11549696, 15788312, 3539199, 11556786, 4961478, 5027916,
				12966674, 936497, 15846067, 4912568, 14923026, 13374799, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		codings = c;
		index = 8;
		while (codings[index] != 0) {
			index++;
		}
		System.out.println("Start at index="+index);
	}

	int[] generate() {
		System.out.println(" U index="+index);
		while (index < codings.length) {
			codings[index] = generateNext();
			System.out.println("coding[" + (index) + "]: (" + getR(codings[index]) + "," + getG(codings[index]) + "," + getB(codings[index]) + ")");
			index++;
			String text = "" + codings[0];
			for (int i = 0; i < codings.length; i++) {
				if (i != 0) {
					text += "," + codings[i];
				}
			}
			try {
				File f = new File("coding_" + codings.length + "." + (index - 1) + ".txt");
				if (f.exists())
					f.delete();
				writeInFile(new File("coding_" + codings.length + "." + index + ".txt"), text);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		String text = "" + codings[0];
		for (int i = 0; i < codings.length; i++) {
			if (i != 0) {
				text += "," + codings[i];
			}
		}
		System.out.println("Codings:" + text);
		try {
			writeInFile(new File("coding_" + codings.length + ".txt"), text);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return codings;
	}
	
	private class aQueue implements Queue {
		
		int[] array;
		
		int head;
		int tail;
		
		public aQueue(int size) {
			array = new int[size];
			head = 0;
			tail = 0;
		}

		@Override
		public boolean addAll(Collection arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void clear() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean contains(Object arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean containsAll(Collection arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isEmpty() {
			return head == tail;
		}

		@Override
		public Iterator iterator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean remove(Object arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean removeAll(Collection arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean retainAll(Collection arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int size() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object[] toArray() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object[] toArray(Object[] arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean add(Object arg0) {
			array[tail] = (Integer) arg0;
			tail++;
			return true;
		}

		@Override
		public Object element() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean offer(Object arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Object peek() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object poll() {
			Integer t = array[head];
			head++;
			return t;
		}

		@Override
		public Object remove() {
			// TODO Auto-generated method stub
			return null;
		}
		
		
	}

	int[] alreadyMarked;
	Queue<Integer> q;

	private int generateNext() {
		long start = System.currentTimeMillis();
		int lastMarked = -1;
		alreadyMarked = new int[256 * 256 * 256 / 32];
		q = new aQueue(256 * 256 * 256 * 4);//new LinkedList<Integer>();

		for (int x = 0; x < index; x++) {
			int a = codings[x];
			for (int y = 0; y < index; y++) {
				int b = codings[y];
				int i = getRGBAsInt(getR(a) / 2 + getR(b) / 2, getG(a) / 2 + getG(b) / 2, getB(a) / 2 + getB(b) / 2);
				q.add(i);
				setAlreadyMarked(i, alreadyMarked);
			}
		}

		while (!q.isEmpty()) {
			int current = q.poll();
			//if (isAlreadyMarked(current, alreadyMarked)) {
			//	continue;
			//}
			//setAlreadyMarked(current, alreadyMarked);
			int r = getR(current);
			int g = getG(current);
			int b = getB(current);
			for (int i = 0; i < 27; i++) {
				if (i == 13) {
					continue;
				}
				int next = current;
				if (i < 9) {
					if (r == 0) {
						continue;
					}
					next -= R;
				}
				if (i > 18) {
					if (r == 255) {
						continue;
					}
					next += R;
				}
				if (i % 9 < 3) {
					if (g == 0) {
						continue;
					}
					next -= G;
				}
				if (i % 9 >= 6) {
					if (g == 255) {
						continue;
					}
					next += G;
				}
				if (i % 3 == 0) {
					if (b == 0) {
						continue;
					}
					next -= B;
				}
				if (i % 3 == 2) {
					if (b == 255) {
						continue;
					}
					next += B;
				}

				if (!isAlreadyMarked(next, alreadyMarked)) {
					// Mark next
					lastMarked = next;
					q.add(next);
					setAlreadyMarked(next, alreadyMarked);
					// System.out.println("Current: " + r2 + "," + g2 + "," +
					// b2);
				}
			}
		}
		long time = System.currentTimeMillis() - start;
		System.out.println("Time: " + (time / 1000l) + "s");
		return lastMarked;
	}

	private boolean isAlreadyMarked(int current, int[] alreadyMarked) {
		return (alreadyMarked[current / 32] & (1 << (31 - (current % 32)))) != 0;
	}

	private void setAlreadyMarked(int current, int[] alreadyMarked) {
		alreadyMarked[current / 32] = alreadyMarked[current / 32] | (1 << (31 - (current % 32)));
	}

	private int getRGBAsInt(int r, int g, int b) {
		return (r << 16) | (g << 8) | b;
	}

	private int getR(int rgb) {
		return rgb >> 16;
	}

	private int getG(int rgb) {
		return (rgb >> 8) & 0b11111111;
	}

	private int getB(int rgb) {
		return rgb & 0b11111111;
	}

	static boolean writeInFile(File f, String s) throws IOException {

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(f));
			writer.write(s);
			writer.close();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			if (writer != null)
				writer.close();
			return false;
		}

	}

	static void showEnc16AsImg() {
		byte[] data = new byte[8];
		for (int i = 0; i < 16; i++) {
			data[i / 2] = (byte) (data[i / 2] | (byte) (i << (((i + 1) % 2) * 4)));
		}
		for (int i = 0; i < 8; i++) {
			System.out.print("," + data[i]);
		}
		BufferedImage image = ImageCreator.createImage(4 * 32, 32, data, Encoding.getEnc4bit());
		FileIO.writeImageToPNG(image, new File("enc16"));
	}
	
	static void showEnc256AsImg() {
		byte[] data = new byte[256];
		for (int i = 0; i < 256; i++) {
			data[i] = (byte) i;
		}
		for (int i = 0; i < 256; i++) {
			System.out.print("," + data[i]);
		}
		BufferedImage image = ImageCreator.createImage(16 * 32, 32, data, Encoding.getEnc8bit());
		FileIO.writeImageToPNG(image, new File("enc256"));
	}

}
