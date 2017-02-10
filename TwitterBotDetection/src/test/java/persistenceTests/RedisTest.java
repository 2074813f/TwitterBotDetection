package persistenceTests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;

import serializer.UserProfileObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
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
	
	//https://raw.githubusercontent.com/MSOpenTech/redis/3.0/Windows%20Service%20Documentation.md
	//**default install (port 6379 and firewall exception ON):**
	private RedisClient redisClient;
	private StatefulRedisConnection<String, String> connection;
	private RedisCommands<String, String> syncCommands;

    private ObjectMapper mapper = new ObjectMapper();

	@Before
	public void setUp() {
		redisClient = RedisClient.create("redis://localhost:6379");
		connection = redisClient.connect();
		syncCommands = connection.sync();
	}
	
	@After
	public void destroy() {
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
		
		syncCommands.flushall();
	}

    @Test
    public void setGetTwitterUser() throws JsonProcessingException, TwitterException {
        Twitter twitter = TwitterConfig.authTwitter();

        //Get the Twitter user.
        User user = twitter.showUser(userId);
        String key = "user:"+user.getId();

        String marshalledUser = mapper.writeValueAsString(user);

        //Persist, retrieve, compare.
        syncCommands.set(key, marshalledUser);
        String returned = syncCommands.get("user:"+user.getId());
        User returnedUser = TwitterObjectFactory.createUser(returned);

        assertTrue(user.compareTo(returnedUser) == 0);
        
        assertTrue(syncCommands.del(key) == 1);
    }
    
    @Test
    public void setGetTwitterStatus() throws JsonProcessingException, TwitterException {
    	Twitter twitter = TwitterConfig.authTwitter();
    	
    	//Get the Twitter status.
    	Status status = twitter.showStatus(statusId);
    	String key = "status:"+status.getId();
    	
    	String marshalledStatus = mapper.writeValueAsString(status);
    	
    	//Persist, retrieve, compare.
    	syncCommands.set(key, marshalledStatus);
    	String returned = syncCommands.get("status:"+status.getId());
    	Status returnedStatus = TwitterObjectFactory.createStatus(returned);
    	
    	assertTrue(status.compareTo(returnedStatus) == 0);
    	
    	assertTrue(syncCommands.del(key) == 1);
    }
    
    @Test
    public void setGetTwitterUserCustom() throws JsonProcessingException, TwitterException {
    	Twitter twitter = TwitterConfig.authTwitter();
    	UserProfileObjectMapper newmapper = new UserProfileObjectMapper();
    	
    	//Get the Twitter user.
        User user = twitter.showUser(userId);
        String key = "user:"+user.getId();
        String testUser = TwitterObjectFactory.getRawJSON(user);
        
        //Do the marshalling with the custom mapper.
        String marshalledUser = newmapper.writeValueAsString(user);

        //Persist, retrieve, compare.
        syncCommands.set(key, marshalledUser);
        String returned = syncCommands.get("user:"+user.getId());
        User returnedUser = TwitterObjectFactory.createUser(returned);

        //Compare the user objects themselves.
        assertTrue(user.compareTo(returnedUser) == 0);
        //Compare the Strings (due to paranoia).
        assertTrue(returned.equals(testUser));
        
        assertTrue(syncCommands.del(key) == 1);
    }
}
