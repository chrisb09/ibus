package de.christian.f.brinkmann.ibus;

import java.util.ArrayList;

public class EncodingThread extends Thread {

	public boolean running = false;

	EncoderQueueEntry entry;
	ArrayList<EncodingThread> threads;
	ArrayList<EncodingThread> freeThreads;

	public EncodingThread() {

	}

	public void set(EncoderQueueEntry encoderQueueEntry, ArrayList<EncodingThread> threads, ArrayList<EncodingThread> freeThreads) {
		this.entry = encoderQueueEntry;
		this.threads = threads;
		this.freeThreads = freeThreads;
		synchronized (this) {
			this.notify();
		}
	}

	String getInfo() {
		return entry.getSource().getName() + "(" + Tool.readableFileSize(entry.getSource().length()) + ")";
	}

	@Override
	public void run() {

		running = true;

		while (true) {

			Encoder.pEncodeFile(entry.getIndf(), entry.getParent(), entry.getSource(), entry.getTargetDir(), entry.getCollisions());
			// Metric.active.addCurrentSize(entry.getSource().length());

			synchronized (threads) {
				threads.remove(this);
			}
			synchronized (freeThreads) {
				freeThreads.add(this);
			}
			synchronized (Encoder.mainEncodingRunnable) {
				Encoder.mainEncodingRunnable.notify();
			}

			synchronized (this) {
				try {
					running = false;
					this.wait();
					running = true;
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}

		}

	}

}
