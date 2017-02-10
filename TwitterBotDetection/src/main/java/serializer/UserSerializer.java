package serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import twitter4j.TwitterObjectFactory;
import twitter4j.User;

public class UserSerializer extends JsonSerializer<User>{

	@Override
	public void serialize(User value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		String marshalledUser = TwitterObjectFactory.getRawJSON(value);
		
		gen.writeRaw(marshalledUser);
	}

}
