package features;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import models.Features;
import models.UserProfile;
import twitter4j.Status;

public class FeatureExtractor {
	
	static Logger logger = LogManager.getLogger();
	
	/**
	 * Populated the feature vector for a list of users.
	 * 
	 * Takes each User in turn and extracts features based on
	 * account information and status metadata.
	 * 
	 * @param users
	 */
	public static void extractFeatures(List<UserProfile> users) {
		
		if (users.isEmpty() || users == null) {
			logger.error("No users given.");
			throw new IllegalArgumentException("Feature extraction requires at least 1 user.");
		}
		
		for (UserProfile user : users) {
			extractFeatures(user);
		}
	}
	
	/**
	 * Populates the feature vector for a given user.
	 * 
	 * Extracts features based on
	 * account information and status metadata.
	 * 
	 * @param users
	 */
	public static void extractFeatures(UserProfile user) {
		Features features = new Features();
		
		//Set the label and id.
		features.setId(user.getUser().getId());
		features.setLabel(user.getLabel());
		
		//##### Profile Features #####
		//Demographics
		//	Account Health
		//	Screen Name Length
		features.setScreenNameLength(nameLength(user));
		features.setIsProtected((user.getUser().isProtected()) == true ? 1.0f : 0.0f);
		features.setIsVerified((user.getUser().isVerified()) == true ? 1.0f : 0.0f);
		
		//Content
		//Network
		//	#Following/#Followers
		features.setFollowerRatio(followerRatio(user));
		
		//History
		//	Account Age / Account Registration Date
		//	Account Verified
		
		//##### Status Features #####
		extractFromStatuses(features, user);
		
		//Set the feature vector in the UserProfile object.
		user.setFeatures(features);
	}
	
	/**
	 * Calculate the screen name length of a User.
	 * 
	 * @param user
	 * @return - the screen name length as an int value.
	 */
	private static int nameLength(UserProfile user) {
		return user.getUser().getScreenName().length();
	}
	
	/**
	 * Calculate the friend/follower ratio of a User.
	 * 
	 * the followerRatio is defined as:
	 * 	followers / followers + friends
	 * 
	 * Where the values for followers have +1 to remove 0 division.
	 * 
	 * @param user
	 * @return - the ratio as a float value.
	 */
	private static float followerRatio(UserProfile user) {
		int friends = user.getUser().getFriendsCount();
		int followers = user.getUser().getFollowersCount() + 1;
		
		float ratio = (float) followers / followers + friends;
		
		return ratio;
	}
	
	/**
	 * Given a features object, populates fields of the feature
	 * object relating to status information.
	 * 
	 * Processes the statuses associated to a given user.
	 * 
	 * @param features
	 * @return
	 */
	private static void extractFromStatuses(Features features, UserProfile user) {
		
		List<Status> statusList = user.getUserTimeline();
		int numStatuses = statusList.size();
		
		//If there are no statuses we cannot do work.
		if (numStatuses == 0) {
			features.setUrlRatio(-1.0F);
			features.setHashtagRatio(-1.0F);
			features.setMentionRatio(-1.0F);
			features.setUniqueDevices(0);
			features.setMainDeviceCount(0);
			features.setMainDevice("NA");
			features.setTweetRate(-1.0F);
			features.setMeanIA(-1.0F);
		}
		//Else process normally.
		else {
			int numStatusesWithURL = 0;
			Map<String, Integer> clientDevices = new HashMap<String, Integer>();
			Map<String, Integer> tweetDates = new HashMap<String, Integer>();
			List<Long> tweetTimes = new ArrayList<Long>();

			int numHashTags = 0;
			int numMentions = 0;
			
			//##### Iterate and get raw features from statuses #####
			for (Status status : statusList) {
				//TODO:#Tweets with URLs / #Tweets
				if (status.getURLEntities().length > 0) numStatusesWithURL++;
				
				//TODO:Link Safety
				
				//TODO:#Total Hashtags in Tweets / #Tweets
				numHashTags += status.getHashtagEntities().length;
				
				//TODO:#Total Mentions in Tweets / #Tweets
				numMentions += status.getUserMentionEntities().length;
				
				//TODO:Client Makeup (Most common)
				//TODO: Consider hashing trick.
				String device = status.getSource();
				if (device != null && !device.equals("")) {
					int count = clientDevices.containsKey(device) ? clientDevices.get(device) : 0;
					clientDevices.put(device, count+1);
				}
				
				//TODO: Temporal
				//TODO: Consider Trimming tail and head days.
				//TODO: Consider more efficient impl.
				Date createdAt = status.getCreatedAt();
				if (createdAt != null) {
					//Add or increment Date.
					String stringRepr = Long.toString(createdAt.getTime());
					int count = tweetDates.containsKey(stringRepr) ? tweetDates.get(stringRepr) : 0;
					tweetDates.put(stringRepr, count + 1);
					
					//Add to list of times.
					tweetTimes.add(createdAt.getTime());
				}
			}
			
			//##### Extract features from raw status info #####
			
			//TODO:set feature fields.
			features.setUrlRatio((float)numStatusesWithURL / numStatuses);
			
			features.setHashtagRatio((float)numHashTags / numStatuses);
			
			features.setMentionRatio((float)numMentions / numStatuses);
			
			//Find the most frequently used device
			int highestCount = -1;
			String highestDevice = "NA";
			
			for (Entry<String, Integer> entry : clientDevices.entrySet()) {
				int currentValue = entry.getValue();
				
				if (currentValue > highestCount) {
					highestCount = currentValue;
					highestDevice = entry.getKey();
				}
			}
			
			features.setMainDevice(highestDevice);
			features.setMainDeviceCount(highestCount);
			features.setUniqueDevices(clientDevices.size());
			
			//TweetRate
			float tweetRate = calcTweetRate(tweetDates);
			features.setTweetRate(tweetRate);
			
			//Get the max tweet rate.
			int maxTweetRate = tweetDates.values().stream().max(Integer::compare).get();
			features.setMaxTweetRate(maxTweetRate);
			
			//Inter-arrival
			List<Long> interArrivals = calcInterArrivals(tweetTimes);
			
			//Mean IA
			//If there is <=1 status then we will get no results for interArrivals and hence cant calc.
			if (!interArrivals.isEmpty()) {
				float meanIA = interArrivals.stream().mapToLong(i -> i).sum() / (float) interArrivals.size();
				features.setMeanIA(meanIA);
			}
			else {
				features.setMeanIA(-1.0F);
			}
		}
	}
	
	/**
	 * Calculates the rate of tweeting/ status posting for a user,
	 * defined as the mean of the number of tweets per day.
	 * 
	 * @param tweetDates - a map of {day, tweet_count}
	 * @return - the rate of tweeting as a float.
	 */
	private static float calcTweetRate(Map<String, Integer> tweetDates) {
		return (tweetDates.values().stream().mapToInt(i -> i).sum() / (float) tweetDates.size());
	}
	
	/**
	 * Calculates the inter-arrival of tweets, i.e. the
	 * time between tweets.
	 * 
	 * @param tweetDates - a map of {day, tweet_count}
	 * @return - a list with the inter-arrival for each pair of tweets
	 */
	private static List<Long> calcInterArrivals(List<Long> tweetTimes) {
		
		List<Long> result = new ArrayList<Long>();
		
		for (int i=0; i<(tweetTimes.size() - 1); i++) {
			long first = tweetTimes.get(i);
			long second = tweetTimes.get(i+1);
			
			long interArrival = first - second;
			
			result.add(interArrival);
		}
		
		return result;
	}

}
