package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;

public class RedisConfig {
	
	private static RedisClient redisClient;
	private static StatefulRedisConnection<String, String> connection;
	private static RedisCommands<String, String> syncCommands;
	
	static Logger logger = LogManager.getLogger();
	
	public static RedisCommands<String, String> startRedis() {
		redisClient = RedisClient.create("redis://localhost:6379");
		connection = redisClient.connect();
		syncCommands = connection.sync();
		
		return syncCommands;
	}
	
	public static void stopRedis() {
		connection.close();
		redisClient.shutdown();
	}

}
