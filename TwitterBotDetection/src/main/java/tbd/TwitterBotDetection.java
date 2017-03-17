package tbd;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
		
		HttpServer server = GrizzlyHttpServerFactory.createHttpServer(ADDRESS, resourceConfig);
		
		//Serve static resources e.g. Index.html
		server.getServerConfiguration().addHttpHandler(new StaticHttpHandler("src/main/webapp"), "/client");
		
		logger.info("Started server at address: {}", addr);
		return server;
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
		String filename = "src/test/resources/labelled100.txt";
		//String filename = "D:\\Documents\\Uni_Work\\Level4_Project\\CraigsData\\cleangroundtruth.txt";
        //String filename = "D:\\Documents\\Uni_Work\\Level4_Project\\ASONAM_honeypot_data\\ASONAM_honeypot_data\\honeypot.txt";
	
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
			
			List<UserProfile> users = AccountChecker.getUsers(twitter, redisApi, toProcess);
			total += users.size();
			
			//FEATURE EXTRACTION
			//Add all the features for each batch to the full set.
			FeatureExtractor.extractFeatures(users);
			features.addAll(users.stream().map(UserProfile::getFeatures).collect(Collectors.toList()));
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
