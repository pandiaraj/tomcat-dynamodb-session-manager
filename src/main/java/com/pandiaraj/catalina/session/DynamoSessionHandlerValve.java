package com.pandiaraj.catalina.session;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class DynamoSessionHandlerValve extends ValveBase {
	
	private static Log log = LogFactory.getLog(DynamoSessionHandlerValve.class);
	
	private DynamoSessionManager manager;
	
	public void setDynamoSessionManager(DynamoSessionManager manager) {
		this.manager = manager;
	}
	
	public void invoke(Request request, Response response) throws IOException, ServletException {
		try {
			getNext().invoke(request, response);
		}finally {
			final Session session = request.getSessionInternal(false);
			storeOrRemoveSession(session);
		}
	}
	
	private void storeOrRemoveSession(Session session) {
		try {
			if(session != null) {
				if(session.isValid()) {
					if(session.getSession() != null) {
						log.debug("Saving session: " + session.getId());
						manager.add(session);
					} else {
						log.debug("No HTTP session present, not saving session: " + session.getId());
					}
				} else {
					log.debug("Invalidating session: " + session.getId());
					manager.remove(session);
				}
			}
		}catch(Exception e) {
			log.error("Could not store or remove session");
		}
	}

}
