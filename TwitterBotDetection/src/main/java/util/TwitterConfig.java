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
			//System.setProperty("twitter4j.oauth.accessToken", oathAccessToken);
			
			String oathAccessTokenSecret = System.getenv("WTF_TWITTER_ACCESS_TOKEN_SECRET");
			//System.setProperty("twitter4j.oauth.accessTokenSecret", oathAccessTokenSecret);
			
			String oathConsumerKey = System.getenv("WTF_TWITTER_CONSUMER_KEY");
			//System.setProperty("twitter4j.oauth.consumerKey", oathConsumerKey);
			
			String oathConsumerKeySecret = System.getenv("WTF_TWITTER_CONSUMER_SECRET");
			//System.setProperty("twitter4j.oauth.consumerSecret", oathConsumerKeySecret);
			
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
