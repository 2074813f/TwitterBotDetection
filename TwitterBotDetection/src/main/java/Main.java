import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import accountProperties.AccountExtractor;
import models.UserAccount;
import models.UserFeatures;
import util.TwitterConfig;
import twitter4j.Twitter;

public class Main {
	
	public static void main(String[] args) {
		
		Logger logger = LogManager.getLogger();
		if (logger == null) System.exit(-1);
		
		//Get a twitter instance and attempt to authenticate with env variables.
		Twitter twitter = TwitterConfig.authTwitter();
		
		//Read statuses from file.
		List<UserAccount> users = DataCapture.ReadStatusFile(args[0]);
		
		//Iterate over UserAccounts and grab timeline from twitter.
		users.parallelStream()
				.forEach(user -> DataCapture.AddTimeline(twitter, user));
		
		//Persist gathered timeline information to file
		//TODO: change filename input either to runtime param or configured from base filename.
		DataCapture.WriteUserAccountFile(users, "test_output.json");
		
		//Extract account features for each user
		List<UserFeatures> features = new ArrayList<UserFeatures>();
		features = users.parallelStream()
				.map(e -> AccountExtractor.extractFeatures(twitter, e))
				.collect(Collectors.toList());
		
		logger.info("Extracted account features for {} users", features.size());

	}

}
