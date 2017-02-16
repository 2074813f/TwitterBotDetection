package serializer;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.module.SimpleModule;

import models.UserProfile;
import twitter4j.Status;
import twitter4j.User;

public class UserProfileModule extends SimpleModule {

	public UserProfileModule() {
		super();
		addSerializer(User.class, new UserSerializer());
		addSerializer(Status.class, new StatusSerializer());
		addSerializer(UserProfile.class, new UserProfileSerializer());
		
		addDeserializer(UserProfile.class, new UserProfileDeserializer());
	}
}
