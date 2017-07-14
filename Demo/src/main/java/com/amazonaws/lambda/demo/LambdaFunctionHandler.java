package com.amazonaws.lambda.demo;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

public class LambdaFunctionHandler implements RequestHandler<Map<String,Object>, Map<String,Object>> {

	private static final String 	EDGE_DEVICE_CLIENT_ID = "AlexaRecognizer";
	private static final String 	TOPIC = "demo/command";
	private static final String		RESPONSE_TYPE_CAMERA = "camera";
	private static final String		RESPONSE_REPLY_IMAGE = "image";
	
    @Override
    public Map<String,Object> handleRequest(Map<String,Object> input, Context context) {
        context.getLogger().log("Input: " + input);

        Map<String, Object> output = null;
        try {
            Map<String,Object> requestMap = getMap(input, "request");
            Object type = (requestMap == null) ? null : requestMap.get("type");
            if(type == null) {
                output = buildEndResponseBody("I don't know how to handle that.");
            }
            else if(type.equals("LaunchRequest")) {
            	Future<JSONObject> edgeResponseFuture = null;
            	try {
	            	edgeResponseFuture = sendImageCaptureRequest(context);
	            	JSONObject edgeResponse = edgeResponseFuture.get(3000, TimeUnit.MILLISECONDS);
            		edgeResponseFuture = null;
	            	JSONObject results = edgeResponse.getJSONObject("results");
	            	context.getLogger().log("EdgeDevice response results: " + results);
	            	String tag = results.optString("tag");
	            	if((tag == null) || tag.isEmpty()) {
	            		HashMap<String, Object> sessionState = new HashMap<>();
	            		String imageId = results.getString("imageId");
	            		sessionState.put("imageId", imageId);
	            		context.getLogger().log("EdgeDevice capture image with id: " + imageId);
	                    output = buildResponseBody("Hello.  I don't recognize you yet. What is your name?", 
	                    		false /* do not end session */, sessionState);
	            	}
	            	else {
	                	output = buildEndResponseBody("Hello " + tag);
	            	}
            	}
            	finally {
            		if((edgeResponseFuture != null) && !edgeResponseFuture.isDone()) {
    	            	context.getLogger().log("Cancelling EdgeDevice Response");
            			edgeResponseFuture.cancel(true /* may interrupt running */);
            		}
            	}
            }
            else if(type.equals("IntentRequest")) {
            	Map<String,Object> nameMap = getMap(requestMap, new String[] { "intent", "slots", "name" });
            	Object value = (nameMap == null) ? null : nameMap.get("value");
            	if(value != null) {
            		String tag = value.toString();
                	Map<String,Object> sessionAttributes = getMap(input, new String[] { "session", "attributes" });
                	Object imageId = (sessionAttributes == null) ? null : sessionAttributes.get("imageId");
                	context.getLogger().log("Tagging {" + imageId + "} as " + tag);
                	output = buildEndResponseBody("Hello " + tag + ".  I will recognize you next time.");
                	if(imageId != null) {
                		sendImageTagRequest(imageId.toString(), tag, context);
                	}
            	}
            }   
            else if(type.equals("SessionEndedRequest")) {
               	Map<String,Object> sessionAttributes = getMap(input, new String[] { "session", "attributes" });
            	Object imageId = (sessionAttributes == null) ? null : sessionAttributes.get("imageId");
            	context.getLogger().log("Cleaning up image {" + imageId + "}");
            	output = buildEndResponseBody("Please try again.  Good-bye.");
            	if(imageId != null) {
            		sendImageRemoveRequest(imageId.toString(), context);
            	}
            }           
        }
        catch (TimeoutException e) {
        	context.getLogger().log("Edge Device timed out: " + e.getMessage());
            output = buildEndResponseBody("The edge device did not respond in time.");
        }
        catch (Exception e) {
        	context.getLogger().log("EXCEPTION: " + e.getMessage());
            output = buildEndResponseBody("Something went wrong.");
        }
 
        if(output == null) {
        	output = buildResponse("This is not expected. Please try again.", true /* end session */);
        }
		context.getLogger().log("Output Map: " + output);

        return output;
    }
    
    static Map<String,Object> getMap(Map<String,Object> map, String[] keys) {
		int count = (keys == null) ? 0: keys.length;
		for(int i = 0;i < count;i++) {
			if(map == null) {
				break;
			}
			map = getMap(map, keys[i]);
		}

        return map;
    }

    static Map<String,Object> getMap(Map<String,Object> map, String key) {
    	Object value = map.get(key);
    	if (value instanceof Map) {
    		return (Map<String,Object>) value;
    	} else {
    		return new HashMap<>();
    	}
    }
    
    private Map<String, Object> buildEndResponseBody(String text) {
        return buildResponseBody(text, true /* end Session */, null /* no session state */);
    }
    
    private Map<String, Object> buildResponseBody(String text, boolean endSession, Map<String, Object> sessionAttributes) {
    	Map<String, Object> responseBody = new HashMap<String, Object>();
        responseBody.put("version", "1.0");
        if(!endSession && (sessionAttributes != null)) {
        	responseBody.put("sessionAttributes", sessionAttributes);
        }
        responseBody.put("response", buildResponse(text, endSession));

        return responseBody;
    }

    private Map<String, Object> buildResponse(String text, boolean endSession) {
    	Map<String, Object> response = new HashMap<String, Object>();
        response.put("outputSpeech", buildOutputSpeech(text));
        response.put("shouldEndSession", endSession);

        return response;
    }

    private Map<String, Object> buildOutputSpeech(String message) {
    	Map<String, Object> text = new HashMap<>();
    	text.put("type", "PlainText");
    	text.put("text", message);
    	
    	return(text);
    }
    
    private void sendImageRemoveRequest(String imageId, Context context) throws Exception {
    	AWSIotMqttClient client = createConnectedClient(context);    	
	
		JSONObject arguments = new JSONObject();
		arguments.put("imageId", imageId);
		JSONObject command = new JSONObject();
		command.put("type", "camera");
		command.put("mode", "image");
		command.put("action", "remove");
		command.put("arguments", arguments);
		client.publish(TOPIC, AWSIotQos.QOS0, command.toString());
		client.disconnect();
	}

    private void sendImageTagRequest(String imageId, String tag, Context context) throws Exception {
    	AWSIotMqttClient client = createConnectedClient(context);    	
	
		JSONObject arguments = new JSONObject();
		arguments.put("imageId", imageId);
		arguments.put("tag", tag);
		JSONObject command = new JSONObject();
		command.put("type", "camera");
		command.put("mode", "image");
		command.put("action", "tag");
		command.put("arguments", arguments);
		client.publish(TOPIC, AWSIotQos.QOS0, command.toString());
		client.disconnect();
	}

    private Future<JSONObject> sendImageCaptureRequest(Context context) throws Exception {
    	AWSIotMqttClient client = createConnectedClient(context);    	
	
		TopicResponseListener listener = new TopicResponseListener(client, TOPIC, RESPONSE_TYPE_CAMERA, RESPONSE_REPLY_IMAGE, context);
   
		JSONObject command = new JSONObject();
		command.put("type", "camera");
		command.put("mode", "image");
		command.put("action", "capture");
		client.publish(TOPIC, AWSIotQos.QOS0, command.toString());
		
		return listener;
	}
    
    private AWSIotMqttClient createConnectedClient(Context context) throws Exception {
    	String awsAccessKeyId = System.getenv("awsAccessKeyId");
    	String awsSecretAccessKey = System.getenv("awsSecretAccessKey");
    	String endpoint = System.getProperty("awsIoTEndpoint", "a131ws0b6gtght.iot.us-west-2.amazonaws.com");

    	if ((awsAccessKeyId == null) || (awsSecretAccessKey == null) || awsAccessKeyId.isEmpty() || awsSecretAccessKey.isEmpty()) {
    		throw new Exception("No edge device credentials.");
    	}
    	
    	AWSIotMqttClient client = new AWSIotMqttClient(endpoint, EDGE_DEVICE_CLIENT_ID, awsAccessKeyId, awsSecretAccessKey);    	
		client.connect();
	
		context.getLogger().log("Connected to IoT queue\n");
		
		return client;
    }
    
    private static class TopicResponseListener extends AWSIotTopic implements Future<JSONObject> {
    	
    	private AWSIotMqttClient client;
    	private String type;
    	private String reply;
    	private Context context;
    	private JSONObject result = null;
    	private Exception exception = null;
    	private boolean cancelled = false;
    	
        public TopicResponseListener(AWSIotMqttClient client, String topic, String type, String reply, Context context) 
        	throws Exception {
            super(topic, AWSIotQos.QOS0);
            this.client = client;
            this.type = type;
            this.reply = reply;
            this.context = context;
            
            this.client.subscribe(this);
        }

        @Override
        public void onMessage(AWSIotMessage message) {
            context.getLogger().log("IoT Queue: " + message.getTopic() + " => " + message.getStringPayload());
			log("onMessage(): done=" + isDone());
        	if(!isDone()) {
        		try {
		            JSONObject payload = new JSONObject(message.getStringPayload());
		            if(type.equals(payload.optString("type")) && reply.equals(payload.optString("reply"))) {
		                context.getLogger().log("IoT Queue processing payload");
		            	synchronized (this) {
		            		if (!cancelled) {
			            		result = payload;
		            		}
		            	}
		            }
        		}
        		catch (Exception e) {
	            	synchronized (this) {
	            		if (!cancelled) {
	            			exception = e;
	            		}
	            	}
        		}
        	}
        }

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			
			boolean cancelSucceeded = false;
			synchronized (this) {
				if (!cancelled && !isDone()) {
					try {
						cleanupEdgeRequest();
					}
					catch (AWSIotException | AWSIotTimeoutException e) {
						context.getLogger().log("Exception cleaning up edge request: " + e.getMessage());
					}
					cancelled = true;
					cancelSucceeded = true;
				}
			}

			return cancelSucceeded;
		}

		@Override
		public synchronized boolean isCancelled() {
			return cancelled;
		}

		@Override
		public synchronized boolean isDone() {
			return cancelled || (result != null) || (exception != null);
		}

		@Override
		public JSONObject get() throws InterruptedException, ExecutionException {
			
			while(!isDone()) {
				Thread.sleep(100);
			}
			
			if(exception != null) {
				throw new ExecutionException(exception);
			}

			return result;
		}

		@Override
		public JSONObject get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			
			log("get(): done=" + isDone());
			if(!isDone()) {
				long millisTimeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
				Thread.sleep(millisTimeout);
			}
			
			if(!isDone()) {
				throw new TimeoutException();
			}

			if(exception != null) {
				throw new ExecutionException(exception);
			}

			return result;
		}
		
		private void log(String message) {
			String logMessage = "this=" + System.identityHashCode(this) + " thread=" + 
					Thread.currentThread().getName() + ": " + message; 
			context.getLogger().log(logMessage);
		}
        
        private void cleanupEdgeRequest() throws AWSIotException, AWSIotTimeoutException {
        	client.unsubscribe(this);
        	client.disconnect(100, true /* blocking */);
        }
    }

}
