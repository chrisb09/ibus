package de.christian.f.brinkmann.ibus;

public class Metric {

	static Metric active;

	public static void startMetric() {
		active = new Metric();
		active.start();
	}

	public static String stopMetric() {
		if (active == null)
			return "";
		active.end();
		String info = active.getInfo();
		active = null;
		return info;
	}

	private long start;
	private long end;
	private long size;
	private Long currentSize = 0l;
	private long loading;
	private long loadingT;
	private long copying;
	private long copyingT;
	private long aes;
	private long aesT;
	private long writing;
	private long writingT;

	public void startLoading() {
		this.loadingT = System.nanoTime();
	}

	public void endLoading() {
		this.loading += System.nanoTime() - this.loadingT;
	}

	public long getLoading() {
		return loading;
	}

	public void startCopying() {
		this.copyingT = System.nanoTime();
	}

	public void endCopying() {
		this.copying += System.nanoTime() - this.copyingT;
	}

	public long getCopying() {
		return copying;
	}

	public void startAES() {
		this.aesT = System.nanoTime();
	}

	public void endAES() {
		this.aes += System.nanoTime() - this.aesT;
	}

	public long getAES() {
		return aes;
	}

	public void startWriting() {
		this.writingT = System.nanoTime();
	}

	public void endWriting() {
		this.writing += System.nanoTime() - this.writingT;
	}

	public long getWriting() {
		return writing;
	}

	public void start() {
		this.start = System.currentTimeMillis();
	}

	public void end() {
		this.end = System.currentTimeMillis();
	}

	public long getTime() {
		return end - start;
	}

	public void addSize(long size) {
		this.size += size;
	}

	public long getSize() {
		return size;
	}

	public void addCurrentSize(long size) {
		synchronized (currentSize) {
			this.currentSize += size;
		}
	}

	public long getCurrentSize() {
		return currentSize;
	}

	public String getTempInfo() {
		//@formatter:off
		return "Progress: "+(getCurrentSize()*100/getSize())+"%"+
				"     "+(Tool.readableNanoTime(1000000l* getRemainingTime()))+" remaining"+
				"     "+Tool.readableFileSize(getCurrentSize())+" / "+Tool.readableFileSize(getSize())+
				"     "+(Encoder.mainEncodingRunnable.scanComplete() ? "scan complete" : "scanning...")+
				"     time passed: "+Tool.readableNanoTime(1000000l*(System.currentTimeMillis()-start))
				+"     threads: "+Encoder.mainEncodingRunnable.getUsedThreads()+"/"+Encoder.mainEncodingRunnable.getMaxThreads()
				+"     queue: "+Encoder.mainEncodingRunnable.getQueueSize();
		//@formatter:on
	}

	public String getInfo() {
		//@formatter:off
		return "Total: "+Tool.readableFileSize(size)+" in "+Tool.readableNanoTime(1000000l*getTime())+": "+(Tool.readableFileSize(size*1000l/getTime()))+"/s \n"+
				"Loading: "+Tool.readableNanoTime(getLoading())+" \n"+
				"Copying: "+Tool.readableNanoTime(getCopying())+" \n"+
				"AES: "+Tool.readableNanoTime(getAES())+" \n"+
				"Writing: "+Tool.readableNanoTime(getWriting())+" \n";
		//@formatter:on
	}

	public long getRemainingTime() {
		return (long) (((System.currentTimeMillis() - start) * (((getSize() * 1.0) / Math.max(1.0, getCurrentSize())) - 1.0)));
	}

}
