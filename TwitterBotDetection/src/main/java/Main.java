import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import features.AccountExtractor;
import models.UserAccount;
import models.UserFeatures;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;
import util.TwitterConfig;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

public class Main {
	
	public static void main(String[] args) {
		
		Logger logger = LogManager.getLogger();
		
		//Get a twitter instance and attempt to authenticate with env variables.
		Twitter twitter = TwitterFactory.getSingleton();
		try {
			TwitterConfig.checkTwitterKeys();
		}
		catch (Exception e){
			logger.error("Couldn't authenticate:", e);
			System.exit(-1);
		}
		
		List<Status> tweets = new ArrayList<Status>();						//Set of statuses/tweets obtained from the file.
		Map<Long, UserAccount> users = new HashMap<Long, UserAccount>();	//Set of users from file.
		
		/*Read in a file line by line, where each line is a status/tweet.
		 *Extract distinct users from these statuses/tweets.
		 */
		try (BufferedReader reader = new BufferedReader( new FileReader(args[0]))){
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
		
		//Extract account features for each user
		List<UserFeatures> features = new ArrayList<UserFeatures>();
		features = users.entrySet()
				.parallelStream()
				.map(e -> AccountExtractor.extractFeatures(twitter, e.getValue()))
				.collect(Collectors.toList());
		
		logger.info("Extracted account features for {} users", features.size());

	}

}
