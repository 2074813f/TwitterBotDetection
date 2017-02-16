package serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import models.Features;
import models.UserProfile;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;

public class UserProfileDeserializer extends JsonDeserializer<UserProfile> {

	@Override
	public UserProfile deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		UserProfile result = new UserProfile();
		
		JsonNode node = p.getCodec().readTree(p);
		String label = node.get("label").asText();
		Features features = mapper.treeToValue(node.get("features"), Features.class);
		
		//Our Marshalled objects.
		String marshalledUser = node.get("marshalledUser").asText();
		
		List<String> marshalledTrainingStatuses = new ArrayList<String>();
		for (JsonNode child : node.get("marshalledTrainingStatuses")) {
			marshalledTrainingStatuses.add(child.asText());
		}
		
		List<String> marshalledUserTimeline = new ArrayList<String>();
		for (JsonNode child : node.get("marshalledUserTimeline")) {
			marshalledUserTimeline.add(child.asText());
		}
		
		//Convert Marshalled objects to Objects.
		User user = null;
		List<Status> trainingStatuses = new ArrayList<Status>();
		List<Status> userTimeline = new ArrayList<Status>();
		
		try {
			user = TwitterObjectFactory.createUser(marshalledUser);
			
			for (String marshalledStatus : marshalledTrainingStatuses) {
				trainingStatuses.add(TwitterObjectFactory.createStatus(marshalledStatus));
			}

			for (String marshalledStatus : marshalledUserTimeline) {
				userTimeline.add(TwitterObjectFactory.createStatus(marshalledStatus));
			}	
		}
		catch (TwitterException e) {
			e.printStackTrace();
		}
		
		//Populate the User Profile.
		result.setLabel(label);
		result.setFeatures(features);
		result.setUser(user, marshalledUser);
		result.setTrainingStatuses(trainingStatuses, marshalledTrainingStatuses);
		result.setUserTimeline(userTimeline, marshalledUserTimeline);
		
		return result;
	}

}
