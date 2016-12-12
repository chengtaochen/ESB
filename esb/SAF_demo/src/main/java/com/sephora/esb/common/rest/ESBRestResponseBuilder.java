package com.sephora.esb.common.rest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ESBRestResponseBuilder {
	
	INSTANCE();
	
	private static final Logger LOG = LoggerFactory.getLogger(ESBRestResponseBuilder.class);
		
	public Response buildResponse(int statusCode, Object data,String message, ESBRestError error, MediaType responseMediaType) {
        LOG.info("In ESBRestResponseBuilder.buildResponse(), building response for given statusCode:{}, data:{}",
                statusCode, data==null?"NULL":data);
       
        Response response = null;
        
        ResponseBuilder responseBuilder = Response.status(statusCode);
                
        ESBRestResponse eSBRestResponse = new ESBRestResponse();   
        eSBRestResponse.setStatus(statusCode);
        if(data!=null) {
           eSBRestResponse.setData(data);
        }
        if(message != null && !message.isEmpty()) {
        	eSBRestResponse.setMessage(message);
        }
        if(error!=null) {
        	eSBRestResponse.setError(error);
        }
        
        if (responseMediaType != null) {
            responseBuilder.type(responseMediaType);
        }
        
        responseBuilder.entity(eSBRestResponse);    
        response = responseBuilder.build();
        
        LOG.info("In ESBRestResponseBuilder.buildResponse(), response built for statusCode:{}, data:{}, response:{}", statusCode, data==null?"NULL":data, response);
        
        return response;
    }

}
