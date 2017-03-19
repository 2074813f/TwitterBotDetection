package persistenceTests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;

import models.Features;
import models.UserProfile;
import serializer.UserProfileObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.User;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import util.TwitterConfig;

public class RedisTest {
	//TODO: Switch to test client in Redis, and flushall/delete after testing.
	
	private long userId = 791455969016442881L;       	//user:2074813fadam
	private long statusId = 818531952391311360L;		//user:2074813fadam (test status)
	private String userKey = "user:"+userId;
	private String statusKey = "status:"+statusId;
	private String userProfileKey = "userprofile:"+userId;
	
	//https://raw.githubusercontent.com/MSOpenTech/redis/3.0/Windows%20Service%20Documentation.md
	//**default install (port 6379 and firewall exception ON):**
	private RedisClient redisClient;
	private StatefulRedisConnection<String, String> connection;
	private RedisCommands<String, String> syncCommands;

	private Twitter twitter = TwitterConfig.authTwitter();
    private UserProfileObjectMapper mapper = new UserProfileObjectMapper();

	@Before
	public void setUp() {
		redisClient = RedisClient.create("redis://localhost:6379");
		connection = redisClient.connect();
		syncCommands = connection.sync();
	}
	
	@After
	public void destroy() {
		//Remove the keys from the store if they exist.
		syncCommands.del(userKey);
		syncCommands.del(statusKey);
		syncCommands.del(userProfileKey);
		
		connection.close();
		redisClient.shutdown();
	}
	
    /**
     * Test that a both setting and getting are possible.
     */
	@Test
	public void setGetKey() {
		assertTrue(connection.isOpen());
		
		syncCommands.set("testkey", "Hello, Test!");
		syncCommands.get("testkey");
		
		//Delete specifically, since key will be used in no other tests
		syncCommands.del("testkey");
	}
    
    @Test
    public void setGetTwitterUser() throws JsonProcessingException, TwitterException {
    	
    	//Get the Twitter user.
        User user = twitter.showUser(userId);
        
        //Marshall the user.
        String marshalledUser = mapper.writeValueAsString(user);

        //Persist, retrieve, Unmarshall.
        syncCommands.set(userKey, marshalledUser);
        String returned = syncCommands.get(userKey);
        User returnedUser = TwitterObjectFactory.createUser(returned);

        //Compare the user objects themselves.
        assertTrue(user.compareTo(returnedUser) == 0);
    }
    
    @Test
    public void setGetTwitterStatus() throws TwitterException, JsonProcessingException {
    	
    	//Get the Twitter user.
    	Status status = twitter.showStatus(statusId);
        
        //Marshall the status.
        String marshalledStatus = mapper.writeValueAsString(status);

        //Persist, retrieve, compare.
        syncCommands.set(statusKey, marshalledStatus);
        String returned = syncCommands.get(statusKey);
        Status returnedStatus = TwitterObjectFactory.createStatus(returned);

        //Compare the user objects themselves.
        assertTrue(status.compareTo(returnedStatus) == 0);
    }
    
    @Test
    public void setGetUserProfile() throws TwitterException, IOException {
    	Status status = twitter.showStatus(statusId);
    	String marshalledStatus = mapper.writeValueAsString(status);
    	
    	User user = twitter.showUser(userId);
    	
    	List<Status> statuses = new ArrayList<Status>();
    	List<String> marshalledStatuses = new ArrayList<String>();
    	statuses.add(status);
    	marshalledStatuses.add(marshalledStatus);
    	
    	
    	
    	UserProfile profile = new UserProfile();
    	profile.setUser(user, TwitterObjectFactory.getRawJSON(user));
    	profile.setLabel("human");
    	profile.setFeatures(new Features());
    	profile.setTrainingStatuses(statuses, marshalledStatuses);
    	profile.setUserTimeline(statuses, marshalledStatuses);
    	
    	//Marshall the status.
		String marshalledUserProfile = mapper.writeValueAsString(profile);
		
		//Persist, retrieve, compare.
		syncCommands.set(userProfileKey, marshalledUserProfile);
		String returned = syncCommands.get(userProfileKey);
        UserProfile returnedUP = mapper.readValue(returned, UserProfile.class);
    }
}
