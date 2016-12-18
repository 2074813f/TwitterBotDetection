package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Provides attempted setup of twitter instance auth.
 * @author Adam
 *
 */
public class TwitterConfig {
	
	static Logger logger = LogManager.getLogger();
	
	public static Twitter authTwitter() {
		try {
			String oathAccessToken = System.getenv("WTF_TWITTER_ACCESS_TOKEN");
			String oathAccessTokenSecret = System.getenv("WTF_TWITTER_ACCESS_TOKEN_SECRET");
			String oathConsumerKey = System.getenv("WTF_TWITTER_CONSUMER_KEY");
			String oathConsumerKeySecret = System.getenv("WTF_TWITTER_CONSUMER_SECRET");
			
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setDebugEnabled(true)
			  .setOAuthConsumerKey(oathConsumerKey)
			  .setOAuthConsumerSecret(oathConsumerKeySecret)
			  .setOAuthAccessToken(oathAccessToken)
			  .setOAuthAccessTokenSecret(oathAccessTokenSecret);
			
			
			OAuthAuthorization auth = new OAuthAuthorization(cb.build());
			Twitter twitter = new TwitterFactory().getInstance(auth);
			
			User user = twitter.verifyCredentials();
			
			logger.info("Verified user: {}", user.getScreenName());
			
			return twitter;
		}
		catch (Exception e){
			logger.error("Couldn't authenticate:", e);
			return null;
		}
	}
}
