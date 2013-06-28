package com.pandiaraj.catalina.session;


public class DynamoSessionItem {
	
	private String sessionId;
	private byte[] value;
	private long expiration;
	
	public DynamoSessionItem(String sessionId) {
		this(sessionId, "null".getBytes(), System.currentTimeMillis());
	}
	
	public DynamoSessionItem(String sessionId, byte[] value) {
		this(sessionId, value, System.currentTimeMillis());
	}

	public DynamoSessionItem(String sessionId, long expiration) {
		this(sessionId, "null".getBytes(), expiration);
	}

	public DynamoSessionItem(String sessionId, byte[] value, long expiration) {
		this.sessionId = sessionId;
		this.value = value;
		this.expiration = expiration;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	public byte[] getValue() {
		return value;
	}
	public void setValue(byte[] value) {
		this.value = value;
	}
	public long getExpiration() {
		return expiration;
	}
	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}
}
