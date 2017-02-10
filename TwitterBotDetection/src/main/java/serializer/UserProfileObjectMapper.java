package serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class UserProfileObjectMapper extends ObjectMapper{
	
	public UserProfileObjectMapper() {
		registerModule(new UserProfileModule());
		//enable(SerializationFeature.INDENT_OUTPUT);
	}

}
