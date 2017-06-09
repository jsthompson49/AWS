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
            context.getLogger().log("Input type is " + input.getClass().getName());
            String name = getMap(input, new String[] { "request", "intent", "slots", "name" }).get("value").toString();
        	
            JSONObject responseJson = new JSONObject();
        	addOutputSpeech(responseJson, "Hello " + name);
            JSONObject outputJson = new JSONObject();
        	outputJson.put("version", "1.0");
        	outputJson.put("sessionAttributes", new JSONObject());
        	outputJson.put("shouldEndSession", false);
        	outputJson.put("response", responseJson);
            context.getLogger().log("Output: " + outputJson);
            
    		ObjectMapper mapper = new ObjectMapper();

    		map = mapper.readValue(outputJson.toString(), new TypeReference<Map<String, Object>>() {});
        }
        catch (Exception e) {
        	context.getLogger().log("EXCEPTION: " + e.getMessage());
        }
 
        context.getLogger().log("Output Map: " + map);

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
    
    private void addOutputSpeech(JSONObject json, String text) throws Exception {
    	JSONObject textJson = new JSONObject();
    	textJson.put("type", "PlainText");
    	textJson.put("text", text);
    	json.put("outputSpeech", textJson);
    }
}
