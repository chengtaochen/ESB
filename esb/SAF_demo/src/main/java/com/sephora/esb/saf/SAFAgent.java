package com.sephora.esb.saf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

abstract class SAFAgent {
	//private static final int PERSIST_STORAGE_TASK_THREAD_COUNT = 1;
	private static final int AGENT_TASK_THREAD_COUNT = 1;
	private static final String PERSIST_STORAGE_PATH = "/tmp/SAF_data.txt";
	private long ttl = -1;
	private int max_retries = -1;
	private int max_queue_size = 10;
	private int retries_interval = 5000; // ms
	private ExecutorService executor;
	// private ScheduledExecutorService executor;
	private BufferedWriter bufferedWriter = null;
	private BufferedReader bufferReader = null;

	Queue<byte[]> persistQueue = new ConcurrentLinkedQueue<byte[]>();
	private File persistStorage = null;
	private final ReentrantLock persistStoragelock = new ReentrantLock();

	public SAFAgent() throws IOException {
		this.executor = Executors.newFixedThreadPool(1 + AGENT_TASK_THREAD_COUNT);
		// this.executor = Executors.newScheduledThreadPool(THREAD_COUNT);
		this.startSAFAgent();
		persistStorage = new File(PERSIST_STORAGE_PATH);
		// check if file exist, otherwise create the file before writing
		if (!this.persistStorage.exists()) {
			this.persistStorage.createNewFile();
		}
		Writer writer = new FileWriter(this.persistStorage);
		this.bufferedWriter = new BufferedWriter(writer);
		// this.bufferReader = new BufferedReader( new
		// FileReader(PERSIST_STORAGE_PATH));
	}

	abstract public void send(byte[] data) throws Exception;

	public void shutDown() {
		try {
			System.out.println("attempt to shutdown executor");
			this.executor.shutdown();
			this.executor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			System.err.println("tasks interrupted");
		} finally {
			if (!this.executor.isTerminated()) {
				System.err.println("cancel non-finished tasks");
			}
			this.executor.shutdownNow();
			System.out.println("shutdown finished");
		}
	}

	public long getTtl() {
		return ttl;
	}

	public void setTtl(long ttl) {
		this.ttl = ttl;
	}

	public int getMax_retries() {
		return max_retries;
	}

	public void setMax_retries(int max_retries) {
		this.max_retries = max_retries;
	}

	public int getMax_queue_size() {
		return max_queue_size;
	}

	public void setMax_queue_size(int max_queue_size) {
		this.max_queue_size = max_queue_size;
	}

	public int getRetries_interval() {
		return retries_interval;
	}

	public void setRetries_interval(int retries_interval) {
		this.retries_interval = retries_interval;
	}

	void persistData(byte[] data) {
		try {
			this.persistStoragelock.lock();
			this.bufferedWriter.write(new String(data));
			this.bufferedWriter.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (this.bufferedWriter != null) {
					this.bufferedWriter.flush();
				}
				this.persistStoragelock.unlock();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void startSAFAgent() {
		// Future<AgentTask> future = executor.submit(new AgentTask());
		for (int i = 0; i < 1; i++) {
			this.executor.submit(new PersistStorageTask(i));
			// this.executor.scheduleWithFixedDelay(new AgentTask(i), 0,
			// this.retries_interval, TimeUnit.MILLISECONDS);
		}
		for (int i = 0; i < AGENT_TASK_THREAD_COUNT; i++) {
			this.executor.submit(new AgentTask(i));
		}
	}

	class AgentTask implements Runnable {
		private int thread_num = -1;

		public AgentTask(int num) {
			this.thread_num = num;
			System.out.println("Start new AgentTask thread number:" + this.thread_num);
		}

		public void run() {
			Iterator<byte[]> it = null;
			byte[] data = null;
			while (true) {
				System.out.println("AgentTask " + this.thread_num + " checking persist queue");
				try {
					data = persistQueue.poll();
					if(data!=null) {
						send(data);
					}
				} catch (Exception e) {
					System.err.println("In AgentTask, exception occured while sending data...");
					e.printStackTrace();
					persistData(data);
				}

				try {
					Thread.sleep(retries_interval);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	class PersistStorageTask implements Runnable {
		private int thread_num = -1;

		public PersistStorageTask(int num) {
			this.thread_num = num;
			System.out.println("Start new PersistStorageTask thread number:" + this.thread_num);
		}

		public void run() {
			try {
				while (true) {
					Thread.sleep(5000);
					System.out.println("PersistStorageTask " + this.thread_num + " checking persist storage");
					// byte[] data = persistQueue.poll();
					byte[] data = null;
					int lineCount = 0;
					String strLine = "";
					if (bufferReader == null) {
						bufferReader = new BufferedReader(new FileReader(PERSIST_STORAGE_PATH));
					}
					persistStoragelock.lock();
					while ((strLine = bufferReader.readLine()) != null && lineCount <= max_queue_size) {
						System.out.println("PersistStorageTask read one line from persist storage:" + strLine);
						data = strLine.getBytes();
						if (data != null) {
							System.out.println("Got data from persist storage, sending it out now...");
							persistQueue.add(data);
						} else {
							System.out.println("No data available in persist storage...");
						}
						lineCount++;
					}
					if (lineCount!=0 && lineCount < max_queue_size) {
						System.out.println("No more data available in persist storage...");
						// EOF encounter in persist storage file
						// Truncate file and re-assign all file reference
						bufferReader.close();
						bufferReader = null;
						bufferedWriter.close();
						bufferedWriter = null;
						FileOutputStream f = new FileOutputStream(PERSIST_STORAGE_PATH, false);
						FileChannel outChan = f.getChannel();
						outChan.truncate(0);
						outChan.close();
						f.close();
						Writer writer = new FileWriter(persistStorage);
						bufferedWriter = new BufferedWriter(writer);
					}
					persistStoragelock.unlock();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
