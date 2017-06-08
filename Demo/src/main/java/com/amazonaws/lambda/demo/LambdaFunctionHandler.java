package com.amazonaws.lambda.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class LambdaFunctionHandler implements RequestHandler<Object, Map<String,Object>> {

    @Override
    public Map<String,Object> handleRequest(Object input, Context context) {
        context.getLogger().log("Input: " + input);

        Map<String, Object> map = null;
        try {
            context.getLogger().log("Input type is " + input.getClass().getName());
        	Map inputMap = (Map) input;
            context.getLogger().log("Getting name");
        	//String name = inputJson.getJSONObject("request").getJSONObject("intent").getJSONObject("slots").getJSONObject("name").getString("value");
        	String name = ((Map)((Map)((Map)((Map)inputMap.get("request")).get("intent")).get("slots")).get("name")).get("value").toString();
        	
        	JSONObject responseJson = new JSONObject();
        	addOutputSpeech(responseJson, "Hello " + name);
            JSONObject outputJson = new JSONObject();
        	outputJson.put("version", "1.0");
        	outputJson.put("sessionAttributes", new JSONObject());
        	outputJson.put("shouldEndSession", false);
        	outputJson.put("response", responseJson);
       /* 	  
        	    "card": {
        	      "type": "Simple",
        	      "title": "Horoscope",
        	      "content": "Today will provide you a new learning opportunity.  Stick with it and the possibilities will be endless."
        	    },
        	    "reprompt": {
        	      "outputSpeech": {
        	        "type": "PlainText",
        	        "text": "Can I help you with anything else?"
        	      }
        	}*/
            context.getLogger().log("Output: " + outputJson);
            
    		ObjectMapper mapper = new ObjectMapper();

    		// read JSON from a file
    		map = mapper.readValue(outputJson.toString(), new TypeReference<Map<String, Object>>() {});
        }
        catch (Exception e) {
        	context.getLogger().log("EXCEPTION: " + e.getMessage());
        }
 
        context.getLogger().log("Output Map: " + map);

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
