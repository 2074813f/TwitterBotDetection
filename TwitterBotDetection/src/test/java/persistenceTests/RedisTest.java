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
    	
    	//Get the Twitter user.
        User user = twitter.showUser(userId);
        String key = "user:"+user.getId();
        
        //Marshall the user.
        String marshalledUser = mapper.writeValueAsString(user);

        //Persist, retrieve, Unmarshall.
        syncCommands.set(key, marshalledUser);
        String returned = syncCommands.get("user:"+user.getId());
        User returnedUser = TwitterObjectFactory.createUser(returned);

        //Compare the user objects themselves.
        assertTrue(user.compareTo(returnedUser) == 0);
        
        assertTrue(syncCommands.del(key) == 1);
    }
    
    @Test
    public void setGetTwitterStatus() throws TwitterException, JsonProcessingException {
    	
    	//Get the Twitter user.
    	Status status = twitter.showStatus(statusId);
        String key = "status:"+status.getId();
        
        
        //Marshall the status.
        String marshalledStatus = mapper.writeValueAsString(status);

        //Persist, retrieve, compare.
        syncCommands.set(key, marshalledStatus);
        String returned = syncCommands.get("status:"+status.getId());
        Status returnedStatus = TwitterObjectFactory.createStatus(returned);

        //Compare the user objects themselves.
        assertTrue(status.compareTo(returnedStatus) == 0);
        
        assertTrue(syncCommands.del(key) == 1);
    }
}
