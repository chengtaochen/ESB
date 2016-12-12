package com.sephora.esb.saf;

import java.net.MalformedURLException;
import java.net.URL;

public class SAFTest {

	public static void main(String[] args) {

		SAFAgent safAgent = null;

		try {
			URL url = new URL("http://localhost:8080/sample-rest-service-0.1.0/api/data");
			safAgent = new RestSAFAgent(url);
			String data = "this is test message ";
			int count = 1;
			while (true) {
				safAgent.send((data+String.valueOf(count)).getBytes());
				count++;
				Thread.sleep(9000);
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (safAgent != null) {
				safAgent.shutDown();
			}
		}
	}

}
