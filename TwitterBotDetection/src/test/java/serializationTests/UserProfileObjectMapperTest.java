package serializationTests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import serializer.UserProfileObjectMapper;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;
import util.TwitterConfig;

public class UserProfileObjectMapperTest {
	
	private long userId = 791455969016442881L;       	//user:2074813fadam
	private long statusId = 818531952391311360L;		//user:2074813fadam (test status)
	
	private Twitter twitter = TwitterConfig.authTwitter();
    private UserProfileObjectMapper mapper = new UserProfileObjectMapper();
    
    @Test
    public void serializeUser() throws JsonProcessingException, TwitterException {
    	//Get the Twitter user.
        User user = twitter.showUser(userId);
        
        //Use native Json factory method from Twitter4J.
    	String testUser = TwitterObjectFactory.getRawJSON(user);
    	
    	//Do the marshalling with the custom mapper.
        String marshalledUser = mapper.writeValueAsString(user);
    	
    	//Compare the Strings.
        assertTrue(marshalledUser.equals(testUser));
    }
    
    @Test
    public void serializeStatus() throws TwitterException, JsonProcessingException {
    	//Get the Twitter user.
    	Status status = twitter.showStatus(statusId);
    	
    	//Use native Json factory method from Twitter4J.
    	String testStatus = TwitterObjectFactory.getRawJSON(status);
    	
    	//Do the marshalling with the custom mapper.
        String marshalledStatus = mapper.writeValueAsString(status);
    	
    	//Compare the Strings (due to paranoia).
        assertTrue(marshalledStatus.equals(testStatus));
    }

}
