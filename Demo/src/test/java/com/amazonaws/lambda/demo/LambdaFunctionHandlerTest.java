package com.amazonaws.lambda.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class LambdaFunctionHandlerTest {

    private static Map<String,Object> input;

    @BeforeClass
    public static void createInput() throws IOException {
        InputStream data = LambdaFunctionHandlerTest.class.getClassLoader().getResourceAsStream("TestInput.json");
   		ObjectMapper mapper = new ObjectMapper();
		input = mapper.readValue(data, new TypeReference<Map<String, Object>>() {});
    }

    private Context createContext() {
        TestContext ctx = new TestContext();

        ctx.setFunctionName("TestFunctionName");

        return ctx;
    }

    @Test
    public void testLambdaFunctionHandler() throws Exception {
        LambdaFunctionHandler handler = new LambdaFunctionHandler();
        Context ctx = createContext();

        Map<String,Object> output = handler.handleRequest(input, ctx);

        System.out.println(output);
        Map<String,Object> response = LambdaFunctionHandler.getMap(output, "response");
        Map<String,Object> outputSpeech = LambdaFunctionHandler.getMap(response, "outputSpeech");

        Assert.assertEquals("Hello joe", outputSpeech.get("text"));
    }
}
