package com.sephora.esb.common.rest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
 
@Provider
public class ESBRestExceotionMapper implements ExceptionMapper<ESBRestException> {

	public Response toResponse(ESBRestException ex) {
			System.out.println("Calling MdCSRestExceotionMapper.toResponse()");
		return ESBRestResponseBuilder.INSTANCE.buildResponse(ex.getStatus(), 
				                                              null,
				                                              null,
				                                              new ESBRestError(ex),
				                                              MediaType.APPLICATION_JSON_TYPE);
		
	}
	
}
