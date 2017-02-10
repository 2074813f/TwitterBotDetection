package serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import twitter4j.Status;
import twitter4j.TwitterObjectFactory;

public class StatusSerializer extends JsonSerializer<Status>{

	@Override
	public void serialize(Status value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		String marshalledStatus = TwitterObjectFactory.getRawJSON(value);
		
		gen.writeRaw(marshalledStatus);
	}

}
