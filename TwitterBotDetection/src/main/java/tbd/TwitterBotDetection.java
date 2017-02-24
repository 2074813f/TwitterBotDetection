package tbd;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.classification.RandomForestClassificationModel;
import org.apache.spark.sql.SparkSession;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.lambdaworks.redis.api.sync.RedisCommands;

import accountProperties.AccountChecker;
import features.FeatureExtractor;
import features.ProfileClassifier;
import models.Features;
import models.LabelledUser;
import models.UserProfile;
import util.RedisConfig;
import util.TwitterConfig;
import twitter4j.Twitter;

public class TwitterBotDetection {
	final static Logger logger = LogManager.getLogger(TwitterBotDetection.class.getName());
	
	private static String addr = "http://localhost:8080";
	private static final URI ADDRESS = UriBuilder.fromPath(addr).build();
	
	public static Twitter twitter;
	public static RedisCommands<String, String> redisApi;
	public static SparkSession spark;
	
	public static HttpServer startServer() {
		logger.info("Starting server...");
		
		//Configure REST service
		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.packages("resources");
		resourceConfig.register(JacksonFeature.class);
		
		return GrizzlyHttpServerFactory.createHttpServer(ADDRESS, resourceConfig);
		
		//logger.info("Started server at address: {}", addr);
	}

	public static void main(String[] args) throws IOException {
		
		//TODO: consider registering hook: https://github.com/jersey/jersey/blob/master/examples/jaxb/src/main/java/org/glassfish/jersey/examples/jaxb/App.java
		
		//Build the models.
		buildModels();
		
		HttpServer server = startServer();
		logger.info("Press any key to exit...");
		System.in.read();
		server.shutdownNow();
	}
	
	public static void buildModels() throws IOException {
		//Build the classifier at construction time.
		
		//TODO: move to properties file.
		//String filename = args[0];
		String filename = "src/test/resources/labelled100.txt";
        //XXX: String filename = "/home/adam/labelled10.txt";
	
		if (logger == null) System.exit(-1);

		//Get a twitter instance and attempt to authenticate with env variables.
		twitter = TwitterConfig.authTwitter();
		
		//Connect to the Redis server.
		redisApi = RedisConfig.startRedis();
		
		//Read statuses from file.
		//List<UserProfile> users = DataCapture.readStatusFile(args[0]);
		
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
		spark = SparkSession
				.builder()
				.appName("TwitterBotDetection")
				.config("spark.master", "local")
				.getOrCreate();
		
		//FEATURE EXTRACTION
		FeatureExtractor.extractFeatures(users);
		List<Features> features = users.stream().map(UserProfile::getFeatures).collect(Collectors.toList());
		
		//NaiveBayesModel model = StatusClassifier.trainBayesClassifier(spark, users);
		
		//Set the model here.
		RandomForestClassificationModel model = ProfileClassifier.train(spark, users);
		PipelineModel pmodel = ProfileClassifier.dataExtractor(spark, features);
		
		//Save the models to file.
		model.write().overwrite().save("tmp/model");
		pmodel.write().overwrite().save("tmp/pmodel");
	}
}
