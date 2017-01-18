package features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import models.Features;
import models.LabelledStatus;
import models.UserProfile;
import twitter4j.Status;
import twitter4j.URLEntity;

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
			
			Features features = new Features();
			
			//TODO: Translate labels at time of classification
			//Set the label.
			//Human = 0.0, Bot = 1.0
			if (user.getLabel().compareTo("human") == 0) {
				features.setLabel(0.0);
			}
			else {
				features.setLabel(1.0);
			}
			
			//##### Profile Features #####
			//Demographics
			//	Account Health
			//	Screen Name Length
			features.setScreenNameLength(nameLength(user));
			
			//Content
			//Network
			//	#Following/#Followers
			features.setFollowerRatio(followerRatio(user));
			
			//	%Bidirectional friends
			//TODO: Tackle without getting rate limited.
			
			//History
			//	Account Age / Account Registration Date
			//	Account Verified
			
			//##### Status Features #####
			extractFromStatuses(features, user);
			
			//Set the feature vector in the UserProfile object.
			user.setFeatures(features);
		}
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
	 * @param user
	 * @return - the ratio as a float value.
	 */
	private static float followerRatio(UserProfile user) {
		return (float)user.getUser().getFriendsCount() / user.getUser().getFollowersCount();
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
		
		int numStatuses = user.getStatuses().size();
		
		if (numStatuses == 0) {
			features.setUrlRatio(-1.0F);
			features.setHashtagRatio(-1.0F);
			features.setMentionRatio(-1.0F);
			return;
		};
		
		int numStatusesWithURL = 0;
		Map<String, Integer> clientDevices = new HashMap<String, Integer>();
		boolean linkSafety = true;
		int numHashTags = 0;
		int numMentions = 0;
		
		for (Status status : user.getStatuses()) {
			//TODO:#Tweets with URLs / #Tweets
			if (status.getURLEntities().length > 0) numStatusesWithURL++;
			
			//TODO:Link Safety
			
			//TODO:#Total Hashtags in Tweets / #Tweets
			numHashTags += status.getHashtagEntities().length;
			
			//TODO:#Total Mentions in Tweets / #Tweets
			numMentions += status.getUserMentionEntities().length;
			
			//TODO:Client Makeup (Most common)
			String device = status.getSource();
			int count = clientDevices.containsKey(device) ? clientDevices.get(device) : 0;
			clientDevices.put(device, count);
		}
		
		//TODO:set feature fields.
		features.setUrlRatio((float)numStatusesWithURL / numStatuses);
		
		features.setHashtagRatio((float)numHashTags / numStatuses);
		
		features.setMentionRatio((float)numMentions / numStatuses);
		
//		clientDevices.entrySet().stream().max((entry1, entry2) -> 
//		Integer.compare(entry1.getValue(), entry2.getValue())).getKey();
	}

}
