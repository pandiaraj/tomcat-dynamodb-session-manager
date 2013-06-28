package com.pandiaraj.catalina.session;

import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;

public class DynamoSession extends StandardSession {

	private static final long serialVersionUID = -8868825758658207373L;

	public DynamoSession(Manager manager) {
		super(manager);
	}
	
	@Override
	public void setAttribute(String key, Object value) {
		System.out.println("----------> setting attribute: "+key+" ---- value: "+value);
		super.setAttribute(key, value);
	}
}
