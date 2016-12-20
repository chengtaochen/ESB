package com.sephora.esb.saf;

import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SAFTest {

	public static void main(String[] args) {

		Logger logger = LoggerFactory.getLogger(SAFTest.class);
		SAFAgent safAgent = null;

		try {
			URL url = new URL("http://localhost:8080/sample-rest-service-0.1.0/api/data");
			safAgent = new RestSAFAgent(url);
			String data = "this is test message ";
			int count = 1;
			while (true) {
				safAgent.sendRequest((data+String.valueOf(count)).getBytes());
				count++;
				Thread.sleep(9000);
			}
		} catch (MalformedURLException e) {
			logger.error("MalformedURLException occred...", e);
			//e.printStackTrace();
		} catch (Exception e) {
			logger.error("Exception occred...", e);
			//e.printStackTrace();
		} finally {
			if (safAgent != null) {
				safAgent.shutDown();
			}
		}
	}

}
