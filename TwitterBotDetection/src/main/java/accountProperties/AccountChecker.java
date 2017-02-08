package accountProperties;

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
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;

public class AccountChecker {
	
	static Logger logger = LogManager.getLogger();
	static ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * Performs a lookup on a set of users by id, to determine whether 
	 * they are accessible, and discards the user if not. Returns a list
	 * of fully hydrated users whose profiles were accessible.
	 * 
	 * TODO: check/get statuses
	 * 
	 * @param user
	 * @return - the List of UserProfiles with inaccessible profiles removed.
	 * @throws TwitterException 
	 */
	public static List<UserProfile> getUsers(Twitter twitter, 
			RedisCommands<String, String> redisApi, List<LabelledUser> users) throws RuntimeException {

		//TODO: add param to fill statuses to the limit for each user, not just relying on those listed in the file.
		
		//Number of statuses per user to limit to, or -1 if no limit.
		int statusLimit = 10;
		
		//Map userId to constructed UserProfile, reduce to list once complete.
		Map<Long, UserProfile> result = new HashMap<Long, UserProfile>();
		
		//##### Deconstruct LabelledUsers #####
		/*
		 * The returned view of users and statuses after "lookup" requests
		 * is a set view. Hence to associate statuses, users and labels we need
		 * mappings.
		 * 
		 * Deconstruct LabelledUsers into mappings for userid -> label and
		 * statusid -> userid to facilitate lookup.
		 */
		
		//Map for userid -> label
		Map<Long, String> mappedUsers = new HashMap<Long, String>();
		//Map for statusid -> userid
		Map<Long, Long> mappedStatuses = new HashMap<Long, Long>();
		
		//Grouped statuses for later batched iteration and retrieval.
		List<Long> allStatuses = new ArrayList<Long>();
		
		//Do the mapping
		users.stream().forEach(
				user -> {
					//mappedUsers i.e. {userId, label}
					mappedUsers.put(user.getUserId(), user.getLabel());
					
					//Sublist user statuses to either statusLimit or take full list.
					List<Long> limitedStatuses;
					if (user.getStatusIds().size() > statusLimit) {
						limitedStatuses = user.getStatusIds().subList(0, statusLimit);
					}
					else {
						limitedStatuses = user.getStatusIds();
					}
					
					//mappedStatuses i.e. {statusId, userId}
					//&& allStatuses i.e. add statuses
					limitedStatuses.forEach(status -> {
						mappedStatuses.put(status, user.getUserId());
						allStatuses.add(status);
					});
				});
		
		//##### Check Cache #####
		logger.info("Checking for cached users...");
		
		//TODO: Consider parallel streams.
		//Check for Users in cache and collect if exists.
		//TODO:Iterate in a safe way.
		List<LabelledUser> notFound = new ArrayList<LabelledUser>();
		
		//If Redis Interface provided...
		if (redisApi != null) {
			//Check for cached users.
			for (LabelledUser user : users) {
				String returned = redisApi.get("user:" + user.getUserId());
				if (returned != null && returned != "null") {
					try {
						//Unmarshell User.
						User returnedUser = TwitterObjectFactory.createUser(returned);
						//Add user to results.
						//TODO: change constructor.
						result.put(user.getUserId(), new UserProfile(mappedUsers.get(user.getUserId()), returnedUser, new ArrayList<Status>()));
					}
					catch (TwitterException e) {
						e.printStackTrace();
						//throw new RuntimeException("Failed to unmarshall User from Redis.");
					}
				}
				else {
					//Requires Twitter API request.
					notFound.add(user);
				}
			}
			
			logger.info("Found {} cached users.", result.size());
		}
		else {
			logger.info("Caching Disabled - No Redis Interface given.");
		}
		
		//##### Non-Cached Users #####
		//Iterate through the users not found in cache and gather from Twitter.
		int index = 0;
		while (index < notFound.size()) {
			List<LabelledUser> toBeProcessed;	//View of sublist of <=100 users
			
			//Take up to 100 users at a time, bounded by size of list.
			if (index+100 < notFound.size()) {
				toBeProcessed = notFound.subList(index, index+100);
				index += 100;
			}
			else {
				toBeProcessed = notFound.subList(index, notFound.size());
				index = notFound.size();
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
				response.stream().forEach(user ->
					{
						result.put(user.getId(), new UserProfile(mappedUsers.get(user.getId()), user, new ArrayList<Status>()));
					});
				
				//If Redis Interface provided...
				//XXX:Re-enable caching.
				//TODO: move inside foreach above.
				if (redisApi != null) {
					for (User user : response) {
						//Add to redis.
						//TODO: Exception on existence of key, should not be in store since earlier check.
						cacheObject(redisApi, user);
					}
				}
			}
			
		}
		
		//TODO: Do not gather for cached users once userProfile caching implemented
		//##### Get the statuses #####
		
		//Retrieve all the statuses from Twitter.
		List<Status> retrievedStatuses = getStatuses(twitter, redisApi, allStatuses);
		
		//Add each status to UserProfile using mapping.
		retrievedStatuses.forEach(status -> {
			//Get the user whom the status belongs to.
			Long userId = mappedStatuses.get(status.getId());
			
			//Add to the UserProfile.
			result.get(userId).addTrainingStatus(status);
		});
		
		//##### Get the UserTimelines #####
		List<UserProfile> results = new ArrayList<UserProfile>(result.values());
		
		results.forEach(user -> lookupUserTimeline(twitter, user));
		
		//Return the reduced list of Users.
		return results;
	}

	/**
	 * Performs a lookup on statuses for a set of users.
	 * 
	 * Attempts to populate the tr
	 * 
	 * @param twitter
	 * @param users
	 * @return
	 */
	public static List<Status> getStatuses(Twitter twitter, RedisCommands<String, String> redisApi, List<Long> statuses) {
		
		List<Status> result = new ArrayList<Status>();
		
		//Check for Statuses in cache and collect if exists.
		//TODO:Iterate in a safe way.
		List<Long> notFound = new ArrayList<Long>();
		
		//If Redis Interface provided...
		if (redisApi != null) {
			logger.info("Checking for cached statuses...");
			
			//Check for cached statuses.
			for (Long status : statuses) {
				String returned = redisApi.get("status:" + status);
				
				//If the key exists.
				if (returned != null) {
					if (returned.compareTo("null") != 0) {
					try {
						//Unmarshell Status.
						Status returnedStatus = TwitterObjectFactory.createStatus(returned);
						//Add user to results.
						result.add(returnedStatus);
						
					}
					catch (TwitterException e) {
						e.printStackTrace();
						//throw new RuntimeException("Failed to unmarshall Status from Redis.");
					}
					}
				}
				else {
					//Requires Twitter API request.
					notFound.add(status);
				}
			}
			
			logger.info("Found {} cached statuses.", result.size());
		}
		else {
			logger.info("Caching Disabled - No Redis Interface given.");
		}
		
		//Save the intermediate value of results to get number before and after twitter queries.
		int cachedStatuses = result.size();
		
		//Iterate through the statuses not found in cache and gather from Twitter.
		int index = 0;
		while (index < notFound.size()) {
			List<Long> toBeProcessed;	//View of sublist of <=Status ids
			
			//Take up to 100 statuses at a time, bounded by size of list.
			if (index+100 < notFound.size()) {
				toBeProcessed = notFound.subList(index, index+100);
				index += 100;
			}
			else {
				toBeProcessed = notFound.subList(index, notFound.size());
				index = notFound.size();
			}
			
			//Get the ids of a subset of statuses to check.
			long[] statusIds = toBeProcessed.stream()
					.mapToLong(Long::longValue)	//Note: need to map to Long object before toArray().
					.toArray();
			
			//Do the lookup.
			ResponseList<Status> response = lookupStatuses(twitter, statusIds);
			
			//Add the results.
			result.addAll(response.stream().collect(Collectors.toList()));
			
			//If Redis Interface provided...
			if (redisApi != null) {
				//Iterate through all statuses, cache object if in results or cache null
				//if not in results.
				//TODO: Simplify/Reduce overhead.
				Map<Long, Status> mappedResponse = response.stream()
						.collect(Collectors.toMap(Status::getId, s -> s));
				
				for(Long statusId : statusIds) {
					Status status = mappedResponse.get(statusId);
					if (status != null) {
						cacheObject(redisApi, status);
					}
					else {
						cacheNullRef(redisApi, "status:"+statusId);
					}
				}
			}
		}
		
		logger.info("Retrieved {} statuses.", (result.size() - cachedStatuses));
		
		//TODO
		//If maxStatuses == true then we iterate through users with non-max statuses and:
		//	1. retrieve timelines 
		//	2. trim to statusLimit
		//	3. cache
		
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
		//TODO: either move batching here or check the size of the array < 100.
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
					logger.info("Rate limit exceeded, waiting...");
					
					try {
						//Get the recommended wait time and sleep until then.
						int retryafter = e.getRetryAfter();
						Thread.sleep(retryafter * 1000);		//Convert from s -> ms
					}
					catch (InterruptedException i) {
						logger.error("Sleeping thread interrupted for some reason ??? aborting.");
						throw new RuntimeException();
					}
					
					//Try again.
					logger.info("Rate limit refreshed, restarting.");
					continue;
				}
				//Twitter overloaded, wait and retry.
				else if (e.getStatusCode() == 503) {
					logger.info("Twitter overloaded, pausing execution...");
					
					try {
						//Get the recommended wait time and sleep until then.
						int delay = 3000;		//Arbitrary delay of 3s
						Thread.sleep(delay);
					}
					catch (InterruptedException i) {
						logger.error("Sleeping thread interrupted for some reason ??? aborting.");
						throw new RuntimeException();
					}
					
					//Try again.
					logger.info("Retrying Twitter service.");
					continue;
				}
				else {
					logger.error("Error checking statuses: ", e);
					throw new RuntimeException();
				}
			}
		}
	}
	
	private static void lookupUserTimeline(Twitter twitter, UserProfile user) {
		
		int statusLimit = 100;
		Paging page = new Paging(1, statusLimit);
		
		//If the user is protected we cannot retrieve their timeline.
		//TODO: Revert to some other method.
		//TODO: investigate application-only authentication parameter for Twitter instance.
		if(user.getUser().isProtected()) {
			logger.info("Protected Timeline for user:{}", user.getUser().getId());
			return;
		}
		
		logger.info("Retrieving UserTimeline for user:{}", user.getUser().getId());
		
		while (true) {
			try {
				ResponseList<Status> response = twitter.getUserTimeline(user.getUser().getId(), page);
				
				//Move to an ArrayList for consistency.
				List<Status> results = new ArrayList<Status>();
				results.addAll(response);
				
				user.setUserTimeline(results);
				
				return;
			} 
			catch (TwitterException e) {
				//Rate limit exceeded, back off, wait, and try again.
				if (e.getStatusCode() == 429 || e.getErrorCode() == 88){
					logger.info("Rate limit exceeded, waiting...");
					
					try {
						//Get the recommended wait time and sleep until then.
						int retryafter = e.getRetryAfter();
						Thread.sleep(retryafter * 1000);		//Convert from s -> ms
					}
					catch (InterruptedException i) {
						logger.error("Sleeping thread interrupted for some reason ??? aborting.");
						throw new RuntimeException();
					}
					
					//Try again.
					logger.info("Rate limit refreshed, restarting.");
					continue;
				}
				//Twitter overloaded, wait and retry.
				else if (e.getStatusCode() == 503) {
					logger.info("Twitter overloaded, pausing execution...");
					
					try {
						//Get the recommended wait time and sleep until then.
						int delay = 3000;		//Arbitrary delay of 3s
						Thread.sleep(delay);
					}
					catch (InterruptedException i) {
						logger.error("Sleeping thread interrupted for some reason ??? aborting.");
						throw new RuntimeException();
					}
					
					//Try again.
					logger.info("Retrying Twitter service.");
					continue;
				}
				else {
					logger.error("Error checking UserTimeline: ", e);
					throw new RuntimeException();
				}
			}
		}
	}

	/**
	 * Cache an object into a Redis server given a Redis API
	 * Interface.
	 * 
	 * @param redisApi
	 * @param o
	 * @throws JsonProcessingException
	 */
	private static void cacheObject(RedisCommands<String, String> redisApi, Object o) {
		//If object is a User...
		if (o instanceof User) {
			User user = (User) o;
			String marshalledUser = TwitterObjectFactory.getRawJSON(user);
			redisApi.set("user:"+user.getId(), marshalledUser);
		}
		//If object is a status...
		else if (o instanceof Status) {
			Status status = (Status) o;
			String marshalledStatus = TwitterObjectFactory.getRawJSON(status);
			redisApi.set("status:"+status.getId(), marshalledStatus);
		}
		//Else don't know how to store.
		else {
			throw new RuntimeException(String.format("Don't know how to store %s object.", o.getClass().getName()));
		}
	}
	
	/**
	 * Cache the String "null" against a given key, to indicate that the object
	 * was not retrievable from Twitter, i.e. it was "looked-up" but not found.
	 * 
	 * @param redisApi - Redis instance API
	 * @param key - the key 
	 */
	private static void cacheNullRef(RedisCommands<String, String> redisApi, String key) {
		//TODO: identify object type automatically as with cacheObject and abstract key concat.
		
		//If we are given a reference as a key for Redis, then store.
		if (key != null) {
			redisApi.set(key, "null");
		}
		else {
			throw new RuntimeException(String.format("Key cannot be null!"));
		}
	}
}
