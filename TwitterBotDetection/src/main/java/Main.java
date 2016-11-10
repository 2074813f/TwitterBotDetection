import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Util.TwitterConfig;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
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
		
		try (BufferedReader reader = new BufferedReader( new FileReader(args[0]))){
			logger.info("Reading from file...");
			
			List<Status> tweets = new ArrayList<Status>();		//Set of statuses/tweets obtained from the file.
			Map<Long, User> users = new HashMap<Long, User>();	//Set of users from statuses.
			String line = reader.readLine();					//Current line.
			
			while (line != null) {
				//Unmarshell status.
				Status tweet = TwitterObjectFactory.createStatus(line); 
				tweets.add(tweet);
				
				//Iff status has user && user not in users then add to users.
				User newUser = tweet.getUser();
				if (newUser != null) {
					users.putIfAbsent(newUser.getId(), newUser);
					logger.info(newUser.getId());
				}
				
				line = reader.readLine();
			}
			
			logger.info("Unmarshelled {} statuses from file.", tweets.size());
			logger.info("Obtained {} users.", users.size());
		}
		catch (Exception e) {
			logger.error(e.toString());
		}

	}

}
