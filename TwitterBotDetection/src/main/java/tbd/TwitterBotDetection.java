package tbd;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.spark.ml.classification.RandomForestClassificationModel;
import org.apache.spark.sql.SparkSession;

import com.lambdaworks.redis.api.sync.RedisCommands;

import accountProperties.AccountChecker;
import features.FeatureExtractor;
import features.ProfileClassifier;
import models.LabelledUser;
import models.UserProfile;
import util.RedisConfig;
import util.TwitterConfig;
import twitter4j.Twitter;

public class TwitterBotDetection {
	
	public static void main(String[] args) {
		
		String filename = args[0];
        //XXX: String filename = "/home/adam/labelled10.txt";
	
    	Logger logger = LogManager.getLogger(TwitterBotDetection.class);
		if (logger == null) System.exit(-1);

		//Get a twitter instance and attempt to authenticate with env variables.
		Twitter twitter = TwitterConfig.authTwitter();
		
		//Connect to the Redis server.
		RedisCommands<String, String> redisApi = RedisConfig.startRedis();
		
		//Read statuses from file.
		//List<UserProfile> users = DataCapture.readStatusFile(args[0]);
		
		//Remove inaccessible users.
		//users = AccountChecker.filter_accessible(twitter, users);
		//logger.info("Reduced to {} usable users.", users.size());
		
		//ENTITY COLLECTION
		//FOR LABELLED
		List<LabelledUser> labelledUsers = DataCapture.readLabelledFile(filename);
		List<UserProfile> users = AccountChecker.getUsers(twitter, redisApi, labelledUsers);
		logger.info("Reduced to {} usable users.", users.size());
		
		int bots = 0;
		int humans = 0;
		for (UserProfile user : users) {
			if (user.getLabel() != null && user.getLabel().compareTo("human") == 0) humans++;
			if (user.getLabel() != null && user.getLabel().compareTo("bot") == 0) bots++;
		}
		
		logger.info("Breakdown: {} humans, {} bots", humans, bots);
		
		//Create the spark session.
		SparkSession spark = SparkSession
				.builder()
				.appName("TwitterBotDetection")
				.config("spark.master", "local")
				.getOrCreate();
		
		//FEATURE EXTRACTION
		FeatureExtractor.extractFeatures(users);
		
		//NaiveBayesModel model = StatusClassifier.trainBayesClassifier(spark, users);
		
		RandomForestClassificationModel model = ProfileClassifier.train(spark, users);
		
		//logger.info("Extracted account features for {} users", features.size());
		
		RedisConfig.stopRedis();
	}

}
