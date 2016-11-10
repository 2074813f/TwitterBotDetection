package Util;

import java.util.logging.Logger;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Provides attempted setup of twitter instance auth.
 * @author Adam
 *
 */
public class TwitterConfig {
	
	public static void checkTwitterKeys() throws Exception {
		String oathAccessToken = System.getenv("WTF_TWITTER_ACCESS_TOKEN");
		System.setProperty("twitter4j.oauth.accessToken", oathAccessToken);
		
		String oathAccessTokenSecret = System.getenv("WTF_TWITTER_ACCESS_TOKEN_SECRET");
		System.setProperty("twitter4j.oauth.accessTokenSecret", oathAccessTokenSecret);
		
		String oathConsumerKey = System.getenv("WTF_TWITTER_CONSUMER_KEY");
		System.setProperty("twitter4j.oauth.consumerKey", oathConsumerKey);
		
		String oathConsumerKeySecret = System.getenv("WTF_TWITTER_CONSUMER_SECRET");
		System.setProperty("twitter4j.oauth.consumerSecret", oathConsumerKeySecret);
	}
}
