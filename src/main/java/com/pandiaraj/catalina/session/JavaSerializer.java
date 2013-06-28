package com.pandiaraj.catalina.session;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.servlet.http.HttpSession;

import org.apache.catalina.util.CustomObjectInputStream;

public class JavaSerializer implements Serializer {
	private ClassLoader loader;

	public void setClassLoader(ClassLoader loader) {
		this.loader = loader;
	}

	public byte[] serializeFrom(HttpSession session) throws IOException {
		DynamoSession dynamoSession = (DynamoSession)session;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(baos));
		oos.writeLong(dynamoSession.getCreationTime());
		dynamoSession.writeObjectData(oos);
		oos.close();
		return baos.toByteArray();
	}
	
	public HttpSession deserializeInto(byte[] data, HttpSession session)
			throws IOException, ClassNotFoundException {
		DynamoSession dynamoSession = (DynamoSession)session;
		BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
		ObjectInputStream ois = new CustomObjectInputStream(bis, loader);
		dynamoSession.setCreationTime(ois.readLong());
		dynamoSession.readObjectData(ois);
		return session;
	}

}
