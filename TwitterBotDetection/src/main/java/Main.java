import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import accountProperties.AccountExtractor;
import models.UserAccount;
import models.UserFeatures;
import util.TwitterConfig;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

public class Main {
	
	public static void main(String[] args) {
		
		Logger logger = LogManager.getLogger();
		
		//Get a twitter instance and attempt to authenticate with env variables.
		Twitter twitter = TwitterFactory.getSingleton();
		try {
			TwitterConfig.checkTwitterKeys();
		}
		catch (Exception e){
			logger.error("Couldn't authenticate:", e);
			System.exit(-1);
		}
		
		//Read statuses from file.
		Map<Long, UserAccount> users = StatusFromFile.ReadStatusFile(args[0]);
		
		//Extract account features for each user
		List<UserFeatures> features = new ArrayList<UserFeatures>();
		features = users.entrySet()
				.parallelStream()
				.map(e -> AccountExtractor.extractFeatures(twitter, e.getValue()))
				.collect(Collectors.toList());
		
		logger.info("Extracted account features for {} users", features.size());

	}

}
