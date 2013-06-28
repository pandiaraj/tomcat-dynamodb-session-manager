package com.pandiaraj.catalina.session;

import java.io.IOException;
import java.util.Arrays;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Loader;
import org.apache.catalina.Session;
import org.apache.catalina.Valve;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class DynamoSessionManager extends ManagerBase implements Lifecycle {
	
	private static Log log = LogFactory.getLog(DynamoSessionManager.class);
	
	protected byte[] NULL_SESSION = "null".getBytes();
	protected LifecycleSupport lifecycle = new LifecycleSupport(this);
	protected DynamoSessionHandlerValve handlerValve;
	protected String serializationStrategyClass = "com.pandiaraj.catalina.session.JavaSerializer";
	protected Serializer serializer;
	private String accessKey;
	private String secretKey;
	private String tableName;
	private String hashKey;
	private String region;
	protected DynamoDBClient dynamoDBClient;
	
	protected static String name = "DynamoSessionManager";
	
	public String getAccessKey() {
		return accessKey;
	}
	
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}
	
	public String getSecretKey() {
		return secretKey;
	}
	
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	public String getHashKey() {
		return hashKey;
	}
	
	public void setHashKey(String hashKey) {
		this.hashKey = hashKey;
	}
	
	public String getRegion() {
		return region;
	}
	
	public void setRegion(String region) {
		this.region = region;
	}
	
	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		lifecycle.addLifecycleListener(listener);
	}
	
	@Override
	public LifecycleListener[] findLifecycleListeners() {
		return lifecycle.findLifecycleListeners();
	}
	
	public void removeLifecycleListener(LifecycleListener listener) {
		lifecycle.removeLifecycleListener(listener);
	}
	
	public void load() throws ClassNotFoundException, IOException {}
	
	public void unload() throws IOException {}
	
	@Override
	protected synchronized void startInternal() throws LifecycleException {
		super.startInternal();
		setState(LifecycleState.STARTING);
		Boolean attachedToValve = false;
		for(Valve valve : getContainer().getPipeline().getValves()) {
			if(valve instanceof DynamoSessionHandlerValve) {
				handlerValve = (DynamoSessionHandlerValve)valve;
				handlerValve.setDynamoSessionManager(this);
				attachedToValve = true;
				break;
			}
		}
		
		if(!attachedToValve) {
			String error = "Unable to attach valve to session handler";
			throw new LifecycleException(error);
		}
		
		try {
			initializeSerializer();
		}catch(ClassNotFoundException e) {
			throw new LifecycleException(e);
		}catch(IllegalAccessException e) {
			throw new LifecycleException(e);
		}catch(InstantiationException e) {
			throw new LifecycleException(e);
		}
		
		initializeDynamoDBClient();
		
		setDistributable(true);
	}
	
	@Override
	protected synchronized void stopInternal() throws LifecycleException {
		setState(LifecycleState.STOPPING);
		if(dynamoDBClient != null) {
			dynamoDBClient.shutdown();
		}
		super.stopInternal();
	}
	
	@Override
	public Session createSession(String sessionId) {
		DynamoSession session = (DynamoSession)createEmptySession();
		session.setNew(true);
		session.setValid(true);
		session.setCreationTime(System.currentTimeMillis());
		session.setMaxInactiveInterval(getMaxInactiveInterval());
		String jvmRoute = getJvmRoute();
		
		if(sessionId == null) {
			sessionId = generateSessionId();
		}
		
		if(jvmRoute != null) {
			sessionId += "." + jvmRoute;
		}
		
		DynamoSessionItem dynamoSessionItem = new DynamoSessionItem(sessionId, NULL_SESSION, session.getCreationTime());
		dynamoDBClient.put(dynamoSessionItem);
		
		session.setId(sessionId);
		session.tellNew();
		
		return session;
	}
	
	@Override
	public Session createEmptySession() {
		return new DynamoSession(this);
	}
	
	@Override
	public void add(Session session) {
		DynamoSession dynamoSession = (DynamoSession)session;
		String sessionId = dynamoSession.getId();

		try {
			byte[] value = serializer.serializeFrom(dynamoSession);
			long expiration = -1;

			if(dynamoSession.getMaxInactiveInterval() > 0) {
				expiration = System.currentTimeMillis() + (dynamoSession.getMaxInactiveInterval() * 1000);
			}

			DynamoSessionItem sessionItem = new DynamoSessionItem(sessionId, value, expiration);
			dynamoDBClient.put(sessionItem);
			
		}catch(IOException e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Session findSession(String sessionId) throws IOException {
		DynamoSession session = null;
		
		if(sessionId != null) {
			DynamoSessionItem sessionItem = dynamoDBClient.get(sessionId);
			
			if(sessionItem != null) {
				byte[] value = sessionItem.getValue();
				
				if(Arrays.equals(NULL_SESSION, value)) {
					throw new IllegalStateException("Attempt to load session [" + sessionId + "] which has been created but not yet serialized");
				} else {
					session = (DynamoSession)createEmptySession();
					try {
						serializer.deserializeInto(value, session);
						session.setId(sessionId);
						session.setNew(false);
						session.setMaxInactiveInterval(getMaxInactiveInterval());
						session.access();
						session.setValid(true);
					}catch(ClassNotFoundException e) {
						log.error("Unable to deserialize into session", e);
						throw new IOException("Unable to deserialize into session", e);
					}
				}
			}
		}
		return session;
	}
	
	@Override
	public void remove(Session session) {
		remove(session, false);
	}
	
	@Override
	public void remove(Session session, boolean update) {
		dynamoDBClient.remove(session.getId());
	}
	
	@Override
	public void processExpires() {
		dynamoDBClient.deleteExpiredItems(System.currentTimeMillis());
	}
	
	private void initializeSerializer() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		serializer = (Serializer)Class.forName(serializationStrategyClass).newInstance();
		Loader loader = null;
		if(container != null) {
			loader = container.getLoader();
		}
		
		ClassLoader classLoader = null;
		if(loader != null) {
			classLoader = loader.getClassLoader();
		}

		serializer.setClassLoader(classLoader);
	}
	
	protected void initializeDynamoDBClient() {
		dynamoDBClient = new DynamoDBClient(accessKey, secretKey, region, tableName, hashKey);
	}
}
