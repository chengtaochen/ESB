package com.sephora.esb.saf;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.core.MediaType;

import com.sephora.esb.common.rest.ESBRestClient;
import com.sephora.esb.common.rest.ESBRestClient.RestResponse;

public class RestSAFAgent extends SAFAgent {

	private URL remoteContext;
	
	public RestSAFAgent(URL remoteContext) throws IOException{
	    	this.remoteContext = remoteContext;
	}
	
	@Override
	public void send(byte[] data) throws Exception {
		System.out.println("Calling RestSAFAgent.send()...");
		// 1. Check remote server status
		// 2. Based on response from remote server, either send the data
		// or store data locally for re-sending at later time
		try {
			if (this.check_connection(this.remoteContext) == false) {
				// Store data locally
				System.err.println("Remote server is down, putting data into persist storage...");
				this.persistData(data);
			}
			else {
				System.out.println("Remote server is up, sending data to remote destination...");
				//Make Rest call
				ESBRestClient eSBRestClient = new ESBRestClient(remoteContext.getProtocol(), 
						                                        remoteContext.getHost(), 
						                                        String.valueOf(remoteContext.getPort()), 
						                                        remoteContext.getPath());
				   
			    ClientData clientData = new ClientData();
				clientData.setData(new String(data));
				   
				RestResponse restResponse = eSBRestClient.sendPostRequest(clientData,MediaType.APPLICATION_JSON_TYPE,  MediaType.APPLICATION_JSON);
				int retStatus = restResponse.getStatus();
				System.out.println("Remote REST service returns status=" + retStatus);
				if (retStatus != 200 ) {
					System.err.println("Status code is not 200");
					//this.persistData(data);
					throw new Exception("Status code is not 200");
				}
			}
		} catch (Exception e) {
			System.err.println("Exception occured while sending data to remote server, putting data into persist storage...");
			e.printStackTrace();
			//this.persistData(data);
			throw e;
		}
	}
	
	private boolean check_connection(URL remoteContext) {
		HttpURLConnection urlConnection = null;
		System.setProperty("http.keepAlive", "false");
		StringBuffer urlStrB = new StringBuffer();
		urlStrB.append(remoteContext.getProtocol())
		       .append("://")
		       .append(remoteContext.getHost())
		       .append(":")
		       .append(String.valueOf(remoteContext.getPort()));
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
