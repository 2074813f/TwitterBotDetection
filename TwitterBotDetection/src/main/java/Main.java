import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import accountProperties.AccountChecker;
import models.LabelledUser;
import models.UserProfile;
import util.TwitterConfig;
import twitter4j.Twitter;

public class Main {
	
	public static void main(String[] args) {
		
		Logger logger = LogManager.getLogger();
		if (logger == null) System.exit(-1);
		
		//Get a twitter instance and attempt to authenticate with env variables.
		Twitter twitter = TwitterConfig.authTwitter();
		
		//Read statuses from file.
		//List<UserProfile> users = DataCapture.readStatusFile(args[0]);
		
		//Remove inaccessible users.
		//users = AccountChecker.filter_accessible(twitter, users);
		//logger.info("Reduced to {} usable users.", users.size());
		
		//FOR LABELLED
		List<LabelledUser> classedUsers = DataCapture.readClassifiedFile(args[0]);
		List<UserProfile> users = AccountChecker.filter_accessible_labelled(twitter, classedUsers);
		logger.info("Reduced to {} usable users.", users.size());
		
		//Get the timelines for each user from twitter.
		//users.parallelStream()
		//		.forEach(user -> DataCapture.AddTimeline(twitter, user));
		
		//Persist gathered timeline information to file
		//TODO: change filename input either to runtime param or configured from base filename.
		//DataCapture.WriteUserAccountFile(users, "test_output.json");
		
		//Extract account features for each user
		//List<UserFeatures> features = new ArrayList<UserFeatures>();
		//features = users.parallelStream()
		//		.map(e -> AccountExtractor.extractFeatures(twitter, e))
		//		.collect(Collectors.toList());
		
		//logger.info("Extracted account features for {} users", features.size());

	}

}
