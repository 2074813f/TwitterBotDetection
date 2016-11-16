package accountProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import models.LabelledUser;
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
	public static List<UserProfile> filter_accessible(Twitter twitter, List<UserProfile> users) throws RuntimeException {

		//XXX: Might be better to refactor this outside this function and return list of ids/users to remove.
		List<UserProfile> processed = users;
		List<UserProfile> toRemove = new ArrayList<UserProfile>();
		
		int index = 0;
		
		while (index < users.size()) {
			List<UserProfile> toBeProcessed;	//View of sublist of <=100 users
			
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
			
			//Do the lookup
			ResponseList<User> response = lookup(twitter, userIds);
			
			//If response is empty then remove all users
			if (response == null) {
				toRemove.addAll(toBeProcessed);
			}
			else {
				//If a user is not in the response, remove the user.
				for (UserProfile user : toBeProcessed) {
					if (!response.contains(user.getUser())) {
						//Note to remove the user, avoids perturbing list size for List.subList()
						toRemove.add(user);
					}
				}
			}	
		}
		
		//Remove each User that wasn't retrievable.
		for (UserProfile user : toRemove) {
			processed.remove(user);
		}
		
		//Return the reduced list of Users.
		return processed;
	}
	
	/**
	 * Performs a lookup on a set of users by id, up to 100 at a time,
	 * to determine whether they are accessible, and discards the user if
	 * not.
	 * 
	 * TODO: store returned user information.
	 * TODO: check/get statuses
	 * 
	 * @param user
	 * @return - the List of UserProfiles with inaccessible profiles removed.
	 */
	public static List<UserProfile> filter_accessible_labelled(Twitter twitter, List<LabelledUser> users) throws RuntimeException {

		//XXX: Might be better to refactor this outside this function and return list of ids/users to remove.
		List<UserProfile> result = new ArrayList<UserProfile>();
		
		int index = 0;
		
		while (index < users.size()) {
			List<LabelledUser> toBeProcessed;	//View of sublist of <=100 users
			
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
					.map(user -> user.getUserId())
					.mapToLong(Long::longValue)	//Note: need to map to Long object before toArray().
					.toArray();
			
			//Do the lookup
			ResponseList<User> response = lookup(twitter, userIds);
			
			//Add the results.
			result.addAll(response.stream()
					.map(user -> new UserProfile(user, null))
					.collect(Collectors.toList()));
		}
		
		//Return the reduced list of Users.
		return result;
	}

	/**
	 * Does the batched userLookup.
	 * 
	 * @param users
	 * @return
	 * @throws InterruptedException 
	 */
	public static ResponseList<User> lookup(Twitter twitter, long[] userIds) throws RuntimeException {
		
		//If rate limit reached, we continue attempting after waiting.
		while(true) {
			try {
				//Do the lookup of the users.
				ResponseList<User> response = twitter.lookupUsers(userIds);
				logger.info("Found {} users.", response.size());
				return response;
			}
			catch (TwitterException e) {
				//Edge case where no single user is matched.
				if (e.getErrorCode() == 17) {
					logger.info("No user profiles in batch, removing all");
					
					return null;
				}
				//Rate limit exceeded, back off, wait, and try again.
				else if (e.getStatusCode() == 503 || e.getStatusCode() == 429){
					logger.info("Rate limit exceeded");
					
					try {
						//Get the recommended wait time and sleep until then.
						int retryafter = e.getRetryAfter();
						Thread.sleep(retryafter / 1000);
					}
					catch (InterruptedException i) {
						logger.error("Sleeping thread interupted for some reason ??? aborting.");
						throw new RuntimeException();
					}
					
					//Try again.
					continue;
				}
				else {
					logger.error("Error checking user profile: ", e);
					throw new RuntimeException();
				}
			}
		}
	}
}
