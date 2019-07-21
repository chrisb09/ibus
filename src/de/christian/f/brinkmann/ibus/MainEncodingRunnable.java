package de.christian.f.brinkmann.ibus;

import java.util.ArrayList;

public class MainEncodingRunnable implements Runnable {

	ArrayList<EncodingThread> threads = new ArrayList<EncodingThread>();
	ArrayList<EncodingThread> freeThreads = new ArrayList<EncodingThread>();

	ArrayList<EncoderQueueEntry> queue = new ArrayList<EncoderQueueEntry>();

	Boolean done = false;
	Boolean allQueued = false;
	int targetThreadAmount = 8;

	public MainEncodingRunnable() {
	}

	@Override
	public void run() {

		while (true) {

			try {
				synchronized (this) {
					this.wait();
				}
				synchronized (queue) {
					synchronized (threads) {
						// System.out.println("q: "+queue.size()+"   t:"+threads.size()+"   a:"+allQueued);
						synchronized (done) {
							done = allQueued && queue.isEmpty() && threads.isEmpty();
						}
						if (done) {
							System.out.println("DONE");
							threads.clear();
							freeThreads.clear();
							queue.clear();
							break;
						}
						for (EncodingThread et : threads) {
							synchronized (et) {
								if (et.running == false) {
									et.notify();
								}
							}
						}
						synchronized (freeThreads) {
							while (queue.size() > 0 && threads.size() < targetThreadAmount) {
								EncodingThread et;
								if (freeThreads.size() == 0) {
									et = new EncodingThread();

									EncoderQueueEntry q = queue.get(0);
									queue.remove(0);
									threads.add(et);
									et.set(q, threads, freeThreads);
									et.start();
								} else {
									et = freeThreads.get(0);
									freeThreads.remove(0);

									EncoderQueueEntry q = queue.get(0);
									queue.remove(0);
									threads.add(et);
									et.set(q, threads, freeThreads);
								}
							}
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.out.println("Leaving mainEncodingThread");
				break;
			}
		}

	}

	public void addQueue(EncoderQueueEntry encoderQueueEntry) {
		synchronized (queue) {
			queue.add(encoderQueueEntry);
		}
	}

	public int getUsedThreads() {
		synchronized (threads) {
			return threads.size();
		}
	}

	public int getMaxThreads() {
		return targetThreadAmount;
	}

	public int getQueueSize() {
		synchronized (queue) {
			return queue.size();
		}
	}

	public boolean scanComplete() {
		return allQueued;
	}
}
