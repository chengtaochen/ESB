package com.sephora.esb.common.rest;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ESBRestClient {
   
   private String server_host;
   private String server_port;
   private String server_protocol;
   private String resource_path;
   Logger logger = LoggerFactory.getLogger(ESBRestClient.class);
   
   public ESBRestClient(String server_protocol, String server_host, String server_port, String resource_path) {
	   this.server_protocol = server_protocol;
	   this.server_host = server_host;
	   this.server_port = server_port;
	   this.resource_path = resource_path;
   }
   
   public <T> RestResponse sendPostRequest(T entity, MediaType mediaType, String acceptedResponseTypes) {
	   
	   Client client = ClientBuilder.newClient();
	   WebTarget target = client.target(this.server_protocol + "://" + this.server_host + ":" + this.server_port).path(this.resource_path);
	    
	   Response response = target.request(acceptedResponseTypes).post(Entity.entity(entity, mediaType));
	   
	   int status = response.getStatus();
	   String responseBody = response.readEntity(String.class);
	   logger.debug("In ESBRestClient.sendPostRequest(), getting status code=" + status);
	   logger.debug("In ESBRestClient.sendPostRequest(), getting response body: " + responseBody);
	   //need to call close if we do not read/process the response entity
	   response.close();
	   
	   return new RestResponse(status, responseBody);
	   
   }
   
   public class RestResponse {
	   private int status;
	   private String responseBody;
	   
	   public RestResponse(int status, String responseBody) {
		   this.status = status;
		   this.responseBody = responseBody;
	   }

	   public int getStatus() {
		   return status;
	   }

	   public void setStatus(int status) {
		   this.status = status;
	   }

	   public String getResponseBody() {
		   return responseBody;
	   }

	   public void setResponseBody(String responseBody) {
		   this.responseBody = responseBody;
	   }
	   
	   @Override
	   public String toString() {
		   StringBuffer sb = new StringBuffer();
	   
		   return  sb.append("RestResponse.status=" + this.status)
	    		     .append(", RestResponse.responseBody=" + this.responseBody)
	    		     .toString();
	   }
	   
   }
   
   /*
   public static class ClientData {
	   private String data;

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
	   
   }
   public static void main(String[] args) {
	   ESBRestClient eSBRestClient = new ESBRestClient("http", "localhost", "8080", "sample-rest-service-0.1.0/api/data");
	   
	   ClientData clientData = new ClientData();
	   clientData.setData("hello123");
	   
	   eSBRestClient.sendPostRequest(clientData,MediaType.APPLICATION_JSON_TYPE,  MediaType.APPLICATION_JSON);
   }
   */
}
