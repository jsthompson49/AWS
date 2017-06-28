package demo.pi.control;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;

public class Main {

	private static final String TOPIC = "demo/command";
	
	private enum Mode {
		EDGE, CLIENT	
	}
	
    public static void main( String[] args )
    {
    	Mode mode = (args.length < 1) ? Mode.CLIENT : Mode.valueOf(args[0]);
    	String endpoint = (args.length < 2) ? "a131ws0b6gtght.iot.us-west-2.amazonaws.com" : args[1];
    	String id = (args.length < 3) ? "demo" : args[2];
    	
    	try {
	    	System.out.println("Creating client \"" + id + "\" to endpoint " + endpoint);
	    	
	    	AWSIotMqttClient client = Authentication.getAuthenticatedClient(endpoint, id);
	    	if (client == null) {
	    		System.out.println("Client not configured");
	    	}
	    	else {
		    	System.out.println("Connecting ...");
		    	
		    	client.connect();
		    	
		    	System.out.println("System Ready.");
		    	
		        TopicSubscriber topic = new TopicSubscriber(TOPIC, AWSIotQos.QOS0);
		        client.subscribe(topic);

		        if (mode.equals(Mode.CLIENT)) {
		        	client.publish(TOPIC, AWSIotQos.QOS0, "Test");	  
		        }

		        while (true) {
	        		Thread.sleep(1000);
	        		System.out.println("Status: " + client.getConnectionStatus());
	        	}
	    	}
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	System.out.println("Exiting");
    	System.exit(0);	
    }
}
    
class TopicSubscriber extends AWSIotTopic {
    public TopicSubscriber(String topic, AWSIotQos qos) {
        super(topic, qos);
    }

    @Override
    public void onMessage(AWSIotMessage message) {
        System.out.println("Recieved from " + message.getTopic() + ": " + message.getStringPayload());
    }
}
