package persistenceTests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import twitter4j.Twitter;
import twitter4j.User;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import util.TwitterConfig;

public class RedisTest {
	
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
		syncCommands.flushall();
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
                long userId = 791455969016442881L;       //user:2074813fadam

                //Get the Twitter user.
                User user = twitter.showUser(userId);

                String marshalledUser = mapper.writeValueAsString(user);

                //Persist, retrieve, compare.
                syncCommands.set("user:"+user.getId(), marshalledUser);
                String returned = syncCommands.get("user:"+user.getId());
                User returnedUser = TwitterObjectFactory.createUser(returned);

                assertTrue(user.compareTo(returnedUser) == 0);
        }
}
