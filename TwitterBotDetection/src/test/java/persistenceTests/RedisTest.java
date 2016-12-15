package persistenceTests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;


public class RedisTest {
	
	//https://raw.githubusercontent.com/MSOpenTech/redis/3.0/Windows%20Service%20Documentation.md
	//**default install (port 6379 and firewall exception ON):**

	private RedisClient redisClient;
	private StatefulRedisConnection<String, String> connection;
	private RedisCommands<String, String> syncCommands;
	
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
	
	@Test
	public void redisConnection() {
		assertTrue(connection.isOpen());
		
		syncCommands.set("testkey", "Hello, Test!");
		
	}
	
//	@Test
//	public void testSet() {
//		syncCommands.set("key", "Hello, Redis!");
//		
//		assertEquals(syncCommands.get("key").compareTo("Hello, Redis!"), 0);
//	}

}
