package accountProperties;

import models.UserAccount;
import models.UserFeatures;
import twitter4j.Twitter;

/**
 * Set of methods to extract features of account properties.
 * @author Adam
 *
 */
public class AccountExtractor {
	
	public static UserFeatures extractFeatures(Twitter twitter, UserAccount user) {
		
		UserFeatures features = new UserFeatures();
		
		features.setId(user.getUser().getId());
		
		//Demographics
		//	Account Health
		//	Screen Name Length
		features.setScreenNameLength(user.getUser().getScreenName().length());
		
		//Content
		//Network
		//	#Following/#Followers
		float ratio = user.getUser().getFriendsCount() / user.getUser().getFollowersCount();
		features.setFollowerRatio(ratio);
		
		//	%Bidirectional friends
		//TODO: Tackle without getting rate limited.
		
		//History
		
		return features;
	}
	
}
