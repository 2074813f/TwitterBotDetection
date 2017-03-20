package tbd;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.classification.RandomForestClassificationModel;
import org.apache.spark.sql.SparkSession;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lambdaworks.redis.api.sync.RedisCommands;

import accountProperties.AccountChecker;
import caching.RedisCacher;
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
		
		HttpServer server = GrizzlyHttpServerFactory.createHttpServer(ADDRESS, resourceConfig);
		
		//Serve static resources e.g. Index.html
		server.getServerConfiguration().addHttpHandler(new StaticHttpHandler("src/main/webapp"), "/client");
		
		logger.info("Started server at address: {}", addr);
		return server;
	}

	public static void main(String[] args) throws IOException {
		
		//TODO: consider registering hook: https://github.com/jersey/jersey/blob/master/examples/jaxb/src/main/java/org/glassfish/jersey/examples/jaxb/App.java
		String pathToProperties = "src/main/resources/config.properties";
		
		//Get the properties file.
		Properties props = new Properties();
		FileInputStream in = new FileInputStream(pathToProperties);
		props.load(in);
		in.close();
		logger.info("Loaded properties file: {}", pathToProperties);
		
		//Build the models.
		buildModels(props);
		
		HttpServer server = startServer();
		logger.info("Press any key to exit...");
		System.in.read();
		server.shutdownNow();
	}
	
	public static void buildModels(Properties props) throws IOException {
		//Build the classifier at construction time.

		String filename = props.getProperty("groundTruth");
		
		if (logger == null) System.exit(-1);

		//Get a twitter instance and attempt to authenticate with env variables.
		twitter = TwitterConfig.authTwitter();
		
		//Connect to the Redis server.
		redisApi = RedisConfig.startRedis();
		
		//Create new cacher.
		RedisCacher cacher = new RedisCacher(redisApi);
		
		//ENTITY COLLECTION
		//FOR LABELLED
		List<LabelledUser> labelledUsers = DataCapture.readLabelledFile(filename);
		
		List<Features> features = new ArrayList<Features>();
		
		int index = 0;
		int total = 0;
		while (index < labelledUsers.size()) {
			List<LabelledUser> toProcess;
			
			//Take up to 100 users at a time, bounded by size of list.
			if (index+100 < labelledUsers.size()) {
				toProcess = labelledUsers.subList(index, index+100);
				index += 100;
			}
			else {
				toProcess = labelledUsers.subList(index, labelledUsers.size());
				index = labelledUsers.size();
			}
			
			List<UserProfile> users = AccountChecker.getUsers(twitter, redisApi, cacher, toProcess);
			total += users.size();
			
			//FEATURE EXTRACTION
			//Get features for cached users.
			List<UserProfile> requireExtraction = new ArrayList<UserProfile>();
			for (UserProfile user : users) {
				//If user not cached then add to the collection for extraction.
				if (user.getFeatures() == null) {
					requireExtraction.add(user);
				}
				//Else get features directly.
				else {
					features.add(user.getFeatures());
				}
			}
			
			//Add all the features for each batch to the full set.
			if (!requireExtraction.isEmpty()) {
				FeatureExtractor.extractFeatures(requireExtraction);
				features.addAll(requireExtraction.stream().map(UserProfile::getFeatures).collect(Collectors.toList()));
				
				logger.info("Extracted features for {} UserProfiles.", requireExtraction.size());
			}
			
//			//##### Cache the new Features #####
//			for (UserProfile userprofile : requireExtraction) {
//				//Add to redis.
//				try {
//					cacher.cacheObject(userprofile);
//				} catch (JsonProcessingException e) {
//					logger.error("Failed to cache UserProfile object.");
//					e.printStackTrace();
//				}
//			}
		}
		
		//TODO: Change to each batch.
		logger.info("Finished Collecting Raw Data && Feature Extraction.");
		logger.debug("Produced {} sets of features from {} users.", features.size(), total);
		
		logger.info("Creating Spark Session.");
		
		//Create the spark session.
		spark = SparkSession
				.builder()
				.appName("TwitterBotDetection")
				.config("spark.master", "local")
				.getOrCreate();
		
		//NaiveBayesModel model = StatusClassifier.trainBayesClassifier(spark, users);
		
		//Set the model here.
		RandomForestClassificationModel model = ProfileClassifier.train(spark, features);
		PipelineModel pmodel = ProfileClassifier.dataExtractor(spark, features);
		
		//Save the models to file.
		model.write().overwrite().save("tmp/model");
		pmodel.write().overwrite().save("tmp/pmodel");
	}
}
