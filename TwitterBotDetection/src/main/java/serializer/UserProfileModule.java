package serializer;

import com.fasterxml.jackson.databind.module.SimpleModule;

import models.UserProfile;
import twitter4j.Status;
import twitter4j.User;

public class UserProfileModule extends SimpleModule {

	public UserProfileModule() {
		super();
		addSerializer(User.class, new UserSerializer());
		addSerializer(Status.class, new StatusSerializer());
	}
}
