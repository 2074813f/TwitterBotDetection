package resources;

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

import com.lambdaworks.redis.api.sync.RedisCommands;

import accountProperties.AccountChecker;
import features.FeatureExtractor;
import features.ProfileClassifier;
import models.Features;
import models.LabelledUser;
import models.UserProfile;
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
		model = RandomForestClassificationModel.load("src/main/resources/tmp/model");
		pmodel = PipelineModel.load("src/main/resources/tmp/pmodel");
	}
	
	public String queryModel(long userid) {
		UserProfile user = AccountChecker.getUser(twitter, redisApi, userid);
		user.setLabel("human");	//XXX
		FeatureExtractor.extractFeatures(user);
		
		//TODO: Transform the userid -> feature vector.
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
		logger.info("Cleaning data for user: {}", userid);
		Dataset<Row> cleanData = pmodel.transform(rawData);
		Vector featureVector = cleanData.select("features").first().getAs("features");
		
		//Transform double -> String result, i.e. re-label.
		String result = model.predict(featureVector) == 0.0 ? "human" : "bot";
		
		logger.info("Prediction for user={} : {}", userid, result);
		return result;
	}
	
	public RandomForestClassificationModel getModel() {
		return model;
	}
	public void setModel(RandomForestClassificationModel model) {
		this.model = model;
	}
}
