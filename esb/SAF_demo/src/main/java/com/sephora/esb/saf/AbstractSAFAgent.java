package com.sephora.esb.saf;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * persist binary record format
 * 1. processed_flag => type: boolean, size:1 byte
 * 2. uuid => type: String, size: 36 bytes
 * 3. retryCount => type: int, size: 4 bytes
 * 4. lastRetry => type: long, size: 8 bytes
 * 5. offset => type: int, size: 4 bytes
 * 6. data => type: byte[], size: variable length 
 * 
 */

abstract class AbstractSAFAgent implements SAFAgent {
	// private static final int PERSIST_STORAGE_TASK_THREAD_COUNT = 1;
	private static final int AGENT_TASK_THREAD_COUNT = 1;
	private static final String PERSIST_STORAGE_PATH = "/tmp/SAF.dat";
	private long ttl = 172800; // 48 hours in seconds
	//private long ttl = 10;
	private int max_retries = 1000;
	//private int max_retries = 2;
	private int max_queue_size = 10;
	private int retries_interval = 5000; // ms
	private ExecutorService executor;
	// private ScheduledExecutorService executor;
	private RandomAccessFile persistStorageWriter = null;
	private RandomAccessFile persistStorageReader = null;
	Queue<RequestEntry> persistQueue = new ConcurrentLinkedQueue<RequestEntry>();
	private final ReentrantLock persistStoragelock = new ReentrantLock();
 
	Logger logger = LoggerFactory.getLogger(AbstractSAFAgent.class);
	
	public AbstractSAFAgent() throws IOException {
		this.init();
	}

	public AbstractSAFAgent(long ttl, int max_retries) throws IOException {
		this.ttl = ttl;
		this.max_retries = max_retries;
		this.init();
	}

	private void init() throws IOException {
		this.executor = Executors.newFixedThreadPool(1 + AGENT_TASK_THREAD_COUNT);
		// this.executor = Executors.newScheduledThreadPool(THREAD_COUNT);
		this.startSAFAgent();
		this.persistStorageWriter = new RandomAccessFile(PERSIST_STORAGE_PATH, "rwd");
		//this.persistStorageReader = new RandomAccessFile(PERSIST_STORAGE_PATH, "r");
		this.persistStorageReader = new RandomAccessFile(PERSIST_STORAGE_PATH, "rwd");
	}

	public void shutDown() {
		try {
			logger.info("attempt to shutdown executor");
			this.executor.shutdown();
			this.executor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("tasks interrupted");
		} finally {
			if (!this.executor.isTerminated()) {
				logger.error("cancel non-finished tasks");
			}
			this.executor.shutdownNow();
			logger.info("shutdown finished");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sephora.esb.saf.SAFAgent#sendRequest(byte[])
	 */
	public void sendRequest(byte[] data) throws Exception {
		RequestEntry requestEntry = new RequestEntry(UUID.randomUUID().toString(), data, 0,
				Calendar.getInstance().getTimeInMillis(), false);
		if (this.check_connection() == false) {
			logger.error(
					"In AbstractSAFAgent.sendRequest(), remote server is down, putting data into persist storage...");
			this.persistData(requestEntry);
		} else {
			try {
				this.send(requestEntry.getData());
			} catch (Exception e) {
				logger.error(
						"In AbstractSAFAgent.sendRequest(), exception occured while sending request to remote destionation, putting data into persist storage...", e);
				//e.printStackTrace();
				this.persistData(requestEntry);
			}
		}
	}

	/*
	 * Abstract method that will be implemented by subclass for sending client
	 * request
	 */
	abstract void send(byte[] data) throws Exception;

	abstract boolean check_connection();

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

	void persistData(RequestEntry requestEntry) {
		try {
			logger.debug("In AbstractSAFAgent.persistData(), uuid=" + requestEntry.getUuid());
			this.persistStoragelock.lock();
			this.persistStorageWriter.writeBoolean(requestEntry.isProcessed());
			this.persistStorageWriter.writeBytes(requestEntry.getUuid());
			this.persistStorageWriter.writeInt(requestEntry.getRetryCount());
			this.persistStorageWriter.writeLong(requestEntry.getCreatedTime());
			this.persistStorageWriter.writeInt(requestEntry.getData().length);
			this.persistStorageWriter.write(requestEntry.getData());
		} catch (IOException e) {
			logger.error("IOException occured...", e);
			//e.printStackTrace();
		} catch (Exception exp) {
			logger.error("Exception occured...", exp);
			//exp.printStackTrace();
		} finally {
			this.persistStoragelock.unlock();
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

	private boolean canRetry(RequestEntry requestEntry) {
		boolean ret = true;
		long currentTime = Calendar.getInstance().getTimeInMillis();
		int rCount = requestEntry.getRetryCount();
		long createdTime = requestEntry.getCreatedTime();

		if (rCount > this.max_retries) {
			ret = false;
			logger.info("In canRetry(), data with uuid=" + requestEntry.getUuid() + " and current retryCount="
					+ rCount + " has reached max retry: " + this.max_retries);
		} else if ((currentTime - createdTime) / 1000 > this.ttl) {
			ret = false;
			logger.info("In canRetry(), data with uuid=" + requestEntry.getUuid() + " and created time="
					+ createdTime + " has exceeded TTL (sec): " + this.ttl);
		} else {
			requestEntry.setRetryCount(++rCount);
		}

		return ret;
	}

	class AgentTask implements Runnable {
		private int thread_num = -1;

		public AgentTask(int num) {
			this.thread_num = num;
			logger.info("Start new AgentTask thread number:" + this.thread_num);
		}

		public void run() {
			RequestEntry requestEntry = null;
			while (true) {
				logger.debug("AgentTask " + this.thread_num + " checking persist queue");
				try {
					requestEntry = persistQueue.poll();
					if (requestEntry != null) {
						logger.debug("AgentTask got a client request entry={}" + requestEntry);
						// check ttl and max_retry
						if (canRetry(requestEntry) == true) {
							logger.debug("In AgentTask.run(), after calling canRetry(), requestEntry={}"+requestEntry);
							send(requestEntry.getData());
						} else {
							logger.debug("client data with uuid=" + requestEntry.getUuid() + " either reached ttl or max retry.");
						}
					}
				} catch (Exception e) {
					logger.error("In AgentTask, exception occured while sending data...", e);
					persistData(requestEntry);
				}

				try {
					Thread.sleep(retries_interval);
				} catch (InterruptedException e) {
					logger.error("InterruptedException occrued...", e);
				}
			}
		}

	}

	class PersistStorageTask implements Runnable {
		private int thread_num = -1;

		public PersistStorageTask(int num) {
			this.thread_num = num;
			logger.info("Start new PersistStorageTask thread number:" + this.thread_num);
		}

		public void run() {
			try {
				while (true) {
					Thread.sleep(5000);
					logger.debug("PersistStorageTask " + this.thread_num + " checking persist storage");
					int recordCount = 0;
					boolean isEOF = false;
					
					persistStoragelock.lock();
					while (true && recordCount <= max_queue_size) {
						try {
							logger.debug("PersistStorageTask reading data from persist storage file...");
							//Save current offset so we
							//can update the processFlag
							long currentRecordStartOffset = persistStorageReader.getFilePointer();
							boolean processFlag = persistStorageReader.readBoolean();
							logger.debug("processFlag=" + String.valueOf(processFlag));
							if(processFlag==true) {
								continue;
							}
							byte[] uuidVal = new byte[36];
							persistStorageReader.readFully(uuidVal);
							logger.debug("uuidVal=" + new String(uuidVal));
							int retryCount = persistStorageReader.readInt();
							logger.debug("retryCount=" + String.valueOf(retryCount));
							long lastRetry = persistStorageReader.readLong();
							logger.debug("lastRetry=" + String.valueOf(lastRetry));
							int offset = persistStorageReader.readInt();
							logger.debug("offset=" + String.valueOf(offset));
							byte[] data = new byte[offset];
							persistStorageReader.readFully(data);
							logger.debug("data=" + new String(data));
							//Save next record starting offset
							//so we can reset the persistStorageReader pointer
							//to the correct start position of next record after setting current record
							//processFlag
							long nextRecordOffset = persistStorageReader.getFilePointer();
							
							RequestEntry requestEntry = new RequestEntry(new String(uuidVal), data, retryCount,
									lastRetry, processFlag);
							logger.debug(
									"PersistStorageTask read one requestEntry from persist storage:{}" + requestEntry);
							logger.debug("Got data from persist storage, push it to persist queue now...");
							persistQueue.add(requestEntry);
							recordCount++;
							//Update processFlag to true
							persistStorageReader.seek(currentRecordStartOffset);
							persistStorageReader.writeBoolean(true);
							//Reset the persistStorageReader pointer
							//to the correct start position of next record
							persistStorageReader.seek(nextRecordOffset);
							
						} catch (EOFException eof) {
							logger.error("Reached end of persist storage file!!!");
							isEOF = true;
							break;
						} catch (Exception e) {
							logger.error("Exception occured...", e);
						}
					}

					if (recordCount != 0 && isEOF == true) {
						logger.debug("No more data available in persist storage...");
						// EOF encounter in persist storage file
						// Truncate file and re-assign all file reference
						persistStorageReader.close();
						persistStorageReader = null;
						FileChannel outChan = persistStorageWriter.getChannel();
						outChan.truncate(0);
						outChan.close();
						persistStorageWriter.close();
						persistStorageWriter = null;
						persistStorageWriter = new RandomAccessFile(PERSIST_STORAGE_PATH, "rwd");
						//persistStorageReader = new RandomAccessFile(PERSIST_STORAGE_PATH, "r");
						persistStorageReader = new RandomAccessFile(PERSIST_STORAGE_PATH, "rwd");
					}

					persistStoragelock.unlock();
				}
			} catch (Exception e) {
				logger.error("Exception occured...", e);
				//e.printStackTrace();
			}
		}
	}

	class RequestEntry {
		private String uuid;
		private byte[] data;
		private int retryCount;
		private long createdTime;
		boolean processed;

		public RequestEntry(String uuid, byte[] data, int retryCount, long createdTime, boolean processed) {

			this.uuid = uuid;
			this.data = data;
			this.retryCount = retryCount;
			this.createdTime = createdTime;
			this.processed = processed;
		}

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public byte[] getData() {
			return data;
		}

		public void setData(byte[] data) {
			this.data = data;
		}

		public int getRetryCount() {
			return retryCount;
		}

		public void setRetryCount(int retryCount) {
			this.retryCount = retryCount;
		}

		public long getCreatedTime() {
			return createdTime;
		}

		public void setCreatedTime(long createdTime) {
			this.createdTime = createdTime;
		}

		public boolean isProcessed() {
			return processed;
		}

		public void setProcessed(boolean processed) {
			this.processed = processed;
		}

		public String toString() {
			StringBuffer strBuffer = new StringBuffer();
			strBuffer.append("UUID=>").append(this.uuid).append(", DATA=>").append(new String(this.data))
					.append(", RETRY_COUNT=>").append(String.valueOf(this.retryCount)).append(", CREATED_TIME=>")
					.append(String.valueOf(this.createdTime)).append(", PROCESSED_FLAG=>")
					.append(String.valueOf(this.processed));

			return strBuffer.toString();
		}

	}

}
