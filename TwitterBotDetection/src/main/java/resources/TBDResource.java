package resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.classification.RandomForestClassificationModel;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.lambdaworks.redis.api.sync.RedisCommands;

import accountProperties.AccountChecker;
import features.FeatureExtractor;
import features.ProfileClassifier;
import models.Features;
import models.LabelledUser;
import models.UserProfile;
import serializer.UserProfileObjectMapper;
import tbd.DataCapture;
import tbd.TwitterBotDetection;
import twitter4j.Twitter;
import util.RedisConfig;
import util.TwitterConfig;

/**
 * Resource class for entitites required in TBDService.
 * 
 * Builds classifier and exposes it, as well as a redis
 * interface to the user.
 * 
 * @author Adam
 *
 */
public class TBDResource {
	final static Logger logger = LogManager.getLogger(TwitterBotDetection.class.getName());
	static UserProfileObjectMapper mapper = new UserProfileObjectMapper();
	
	Twitter twitter;
	RedisCommands<String, String> redisApi;
	SparkSession spark;
	
	private RandomForestClassificationModel model;
	private PipelineModel pmodel;
	
	public TBDResource() {
		//Get interfaces.
		twitter = TwitterBotDetection.twitter;
		redisApi = TwitterBotDetection.redisApi;
		spark = TwitterBotDetection.spark;
		
		//Load the models.
		model = RandomForestClassificationModel.load("tmp/model");
		pmodel = PipelineModel.load("tmp/pmodel");
	}
	
	public Result queryModel(long userid) {
		//Get the user.
		UserProfile user = AccountChecker.getUser(twitter, redisApi, userid);
		user.setLabel("human");	//XXX
		FeatureExtractor.extractFeatures(user);
		
		//Extract user features.
		logger.info("Extracting features for user: {}", userid);
		List<Features> features = new ArrayList<Features>();
		features.add(user.getFeatures());
		
		/*
		 * Convert UserFeatures objects to Dataset<Row>, transforming features to
		 * a Spark Vector.
		 */
		logger.info("Encoding features for user: {}", userid);
		Encoder<Features> featuresEncoder = Encoders.bean(Features.class);
		Dataset<Features> rawData = spark.createDataset(features, featuresEncoder);
		
		//Transform the row to predict to a vector.
		logger.info("Transforming data for user: {}", userid);
		Dataset<Row> cleanData = pmodel.transform(rawData);
		Vector featureVector = cleanData.select("features").first().getAs("features");
		
		//Transform double -> String result, i.e. re-label.
		Result result = new Result();
		result.setLabel(model.predict(featureVector) == 0.0 ? "human" : "bot");
		result.setFeatures(features.get(0));
		result.setProbability(model.getImpurity());
		
		logger.info("Prediction for user={} : {}", userid, result.getLabel());
		return result;
	}
	
	public void classifyUser(long userid, String label) throws JsonParseException, JsonMappingException, IOException {
		//Check if user exists already.
		String key = "userprofile:"+userid;
		String marshalleduser = redisApi.get(key);
		UserProfile user;
		
		//If exists, retrieve, update value, and store.
		if (marshalleduser != null) {
			user = mapper.readValue(marshalleduser, UserProfile.class);
			user.setLabel(label);
		}
		//Else retrieve, build and set.
		else {
			user = AccountChecker.getUser(twitter, redisApi, userid);
			FeatureExtractor.extractFeatures(user);
			user.setLabel(label);
		}
		
		String updated = mapper.writeValueAsString(user);
		redisApi.set(key, updated);
	}
}
