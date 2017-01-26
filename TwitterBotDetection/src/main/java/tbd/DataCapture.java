package tbd;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import models.LabelledUser;
import models.UserProfile;
import twitter4j.Status;
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
	
	/**
	 * Reads the statuses from a json file containing a twitter status
	 * per line. For each status the user is read and the status associated
	 * with that user.
	 * 
	 * If a status has no user associated with it, it is discarded.
	 * 
	 * @param filename - the file to process
	 * @return - a list of UserProfiles based on the users found.
	 */
	public static List<UserProfile> readStatusFile(String filename) {
	
		List<Status> tweets = new ArrayList<Status>();						//Set of statuses/tweets obtained from the file.
		List<UserProfile> users = new ArrayList<UserProfile>();				//Set of users from file.
		
		/*Read in a file line by line, where each line is a status/tweet.
		 *Extract distinct users from these statuses/tweets.
		 */
		try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
			logger.info("Reading from status file {}...", filename);	
	
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
						List<Status> statuses = new ArrayList<Status>();
						statuses.add(tweet);
						users.add(new UserProfile(null, newUser, statuses));
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
	
	/**
	 * Reads and processes a file of a form where:
	 * 	- Each line describes one user in the dataset.
	 *  - Each line is formatted as follows:
	 *  	<label> <user_id> <tweet_id_1> <tweet_id_2> ... <tweet_id_n>
	 *  - <label>: either "bot", or "human", indicating the label the user received from the corresponding labeling method.
	 *	- <user_id>: the user's Twitter ID.
	 *	- <tweet_id_1:n>: the status Ids for a user.
	 * 
	 * @param filename
	 * @return - A list of LabelledUsers
	 */
	public static List<LabelledUser> readLabelledFile(String filename) {
		
		List<LabelledUser> labelledUsers = new ArrayList<LabelledUser>();	//Temp. set of users, to be populated as UserProfiles
		
		/* Read in a file line by line.
		 * Read each user as a LabeledUser, which contains only ids
		 * and not populated User or Status objects.
		 */
		try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
			logger.info("Reading from classified file {}...", filename);	
	
			String line = reader.readLine();								//Current line.
			
			while (line != null) {
				String[] tokens = line.split(" ");
				
				LabelledUser newUser = new LabelledUser();
				
				newUser.setLabel(tokens[0]);
				newUser.setUserId(Long.parseLong(tokens[1]));
				
				for (int i=2; i<tokens.length; i++) {
					newUser.addStatus(Long.parseLong(tokens[i]));
				}
				
				//Add user to temp. list.
				labelledUsers.add(newUser);
				
				line = reader.readLine();
			}
			
			logger.info("Obtained {} labelled users.", labelledUsers.size());
		}
		catch (IOException e) {
			logger.error("Error reading input file.");
			e.printStackTrace();
		    System.exit(-1);
        }
		
		return labelledUsers;
	}

}
