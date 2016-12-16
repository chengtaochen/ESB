package com.sephora.esb.saf;

public interface SAFAgent {
	
	/*
	 * Public method that will be called by
	 * consuming clients to send request to remote
	 * destination through SAF agent
	 */
	void sendRequest(byte[] data) throws Exception;
	
	/*
	 * Public method for cleaning up resource during application
	 * shutdown process
	 */
	void shutDown();
}
