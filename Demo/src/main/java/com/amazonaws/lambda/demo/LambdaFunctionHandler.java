package com.amazonaws.lambda.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class LambdaFunctionHandler implements RequestHandler<Map<String,Object>, Map<String,Object>> {

    @Override
    public Map<String,Object> handleRequest(Map<String,Object> input, Context context) {
        context.getLogger().log("Input: " + input);

        Map<String, Object> map = null;
        try {
            // Default response
            JSONObject outputJson = buildResponse("This is not expected. Please try again.", true /* end session */);

            Map<String,Object> requestMap = getMap(input, "request");
            Object type = (requestMap == null) ? null : requestMap.get("type");
            if(type == null) {
                outputJson = buildResponseBody("I don't know how to handle that.", true /* end session */);
            }
            else if(type.equals("LaunchRequest")) {
                outputJson = buildResponseBody("Hello.  I don't recognize you yet.", false /* do not end session */);
            }
            else if(type.equals("IntentRequest")) {
            	Map<String,Object> nameMap = getMap(requestMap, new String[] { "intent", "slots", "name" });
            	Object value = (nameMap == null) ? null : nameMap.get("value");
            	if(value == null) {
                	outputJson = buildResponseBody("Hello.  Can you tell me your name?", false /* do not end session */);
            	}
            	else {
                	outputJson = buildResponseBody("Hello " + value.toString(), true /* end session */);
            	}
            }   
            
    		ObjectMapper mapper = new ObjectMapper();
    		map = mapper.readValue(outputJson.toString(), new TypeReference<Map<String, Object>>() {});
        }
        catch (Exception e) {
        	context.getLogger().log("EXCEPTION: " + e.getMessage() + "\n");
        }
 
		context.getLogger().log("Output Map: " + map + "\n");

        return map;
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
    
    private JSONObject buildResponseBody(String text, boolean endSession) throws Exception {
        JSONObject responseBody = new JSONObject();
        responseBody.put("version", "1.0");
        responseBody.put("sessionAttributes", new JSONObject());
        responseBody.put("response", buildResponse(text, endSession));

        return responseBody;
    }

    private JSONObject buildResponse(String text, boolean endSession) throws Exception {
        JSONObject response = new JSONObject();
        response.put("outputSpeech", buildOutputSpeech(text));
        response.put("shouldEndSession", endSession);

        return response;
    }

    private JSONObject buildOutputSpeech(String message) throws Exception {
    	JSONObject text = new JSONObject();
    	text.put("type", "PlainText");
    	text.put("text", message);
    	
    	return(text);
    }
}
