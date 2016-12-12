package com.sephora.esb.common.rest;

public class ESBRestException extends Exception {
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 3669756802344598799L;

	/** 
	 * HTTP status code
	 */
	Integer status;
	
	/** application specific error code */
	int code; 
	
	String message;
			
	/**
	 * 
	 * @param status
	 * @param code
	 * @param message
	 */
	public ESBRestException(int status, int code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

}
