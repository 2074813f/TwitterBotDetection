package accountProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambdaworks.redis.api.sync.RedisCommands;

import models.LabelledUser;
import models.UserProfile;
import twitter4j.HttpResponseCode;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;
import twitter4j.json.DataObjectFactory;

public class AccountChecker {
	
	static Logger logger = LogManager.getLogger();
	
	/**
	 * Performs a lookup on a set of users by id, up to 100 at a time,
	 * to determine whether they are accessible, and discards the user if
	 * not.
	 * 
	 * TODO: store returned user information.
	 * 
	 * @deprecated - TODO: replace with workings of labelled, then have helper function for adding labels
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
			ResponseList<User> response = lookupUsers(twitter, userIds);
			
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
	 * Performs a lookup on a set of users by id, to determine whether 
	 * they are accessible, and discards the user if not. Returns a list
	 * of fully hydrated users whose profile was accessible.
	 * 
	 * TODO: consider renaming.
	 * TODO: store returned user information.
	 * TODO: check/get statuses
	 * 
	 * @param user
	 * @return - the List of UserProfiles with inaccessible profiles removed.
	 * @throws TwitterException 
	 */
	public static List<UserProfile> getUsers(Twitter twitter, 
			RedisCommands<String, String> redisApi, List<LabelledUser> users) throws RuntimeException {

		ObjectMapper mapper = new ObjectMapper();
		//mapper.registerModule(new MrBeanModule());
		
		List<UserProfile> result = new ArrayList<UserProfile>();
		
		//Construct map for id lookup to match to results.
		Map<Long, LabelledUser> mappedUsers = new HashMap<Long, LabelledUser>();
		users.stream().forEach(user -> mappedUsers.put(user.getUserId(), user));
		
		//Check for cached users.
		logger.info("Checking for cached users...");
		
		//TODO: Consider parallel streams.
		//Check for Users in cache and collect if exists.
		//TODO:Iterate in a safe way.
		List<LabelledUser> toRemove = new ArrayList<LabelledUser>();
		
		for (LabelledUser user : users) {
			String returned = redisApi.get("user:" + user.getUserId());
			if (returned != null) {
				try {
					//Unmarshell User.
					//User returnedUser = mapper.readValue(returned, User.class);
					User returnedUser = TwitterObjectFactory.createUser(returned);
					//Add user to results.
					result.add(new UserProfile(mappedUsers.get(user.getUserId()).getLabel(), returnedUser, new ArrayList<Status>()));
					//Remove from further searching.
					toRemove.add(user);
				}
				catch (TwitterException e) {
					e.printStackTrace();
					//throw new RuntimeException("Failed to unmarshall User from Redis.");
				}
			}
		}
		
		logger.info("Found {} cached users.", result.size());
		//Remove the cached users from the id collection.
		toRemove.forEach(user -> users.remove(user));
		
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
			ResponseList<User> response = lookupUsers(twitter, userIds);
			
			//Add the results, setting the label from mappedUsers.
			if (response != null) {
				result.addAll(response.stream()
						.map(user -> new UserProfile(mappedUsers.get(user.getId()).getLabel(), user, new ArrayList<Status>()))
						.collect(Collectors.toList()));
				for (User user : response) {
					//Add to redis.
					//TODO: Exception on existence of key, should not be in store since earlier check.
					String marshalledUser;
					try {
						marshalledUser = mapper.writeValueAsString(user);
						redisApi.set("user:"+user.getId(), marshalledUser);
					} catch (JsonProcessingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
			
		}
		
		//Return the reduced list of Users.
		return result;
	}

	/**
	 * Performs a lookup on a set of statuses by id, returning a
	 * fully "hydrated" list of statuses.
	 * 
	 * @param twitter
	 * @param users
	 * @return
	 */
	public static List<Status> getStatuses(Twitter twitter, List<LabelledUser> users) {
		List<Status> result = new ArrayList<Status>();
		
		List<Long> allStatusIds = new ArrayList<Long>();
		for (LabelledUser user : users) {
			//XXX: waaaay too many statuses for some users, gonna get rate limited.
			//	Trimmed arbitrarily to 10 statuses per user.
			List<Long> userStatuses = user.getStatusIds();
			if (userStatuses.size() <= 10) {
				//TODO: fill to threshold from twitter.
				allStatusIds.addAll(userStatuses);
			}
			else {
				allStatusIds.addAll(userStatuses.subList(0, 10));
			}
		}
		
		int index = 0;
		
		while (index < allStatusIds.size()) {
			List<Long> toBeProcessed;	//View of sublist of <=Status ids
			
			//Take up to 100 users at a time, bounded by size of list.
			if (index+100 < allStatusIds.size()) {
				toBeProcessed = allStatusIds.subList(index, index+100);
				index += 100;
			}
			else {
				toBeProcessed = allStatusIds.subList(index, allStatusIds.size());
				index = allStatusIds.size();
			}
			
			//Get the ids of a subset of users to check.
			long[] statusIds = toBeProcessed.stream()
					.mapToLong(Long::longValue)	//Note: need to map to Long object before toArray().
					.toArray();
			
			//Do the lookup
			ResponseList<Status> response = lookupStatuses(twitter, statusIds);
			
			//Add the results, setting the label from mappedUsers.
			result.addAll(response.stream()
					.collect(Collectors.toList()));
			
		}
		
		//Return the reduced list of Users.
		return result;
	}
	
	/**
	 * Does the lookup for batches of users.
	 * 
	 * @param users
	 * @return - A ResponseList of Users found, or null if there were no users found.
	 * @throws InterruptedException 
	 */
	private static ResponseList<User> lookupUsers(Twitter twitter, long[] userIds) throws RuntimeException {
		
		//TODO: consider futures
		//If rate limit reached, we continue attempting after waiting.
		while(true) {
			try {
				//Do the lookup of the users.
				ResponseList<User> response = twitter.lookupUsers(userIds);
				logger.info("Found {}({}) users.", response.size(), userIds.length);
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
						logger.error("Sleeping thread interrupted for some reason ??? aborting.");
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
	
	/**
	 * Does the lookup for batches of statuses.
	 * 
	 * @param twitter
	 * @param user
	 */
	private static ResponseList<Status> lookupStatuses(Twitter twitter, long[] statusIds) {
		
		//TODO: consider futures
		//If rate limit reached, we continue attempting after waiting.
		while(true) {
			try {
				//Do the lookup of the users.
				ResponseList<Status> response = twitter.lookup(statusIds);
				logger.info("Found {}({}) statuses.", response.size(), statusIds.length);
				return response;
			}
			catch (TwitterException e) {
				//Edge case where no single user is matched.
				if (e.getErrorCode() == 17) {
					logger.info("No statuses in batch.");
					
					return null;
				}
				//Rate limit exceeded, back off, wait, and try again.
				else if (e.getStatusCode() == 429 || e.getErrorCode() == 88){
					logger.info("Rate limit exceeded");
					
					try {
						//Get the recommended wait time and sleep until then.
						int retryafter = e.getRetryAfter();
						Thread.sleep(retryafter / 1000);
					}
					catch (InterruptedException i) {
						logger.error("Sleeping thread interrupted for some reason ??? aborting.");
						throw new RuntimeException();
					}
					
					//Try again.
					continue;
				}
				else {
					logger.error("Error checking statuses: ", e);
					throw new RuntimeException();
				}
			}
		}
	}
}
