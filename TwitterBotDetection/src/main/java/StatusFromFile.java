import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import models.UserAccount;
import twitter4j.Status;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;

/**
 * Provides a method to read a set of statuses from a file, extract the user for
 * each status and construct a set of users and associated statuses.
 * 
 * The UserAccounts that are extracted are returned as a Map, keyed by user id,
 * for constant time lookup.
 */
public class StatusFromFile {
	
	public static Map<Long, UserAccount> ReadStatusFile(String filename) {
		
		Logger logger = LogManager.getLogger();
	
		List<Status> tweets = new ArrayList<Status>();						//Set of statuses/tweets obtained from the file.
		Map<Long, UserAccount> users = new HashMap<Long, UserAccount>();	//Set of users from file.
		
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
				long id = newUser.getId();
				
				//Iff status has user && user not in users then add to users.
				if (newUser != null) {
					
					//If the user is present, add status, else add user
					if (users.containsKey(id)) {
						users.get(id).addStatus(tweet);
					}
					else {
						users.put(id, new UserAccount(newUser, tweet));
					}
				}
				
				line = reader.readLine();
			}
			
			logger.info("Unmarshelled {} statuses from file.", tweets.size());
			logger.info("Obtained {} users.", users.size());
		}
		catch (Exception e) {
			logger.error(e.toString());
		}
		
		return users;
	}

}
