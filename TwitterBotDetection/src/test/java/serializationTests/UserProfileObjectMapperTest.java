package serializationTests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import models.UserProfile;
import serializer.UserProfileObjectMapper;
import twitter4j.Paging;
import twitter4j.ResponseList;
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
    
    @Test
    public void serializeUserProfile() throws TwitterException, JsonProcessingException {
    	//Get the Twitter user.
        User user = twitter.showUser(userId);
        //Get the Twitter status.
    	Status status = twitter.showStatus(statusId);
    	List<Status> statuses = new ArrayList<Status>();
    	statuses.add(status);
    	//Get the Twitter user timeline
    	Paging page = new Paging(1, 1);
    	ResponseList<Status> response = twitter.getUserTimeline(user.getId(), page);
    	List<Status> timeline = new ArrayList<Status>();
    	//timeline.addAll(response);
    	response.forEach(returnedstatus -> timeline.add(returnedstatus));
    	
    	//Construct the UserProfile
    	UserProfile profile = new UserProfile("human", user, statuses);
    	profile.setUserTimeline(timeline);
    	
    	String marshalledUserProfile = mapper.writeValueAsString(profile);
    }

}
