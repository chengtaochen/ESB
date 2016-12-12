package com.sephora.esb.common.rest;

public class ESBRestResponse {
	
	/** contains the same HTTP Status code returned by the server */
	private int status;
	private ESBRestError error;
	private Object data;
	private String message;
	
	public ESBRestResponse() {};
		
	public int getStatus() {
		return status;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
	
	public ESBRestError getError() {
		return error;
	}

	public void setError(ESBRestError error) {
		this.error = error;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	@Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ESBRestResponse [data=")
               .append(data)
               .append(", status=")
               .append(status)
               .append(", message=")
               .append(message)
               .append(", error=")
               .append(error)
               .append("]");
        return builder.toString();
    }

}
