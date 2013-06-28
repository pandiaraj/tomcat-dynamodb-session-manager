package com.pandiaraj.catalina.session;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;

public class DynamoDBClient {
	
	private static Log log = LogFactory.getLog(DynamoDBClient.class);
	
	private AmazonDynamoDBClient dynamoDBClient;
	private AmazonDynamoDBAsyncClient dynamoDBAsyncClient;
	private String tableName;
	private String hashKey;
	
	public DynamoDBClient(String accessKey, String secretKey, String region, String tableName, String hashKey) {
		BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
		dynamoDBClient = new AmazonDynamoDBClient(awsCredentials);
		dynamoDBAsyncClient = new AmazonDynamoDBAsyncClient(awsCredentials);
		
		Region regionValue = Region.getRegion(Regions.US_EAST_1);
		if(region.equals("us-east-1")) {
			regionValue = Region.getRegion(Regions.US_EAST_1);
		} else if (region.equals("us-west-1")) {
			regionValue = Region.getRegion(Regions.US_WEST_1);
		} else if (region.equals("us-west-2")) {
			regionValue = Region.getRegion(Regions.US_WEST_2);
		} else if (region.equals("eu-west-1")) {
			regionValue = Region.getRegion(Regions.EU_WEST_1);
		} else if (region.equals("ap-southeast-1")) {
			regionValue = Region.getRegion(Regions.AP_SOUTHEAST_1);
		} else if (region.equals("ap-southeast-2")) {
			regionValue = Region.getRegion(Regions.AP_SOUTHEAST_2);
		} else if (region.equals("ap-northeast-1")) {
			regionValue = Region.getRegion(Regions.AP_NORTHEAST_1);
		} else if (region.equals("sa-east-1")) {
			regionValue = Region.getRegion(Regions.SA_EAST_1);
		}

		dynamoDBClient.setRegion(regionValue);
		this.tableName = tableName;
		this.hashKey = hashKey;
	}
	
	public void put(DynamoSessionItem sessionItem) {
		Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
		item.put(hashKey, new AttributeValue(sessionItem.getSessionId()));
		item.put("value", new AttributeValue().withB(ByteBuffer.wrap(sessionItem.getValue())));
		item.put("expiration", new AttributeValue().withN(String.valueOf(sessionItem.getExpiration())));
		
		PutItemRequest putRequest = new PutItemRequest(tableName, item);
		PutItemResult putResult = dynamoDBClient.putItem(putRequest);
		
		log.debug(putResult.toString());
	}
	
	public DynamoSessionItem get(String sessionId) {
		DynamoSessionItem sessionItem = null;
		
		HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
		
		Condition condition = new Condition();
		condition.withComparisonOperator(ComparisonOperator.EQ.toString());
		condition.withAttributeValueList(new AttributeValue(sessionId));
		
		scanFilter.put(hashKey, condition);
		
		ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
		ScanResult scanResult = dynamoDBClient.scan(scanRequest);
		
		List<Map<String, AttributeValue>> items = scanResult.getItems();
		if(items.size() > 0) {
			sessionItem = new DynamoSessionItem(sessionId);
			Map<String, AttributeValue> item = items.get(0);

			ByteBuffer buffer = item.get("value").getB();
			sessionItem.setValue(buffer.array());
			
			long expiration = Long.valueOf(item.get("expiration").getN());
			sessionItem.setExpiration(expiration);
		}
		
		return sessionItem;
	}
	
	public DynamoSessionItem update(DynamoSessionItem sessionItem) {
		Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
		key.put(hashKey, new AttributeValue(sessionItem.getSessionId()));
		
		Map<String, AttributeValueUpdate> updateItems = new HashMap<String, AttributeValueUpdate>();
		byte[] value = sessionItem.getValue();
		updateItems.put("value", new AttributeValueUpdate().withValue(new AttributeValue().withB(ByteBuffer.wrap(value))));
		
		updateItems.put("expiration", new AttributeValueUpdate().withValue(new AttributeValue().withN(String.valueOf(sessionItem.getExpiration()))));
		
		UpdateItemRequest updateRequest = new UpdateItemRequest(tableName, key, updateItems).withReturnValues(ReturnValue.ALL_NEW);
		UpdateItemResult updateResult = dynamoDBClient.updateItem(updateRequest);

		Map<String, AttributeValue> updatedMap = updateResult.getAttributes();
		sessionItem.setValue(updatedMap.get("value").getB().array());
		sessionItem.setExpiration(Long.valueOf(updatedMap.get("expiration").getN()));

		return sessionItem;
	}
	
	public DynamoSessionItem updateAsync(DynamoSessionItem sessionItem) {
		Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
		key.put(hashKey, new AttributeValue(sessionItem.getSessionId()));
		
		byte[] value = sessionItem.getValue();
		
		Map<String, AttributeValueUpdate> updateItems = new HashMap<String, AttributeValueUpdate>();
		updateItems.put("value", new AttributeValueUpdate().withValue(new AttributeValue().withB(ByteBuffer.wrap(value))));
		updateItems.put("expiration", new AttributeValueUpdate().withValue(new AttributeValue().withN(String.valueOf(sessionItem.getExpiration()))));
		
		UpdateItemRequest updateRequest = new UpdateItemRequest(tableName, key, updateItems).withReturnValues(ReturnValue.ALL_NEW);
		Future<UpdateItemResult> futureResult = dynamoDBAsyncClient.updateItemAsync(updateRequest);

		if(futureResult.isDone()) {
			try {
				UpdateItemResult updateResult = futureResult.get();
				Map<String, AttributeValue> updatedMap = updateResult.getAttributes();
				sessionItem.setValue(updatedMap.get("value").getB().array());
				sessionItem.setExpiration(Long.valueOf(updatedMap.get("expiration").getN()));
			}catch(ExecutionException e) {
				// nothing
			}catch(InterruptedException e) {
				// nothing
			}
		}

		return sessionItem;
	}

	public void remove(String sessionId) {
		Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
		key.put(hashKey, new AttributeValue(sessionId));
		
		DeleteItemRequest deleteRequest = new DeleteItemRequest(tableName, key);
		DeleteItemResult deleteResult = dynamoDBClient.deleteItem(deleteRequest);

		log.debug(deleteResult.toString());
	}
	
	public void removeAsync(String sessionId) {
		Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
		key.put(hashKey, new AttributeValue(sessionId));
		
		DeleteItemRequest deleteRequest = new DeleteItemRequest(tableName, key);
		Future<DeleteItemResult> futureResult = dynamoDBAsyncClient.deleteItemAsync(deleteRequest);

		if(futureResult.isDone()) {
			try {
				DeleteItemResult deleteResult = futureResult.get();
				if(log.isDebugEnabled()) {
					log.debug(deleteResult.toString());
				}
			}catch(ExecutionException e) {
				// nothing
			}catch(InterruptedException e) {
				// nothing
			}
		}
	}
	
	public void deleteExpiredItems(long expiration) {
		HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
		
		Condition condition = new Condition();
		condition.withComparisonOperator(ComparisonOperator.LT.toString());
		condition.withAttributeValueList(new AttributeValue().withN(String.valueOf(expiration)));
		
		scanFilter.put("expiration", condition);
		
		ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
		ScanResult scanResult = dynamoDBClient.scan(scanRequest);
		
		List<Map<String, AttributeValue>> items = scanResult.getItems();
		for(Map<String, AttributeValue> item : items) {
			String sessionId = item.get(hashKey).getS();
			remove(sessionId);
		}
	}
	
	public void shutdown() {
		if(dynamoDBClient != null) {
			dynamoDBClient.shutdown();
		}
		if(dynamoDBAsyncClient != null) {
			dynamoDBAsyncClient.shutdown();
		}
	}
}
