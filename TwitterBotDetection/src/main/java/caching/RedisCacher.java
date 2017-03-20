package caching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lambdaworks.redis.api.sync.RedisCommands;

import models.UserProfile;
import serializer.UserProfileObjectMapper;
import twitter4j.Status;
import twitter4j.User;

public class RedisCacher {
	
	private RedisCommands<String, String> redisApi;
	private UserProfileObjectMapper mapper;
	
	public RedisCacher(RedisCommands<String, String> redisApi) {
		this.redisApi = redisApi;
		this.mapper = new UserProfileObjectMapper();
	}
	
	/**
	 * Cache an object into a Redis server given a Redis API
	 * Interface.
	 * 
	 * @param redisApi
	 * @param o
	 * @throws JsonProcessingException
	 */
	public void cacheObject(Object o) throws JsonProcessingException {
		//If object is a User...
		if (o instanceof User) {
			User user = (User) o;
			String marshalledUser = mapper.writeValueAsString(user);
			redisApi.set("user:"+user.getId(), marshalledUser);
		}
		//If object is a status...
		else if (o instanceof Status) {
			Status status = (Status) o;
			String marshalledStatus = mapper.writeValueAsString(status);
			redisApi.set("status:"+status.getId(), marshalledStatus);
		}
		//If object is a UserProfile...
		else if (o instanceof UserProfile){
			UserProfile user = (UserProfile) o;
			String marshalledUserProfile = mapper.writeValueAsString(user);
			redisApi.set("userprofile:"+user.getUser().getId(), marshalledUserProfile);
		}
		//Else don't know how to store.
		else {
			throw new RuntimeException(String.format("Don't know how to store %s object.", o.getClass().getName()));
		}
	}
	
	/**
	 * Cache the String "null" against a given key, to indicate that the object
	 * was not retrievable from Twitter, i.e. it was "looked-up" but not found.
	 * 
	 * @param redisApi - Redis instance API
	 * @param key - the key 
	 */
	public void cacheNullRef(String key) {
		//TODO: identify object type automatically as with cacheObject and abstract key concat.
		
		//If we are given a reference as a key for Redis, then store.
		if (key != null) {
			redisApi.set(key, "null");
		}
		else {
			throw new RuntimeException(String.format("Key cannot be null!"));
		}
	}

}
