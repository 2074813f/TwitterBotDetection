package accountProperties;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import models.UserProfile;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class AccountChecker {
	
	static Logger logger = LogManager.getLogger();
	
	/**
	 * Performs a lookup on a set of users by id, up to 100 at a time,
	 * to determine whether they are accessible, and discards the user if
	 * not.
	 * 
	 * TODO: store returned user information.
	 * 
	 * @param user
	 * @return - the List of UserProfiles with inaccessible profiles removed.
	 */
	public static List<UserProfile> filter_accessible(Twitter twitter, List<UserProfile> users) {

		List<UserProfile> processed = users;
		List<UserProfile> toRemove = new ArrayList<UserProfile>();
		
		int index = 0;
		
		while (index < users.size()) {
			List<UserProfile> toBeProcessed;
			
			//Take up to 100 users at a time, bounded by size of list.
			if (index+100 < users.size()) {
				toBeProcessed = users.subList(index, index+100);
				index += 100;
			}
			else {
				toBeProcessed = users.subList(index, users.size());
				index = users.size();
			}
			
			//Get the ids of a subset of users to check.
			long[] userIds = toBeProcessed.stream()
					.map(user -> user.getUser()
					.getId())
					.mapToLong(Long::longValue)	//Note: need to map to Long object before toArray().
					.toArray();
			
			try {
				//Do the lookup of the users.
				ResponseList<User> response = twitter.lookupUsers(userIds);
				logger.info("Found {} users.", response.size());
				
				//If a user is not in the response, remove the user.
				for (UserProfile user : toBeProcessed) {
					if (!response.contains(user.getUser())) {
						//Note to remove the user, avoids perturbing list size for List.subList()
						toRemove.add(user);
					}
				}
				
			}
			catch (TwitterException e) {
				//Edge case where no single user is matched.
				if (e.getErrorCode() == 17) {
					logger.info("No user profiles in batch, removing all");
					toRemove.addAll(toBeProcessed);
				}
				else {
					logger.error("Error checking user profile: ", e);
				}
			}
			
		}
		
		for (UserProfile user : toRemove) {
			processed.remove(user);
		}
		
		return processed;
	}

}
