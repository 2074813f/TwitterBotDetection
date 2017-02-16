package serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import models.UserProfile;
import twitter4j.TwitterObjectFactory;

public class UserProfileSerializer extends JsonSerializer<UserProfile>{

	@Override
	public void serialize(UserProfile value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		
		gen.writeStartObject();
		gen.writeStringField("label", value.getLabel());
		gen.writeObjectField("features", value.getFeatures());
		gen.writeObjectField("marshalledUser", value.getMarshalledUser());
		
		gen.writeObjectField("marshalledTrainingStatuses", value.getMarshalledTrainingStatuses());
		gen.writeObjectField("marshalledUserTimeline", value.getMarshalledUserTimeLine());
		gen.writeEndObject();
	}

}
