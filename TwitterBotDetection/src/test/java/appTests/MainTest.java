package appTests;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.spark.ml.classification.NaiveBayesModel;
import org.apache.spark.sql.SparkSession;
import org.junit.Test;

import accountProperties.AccountChecker;
import features.StatusClassifier;
import models.LabelledUser;
import models.UserProfile;
import tbd.DataCapture;
import twitter4j.Status;
import twitter4j.Twitter;
import util.TwitterConfig;

public class MainTest {
	
	static Twitter twitter = TwitterConfig.authTwitter();
	
	@Test
	public void runApplication() {
		List<LabelledUser> labelledUsers = DataCapture.readLabelledFile("src/test/resources/labelled100.txt");
		assertTrue(labelledUsers.size() == 100);
		
		List<UserProfile> users = AccountChecker.getUsers(twitter, null, labelledUsers);
		
		//Get hydrated statuses and associate with the users.
		List<Status> statuses = AccountChecker.getStatuses(twitter, labelledUsers);
		
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
		
		NaiveBayesModel model = StatusClassifier.trainBayesClassifier(spark, users);
		
	}

}
