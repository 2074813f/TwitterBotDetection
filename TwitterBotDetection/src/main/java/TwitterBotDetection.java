import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.spark.sql.SparkSession;

import accountProperties.AccountChecker;
import features.StatusClassifier;
import models.LabelledUser;
import models.UserProfile;
import util.TwitterConfig;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.User;

public class TwitterBotDetection {
	
	public static void main(String[] args) {
		
		Logger logger = LogManager.getLogger(TwitterBotDetection.class);
		if (logger == null) System.exit(-1);
		
		//Get a twitter instance and attempt to authenticate with env variables.
		Twitter twitter = TwitterConfig.authTwitter();
		
		//Read statuses from file.
		//List<UserProfile> users = DataCapture.readStatusFile(args[0]);
		
		//Remove inaccessible users.
		//users = AccountChecker.filter_accessible(twitter, users);
		//logger.info("Reduced to {} usable users.", users.size());
		
		//FOR LABELLED
		List<LabelledUser> labelledUsers = DataCapture.readLabelledFile(args[0]);
		List<UserProfile> users = AccountChecker.filter_accessible_labelled(twitter, labelledUsers);
		logger.info("Reduced to {} usable users.", users.size());
		
		int bots = 0;
		int humans = 0;
		for (UserProfile user : users) {
			if (user.getLabel() != null && user.getLabel().compareTo("human") == 0) humans++;
			if (user.getLabel() != null && user.getLabel().compareTo("bot") == 0) bots++;
		}
		
		logger.info("Breakdown: {} humans, {} bots", humans, bots);
		
		//Get hydrated statuses and associate with the users.
		List<Status> statuses = AccountChecker.getStatuses(twitter, labelledUsers);
		logger.info("Retrieved {} statuses.", statuses.size());
		
		//TODO: Refactor to avoid this
		Map<Long, UserProfile> mappedUsers = new HashMap<Long, UserProfile>();
		users.stream().forEach(user -> mappedUsers.put(user.getUser().getId(), user));
		
		statuses.stream().forEach(status -> {
			long userid = status.getUser().getId();
			UserProfile user = mappedUsers.get(userid);
			if (user != null) user.addStatus(status);
		});
		
		//Create the spark session.
		SparkSession spark = SparkSession
				.builder()
				.appName("Example")
				.config("spark.master", "local")
				.getOrCreate();
		
		StatusClassifier.trainClassifier(spark, users);
		
		//Extract account features for each user
		//List<UserFeatures> features = new ArrayList<UserFeatures>();
		//features = users.parallelStream()
		//		.map(e -> AccountExtractor.extractFeatures(twitter, e))
		//		.collect(Collectors.toList());
		
		//logger.info("Extracted account features for {} users", features.size());

	}

}
