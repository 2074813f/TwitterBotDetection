package appTests;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.spark.ml.classification.NaiveBayesModel;
import org.apache.spark.sql.SparkSession;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.lambdaworks.redis.api.sync.RedisCommands;

import accountProperties.AccountChecker;
import features.StatusClassifier;
import models.LabelledUser;
import models.UserProfile;
import tbd.DataCapture;
import twitter4j.Status;
import twitter4j.Twitter;
import util.RedisConfig;
import util.TwitterConfig;

@Deprecated
public class MainTest {
	
	static Twitter twitter;
	
	@Ignore
	@Before
	public void setUp() {
		twitter = TwitterConfig.authTwitter();
	}
	
	@Ignore
	@Test
	public void runApplication() {
		List<LabelledUser> labelledUsers = DataCapture.readLabelledFile("src/test/resources/labelled10.txt");
		assertTrue(labelledUsers.size() == 10);
		
		List<UserProfile> users = AccountChecker.getUsers(twitter, null, labelledUsers);
		
		//Create the spark session.
		SparkSession spark = SparkSession
				.builder()
				.appName("Example")
				.config("spark.master", "local")
				.getOrCreate();
		
		NaiveBayesModel model = StatusClassifier.trainBayesClassifier(spark, users);
		
	}
}
