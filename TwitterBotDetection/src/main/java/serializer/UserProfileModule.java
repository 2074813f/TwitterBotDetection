package serializer;

import com.fasterxml.jackson.databind.module.SimpleModule;

import models.UserProfile;
import twitter4j.User;

public class UserProfileModule extends SimpleModule {

	public UserProfileModule() {
		super();
		addSerializer(User.class, new UserSerializer());
	}
}
