package features;

import models.UserProfile;
import tbd.TwitterBotDetection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.RandomForestClassificationModel;
import org.apache.spark.ml.classification.RandomForestClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.IndexToString;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.StringIndexerModel;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.feature.VectorIndexer;
import org.apache.spark.ml.feature.VectorIndexerModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import static org.apache.spark.sql.types.DataTypes.*;

import models.LabelledStatus;
import models.Features;
import twitter4j.Twitter;
import twitter4j.User;

/**
 * Set of methods to extract features of account properties.
 * @author Adam
 *
 */
public class ProfileClassifier {
	
	static Logger logger = LogManager.getLogger(TwitterBotDetection.class);
	
//	public static List<Features> extractFeatures(List<UserProfile> users) {
//		
//		if (users.isEmpty()) {
//			logger.error("No training data.");
//			return null;
//		}
//		
//		List<Features> features = new ArrayList<Features>();
//		
//		for (UserProfile user : users) {
//			
//			Features uf = new Features();
//			
//			//Set the label.
//			//Human = 0.0, Bot = 1.0
//			if (user.getLabel().compareTo("human") == 0) {
//				uf.setLabel(0.0);
//			}
//			else {
//				uf.setLabel(1.0);
//			}
//			
//			//Demographics
//			//	Account Health
//			//	Screen Name Length
//			uf.setScreenNameLength(user.getUser().getScreenName().length());
//			
//			//Content
//			//Network
//			//	#Following/#Followers
//			float ratio = (float)user.getUser().getFriendsCount() / user.getUser().getFollowersCount();
//			uf.setFollowerRatio(ratio);
//			
//			//	%Bidirectional friends
//			//TODO: Tackle without getting rate limited.
//			
//			//History
//			
//			//Add the user features to the collection.
//			features.add(uf);
//		}
//		
//		return features;
//	}
	
	public static RandomForestClassificationModel train(SparkSession spark, List<UserProfile> users) {
		//Collect the features from all users.
		List<Features> features = users.stream().map(UserProfile::getFeatures).collect(Collectors.toList());
		
		logger.debug("Produced {} sets of features from {} users.", features.size(), users.size());
		
		RandomForestClassificationModel model = trainRFClassifier(spark, features);
		return model;
	}
	
	public static RandomForestClassificationModel trainRFClassifier(SparkSession spark, List<Features> features) {
		
		/*
		 * Convert UserFeatures objects to Dataset<Row>, transforming features to
		 * a Spark Vector.
		 */
		Encoder<Features> featuresEncoder = Encoders.bean(Features.class);
		Dataset<Features> rawData = spark.createDataset(features, featuresEncoder);
		rawData.show();
		
		VectorAssembler assembler = new VectorAssembler()
				.setInputCols(new String[]{"screenNameLength", "followerRatio", "urlRatio", "hashtagRatio", "mentionRatio"})
				.setOutputCol("features");
		Dataset<Row> data = assembler.transform(rawData);
		data.show();
	
		// Index labels, adding metadata to the label column.
		// Fit on whole dataset to include all labels in index.
		StringIndexerModel labelIndexer = new StringIndexer()
		  .setInputCol("label")
		  .setOutputCol("indexedLabel")
		  .fit(data);
		
		//XXX: NOTE: we don't have any categorical features thus far.
		//     see:https://en.wikipedia.org/wiki/Categorical_variable
		//	   (Essentially an enumerable variable).
		// Automatically identify categorical features, and index them.
		// Set maxCategories so features with > 4 distinct values are treated as continuous.
		VectorIndexerModel featureIndexer = new VectorIndexer()
		  .setInputCol("features")
		  .setOutputCol("indexedFeatures")
		  .setMaxCategories(4)
		  .fit(data);
	
		// Split the data into training and test sets (30% held out for testing)
		Dataset<Row>[] splits = data.randomSplit(new double[] {0.7, 0.3});
		Dataset<Row> trainingData = splits[0];
		Dataset<Row> testData = splits[1];
	
		// Train a RandomForest model.
		RandomForestClassifier rf = new RandomForestClassifier()
		  .setLabelCol("indexedLabel")
		  .setFeaturesCol("indexedFeatures");
	
		// Convert indexed labels back to original labels.
		IndexToString labelConverter = new IndexToString()
		  .setInputCol("prediction")
		  .setOutputCol("predictedLabel")
		  .setLabels(labelIndexer.labels());
	
		// Chain indexers and forest in a Pipeline
		Pipeline pipeline = new Pipeline()
		  .setStages(new PipelineStage[] {labelIndexer, featureIndexer, rf, labelConverter});
	
		// Train model. This also runs the indexers.
		PipelineModel model = pipeline.fit(trainingData);
	
		// Make predictions.
		Dataset<Row> predictions = model.transform(testData);
	
		// Select example rows to display.
		predictions.select("predictedLabel", "label", "features").show(5);
	
		// Select (prediction, true label) and compute test error
		MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
		  .setLabelCol("indexedLabel")
		  .setPredictionCol("prediction")
		  .setMetricName("accuracy");
		
		double accuracy = evaluator.evaluate(predictions);
		logger.info("Test Error = {}", (1.0 - accuracy));
	
		RandomForestClassificationModel rfModel = (RandomForestClassificationModel)(model.stages()[2]);
		logger.info("Learned classification forest model: {}\n", rfModel.toDebugString());
		
		return rfModel;
	}
	
}
