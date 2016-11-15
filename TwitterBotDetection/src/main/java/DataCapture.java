import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import models.UserProfile;
import twitter4j.HttpResponseCode;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;

/**
 * Provides a method to read a set of statuses from a file, extract the user for
 * each status and construct a set of users and associated statuses.
 * 
 * The UserAccounts that are extracted are returned as a Map, keyed by user id,
 * for constant time lookup.
 */
public class DataCapture {
	
	static Logger logger = LogManager.getLogger();
	
	public static List<UserProfile> ReadStatusFile(String filename) {
	
		List<Status> tweets = new ArrayList<Status>();						//Set of statuses/tweets obtained from the file.
		List<UserProfile> users = new ArrayList<UserProfile>();				//Set of users from file.
		
		/*Read in a file line by line, where each line is a status/tweet.
		 *Extract distinct users from these statuses/tweets.
		 */
		try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
			logger.info("Reading from file...");	
	
			String line = reader.readLine();					//Current line.
			
			while (line != null) {
				//Unmarshal status.
				Status tweet = TwitterObjectFactory.createStatus(line); 
				tweets.add(tweet);
				
				User newUser = tweet.getUser();
				
				//Iff status has user && user not in users then add to users.
				if (newUser != null) {
					int index = users.indexOf(newUser);
					
					//If the user is present, add status, else add user
					if (index != -1) {
						users.get(index).addStatus(tweet);
					}
					else {
						users.add(new UserProfile(newUser, tweet));
					}
				}
				
				line = reader.readLine();
			}
			
			logger.info("Unmarshelled {} statuses from file.", tweets.size());
			logger.info("Obtained {} users.", users.size());
		}
		catch (IOException e) {
			logger.error("Error reading file: ", e.toString());
		}
		catch (TwitterException t) {
			logger.error("Error Unmarshalling status: ", t.toString());
		}
		
		return users;
	}
	
	public static void WriteUserAccountFile(List<UserProfile> users, String filename) {
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
			
			for (UserProfile user : users) {
				//TODO: combine json representations of user and statuses and write to single line.
				
				ObjectMapper mapper = new ObjectMapper();

				//Object to JSON in String
				mapper.writeValue(new File(filename), user);
			}
		}
		catch (IOException e) {
			logger.error("Error writing to file: ", e.toString());
		}
	}
	
	/**
	 * Get statuses from the timeline of a given user, and add to the list of statuses
	 * for that user.
	 * 
	 * @param twitter
	 * @param user
	 */
	public static void AddTimeline(Twitter twitter, UserProfile user) {
		
		//TODO: deal with rate limit.
		try {
			twitter.getUserTimeline(user.getUser().getId())
				.forEach(result -> user.addStatus(result));
		}
		catch (TwitterException e) {
			
			// if user has protected tweets, or if they deleted their account, we note this.
	        if (e.getStatusCode() == HttpResponseCode.UNAUTHORIZED ||
	            e.getStatusCode() == HttpResponseCode.NOT_FOUND) {
	        	logger.error("User not found or inaccessible: {}", user.getUser().getId(), e);
	        }
	        else {
				logger.error("Unknown error retrieving timeline for user: {}", user.getUser().getId(), e);
	        }

		}
		
	}

}
