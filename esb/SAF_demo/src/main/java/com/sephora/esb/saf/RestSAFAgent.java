package com.sephora.esb.saf;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sephora.esb.common.rest.ESBRestClient;
import com.sephora.esb.common.rest.ESBRestClient.RestResponse;

public class RestSAFAgent extends AbstractSAFAgent {

	private URL remoteContext;

	Logger logger = LoggerFactory.getLogger(RestSAFAgent.class);
	
	public RestSAFAgent(URL remoteContext) throws IOException {
		this.remoteContext = remoteContext;
	}

	void send(byte[] data) throws Exception {
		logger.debug("Calling RestSAFAgent.send()...");
		
		try {

			logger.debug("In RestSAFAgent.send(), sending data to remote destination...");
			// Make Rest call
			ESBRestClient eSBRestClient = new ESBRestClient(remoteContext.getProtocol(), remoteContext.getHost(),
					String.valueOf(remoteContext.getPort()), remoteContext.getPath());

			ClientData clientData = new ClientData();
			clientData.setData(new String(data));

			RestResponse restResponse = eSBRestClient.sendPostRequest(clientData, MediaType.APPLICATION_JSON_TYPE,
					MediaType.APPLICATION_JSON);
			int retStatus = restResponse.getStatus();
			logger.debug("Remote REST service returns status=" + retStatus);
			if (retStatus != 200) {
				logger.error("Status code is not 200");
				throw new Exception("Status code is not 200");
			}

		} catch (Exception e) {
			logger.error(
					"Exception occured while sending data to remote server, putting data into persist storage...", e);
			//e.printStackTrace();
			throw e;
		}
	}

    boolean check_connection() {
		HttpURLConnection urlConnection = null;
		System.setProperty("http.keepAlive", "false");
		StringBuffer urlStrB = new StringBuffer();
		urlStrB.append(this.remoteContext.getProtocol()).append("://").append(this.remoteContext.getHost()).append(":")
				.append(String.valueOf(this.remoteContext.getPort()));
		try {
			URL url = new URL(urlStrB.toString());
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("HEAD");
			urlConnection.getInputStream().close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}

		return true;
	}

	class ClientData {
		private String data;

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

	}

}
