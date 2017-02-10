package serializer;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import twitter4j.Status;
import twitter4j.TwitterObjectFactory;

public class StatusesSerializer extends JsonSerializer<List<Status>> {

	@Override
	public void serialize(List<Status> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		
		gen.writeStartArray();
		for (Status status : value) {
			String marshalledStatus = TwitterObjectFactory.getRawJSON(status);
			gen.writeRaw(marshalledStatus);
		};
		gen.writeEndArray();

	}

}
