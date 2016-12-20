package com.sephora.esb.common.rest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
@Provider
public class ESBRestExceotionMapper implements ExceptionMapper<ESBRestException> {
	Logger logger = LoggerFactory.getLogger(ESBRestExceotionMapper.class);
	
	public Response toResponse(ESBRestException ex) {
		logger.debug("Calling MdCSRestExceotionMapper.toResponse()");
		return ESBRestResponseBuilder.INSTANCE.buildResponse(ex.getStatus(), 
				                                              null,
				                                              null,
				                                              new ESBRestError(ex),
				                                              MediaType.APPLICATION_JSON_TYPE);
		
	}
	
}
