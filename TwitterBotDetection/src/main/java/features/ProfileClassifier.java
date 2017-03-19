package features;

import tbd.TwitterBotDetection;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.RandomForestClassificationModel;
import org.apache.spark.ml.classification.RandomForestClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.IndexToString;
import org.apache.spark.ml.feature.OneHotEncoder;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.StringIndexerModel;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.feature.VectorIndexer;
import org.apache.spark.ml.feature.VectorIndexerModel;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.ml.tuning.CrossValidator;
import org.apache.spark.ml.tuning.CrossValidatorModel;
import org.apache.spark.ml.tuning.ParamGridBuilder;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import models.Features;

/**
 * Set of methods to extract features of account properties.
 * @author Adam
 *
 */
public class ProfileClassifier {
	//TODO: Convert features to builder pattern
	//TODO: allow configuration of classifier e.g.CrossValidate or not.
	
	static Logger logger = LogManager.getLogger(TwitterBotDetection.class);
	
	static String[] rawFeatures = new String[]{"isProtected", "isVerified", "meanIA", "tweetRate", "maxTweetRate", "screenNameLength", "followerRatio", "urlRatio", "hashtagRatio", "mentionRatio", "uniqueDevices", "mainDeviceCount", "indexedMainDevice"};
	
	public static RandomForestClassificationModel train(SparkSession spark, List<Features> userFeatures) {

		RandomForestClassificationModel model = trainRFClassifier(spark, userFeatures);
		return model;
	}
	
	public static RandomForestClassificationModel trainRFClassifier(SparkSession spark, List<Features> features) {
		
		long seed = 207325481L;
		logger.info("Training with seed: {}", seed);
		Long datetime = new Date().getTime();
		
		/*
		 * Convert UserFeatures objects to Dataset<Row>, transforming features to
		 * a Spark Vector.
		 */
		Encoder<Features> featuresEncoder = Encoders.bean(Features.class);
		Dataset<Features> rawData = spark.createDataset(features, featuresEncoder);
		
//		Map<String, String> mapping = new HashMap<String, String>();
//		mapping.put("", "NA");
//		rawData.na().replace("MainDevice", mapping);
		
		// Index the mainDevice column.
		StringIndexerModel sourceIndexer = new StringIndexer()
			.setInputCol("mainDevice")
			.setOutputCol("indexedMainDevice")
			.fit(rawData);
		Dataset<Row> indexedData = sourceIndexer.transform(rawData);
		indexedData.show();
		
		//Write the feature table to disk.
		String featurePath = String.format("tmp/results/%s/features", datetime);
		indexedData.select("id", "label", "tweetRate", "maxTweetRate", "screenNameLength", "followerRatio", "urlRatio", "hashtagRatio", "mentionRatio", "uniqueDevices", "mainDeviceCount", "mainDevice", "indexedMainDevice")
			.write()
			.option("header", "true")
			.csv(featurePath);
		
		//Convert label indices -> vectors to reduce bins
//		OneHotEncoder encoder = new OneHotEncoder()
//				.setInputCol("indexedMainDevice")
//				.setOutputCol("mainDeviceVec");
//		indexedData = encoder.transform(indexedData);
		
		// Assemble the features into a vector for classification.
		VectorAssembler assembler = new VectorAssembler()
				.setInputCols(rawFeatures)
				.setOutputCol("features");
		Dataset<Row> data = assembler.transform(indexedData);
	
		// Index labels, adding metadata to the label column.
		// Fit on whole dataset to include all labels in index.
		StringIndexerModel labelIndexer = new StringIndexer()
		  .setInputCol("label")
		  .setOutputCol("indexedLabel")
		  .fit(data);
		
		// see:https://en.wikipedia.org/wiki/Categorical_variable
		//	   (Essentially an enumerable variable).
		// Automatically identify categorical features, and index them.
		// Set maxCategories so features with > 4 distinct values are treated as continuous.
		VectorIndexerModel featureIndexer = new VectorIndexer()
		  .setInputCol("features")
		  .setOutputCol("indexedFeatures")
		  .setMaxCategories(4)
		  .fit(data);
//		Dataset<Row> test = featureIndexer.transform(data);
//		test.select("indexedFeatures").show(false);
	
		// Split the data into training and test sets (40% held out for testing)
		Dataset<Row>[] splits = data.randomSplit(new double[] {0.6, 0.4}, seed);
		Dataset<Row> trainingData = splits[0];
		Dataset<Row> testData = splits[1];
	
		// Train a RandomForest model.
		//NOTE: need to increase the maximum number of bins for larger datasets for device types.
		RandomForestClassifier rf = new RandomForestClassifier()
		  .setLabelCol("indexedLabel")
		  .setFeaturesCol("indexedFeatures")
		  .setSeed(seed)
		  .setMaxBins(280);
	
		// Convert indexed labels back to original labels.
		IndexToString labelConverter = new IndexToString()
		  .setInputCol("prediction")
		  .setOutputCol("predictedLabel")
		  .setLabels(labelIndexer.labels());
	
		// Chain indexers and forest in a Pipeline
		Pipeline pipeline = new Pipeline()
		  .setStages(new PipelineStage[] {labelIndexer, featureIndexer, rf, labelConverter});
		
		// Select (prediction, true label) and compute test error
		MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
		  .setLabelCol("indexedLabel")
		  .setPredictionCol("prediction")
		  .setMetricName("accuracy");
		
		/*
		 * Tune 2 choices for numTrees, 3 for maxDepth, giving 2 x 3 grid = 
		 * hence 6 parameter settings for CrossValidator to choose from.
		 */
		ParamMap[] paramGrid = new ParamGridBuilder()
				.addGrid(rf.numTrees(), new int[] {10, 100})
				.addGrid(rf.maxDepth(), new int[] {rf.getMaxDepth(), 5, 10})
				//TODO: Min info gain.
				.build();
		
		CrossValidator cv = new CrossValidator()
				.setEstimator(pipeline)
				.setEvaluator(evaluator)
				.setEstimatorParamMaps(paramGrid).setNumFolds(4);
		
		CrossValidatorModel model = cv.fit(trainingData);
		
//		// Train model. This also runs the indexers.
//		PipelineModel model = pipeline.fit(trainingData);
	
		// Make predictions.
		Dataset<Row> predictions = model.transform(testData);
	
		// Select example rows to display.
		//XXX: select rows where we incorrectly classify
//		Dataset<Row> results = predictions.select("id", "predictedLabel", "label", "mainDevice");
//		//results.show(false);
		
		//Write results of evaluation to disk.
		String resultPath = String.format("tmp/results/%s/results/", datetime);
		predictions.select("id", "predictedLabel", "label")
			.write()
			.option("header", "true")
			.csv(resultPath);
		
		double accuracy = evaluator.evaluate(predictions);
		logger.debug("Test Error = {}", (1.0 - accuracy));
		
		Dataset<Row> predictionAndLabels = predictions.select("prediction", "indexedLabel");
		MulticlassMetrics metrics = new MulticlassMetrics(predictionAndLabels);
		
		logger.debug("##### Training Statistics #####");
		logger.debug("Accuracy: {}", metrics.accuracy());
		logger.debug("Confusion Matrix: \n{}", metrics.confusionMatrix().toString());
		
//		RandomForestClassificationModel rfModel = (RandomForestClassificationModel)(model.stages()[2]);
		PipelineModel plModel = (PipelineModel)model.bestModel();
		RandomForestClassificationModel rfModel = (RandomForestClassificationModel)plModel.stages()[2];
		
		logger.debug("Best numTrees: {}", rfModel.getNumTrees());
		logger.debug("Best maxDepth: {}", rfModel.getMaxDepth());
		
		logger.debug("Feature importance: {}", rfModel.featureImportances());
		//logger.info("Learned classification forest model: {}\n", rfModel.toDebugString());
		
		return rfModel;
	}
	
	public static PipelineModel dataExtractor(SparkSession spark, List<Features> features) {
		
		/*
		 * Convert UserFeatures objects to Dataset<Row>, transforming features to
		 * a Spark Vector.
		 */
		Encoder<Features> featuresEncoder = Encoders.bean(Features.class);
		Dataset<Features> rawData = spark.createDataset(features, featuresEncoder);
		
		//Show the non-truncated devices for debugging.
		//rawData.select("mainDevice").show(20, false);
		
		//TODO: Should drop elements where we have no statuses before train..() called?
		//rawData = rawData.filter(rawData.col("urlRatio").gt(0));
		
		// Index the mainDevice column.
		StringIndexer sourceIndexer = new StringIndexer()
			.setInputCol("mainDevice")
			.setOutputCol("indexedMainDevice");
		//	.fit(rawData);
		//Dataset<Row> indexedData = sourceIndexer.transform(rawData);
	
		// Assemble the features into a vector for classification.
		VectorAssembler assembler = new VectorAssembler()
				.setInputCols(rawFeatures)
				.setOutputCol("features");
	
		// Index labels, adding metadata to the label column.
		// Fit on whole dataset to include all labels in index.
		StringIndexer labelIndexer = new StringIndexer()
		  .setInputCol("label")
		  .setOutputCol("indexedLabel");
		
		// see:https://en.wikipedia.org/wiki/Categorical_variable
		//	   (Essentially an enumerable variable).
		// Automatically identify categorical features, and index them.
		// Set maxCategories so features with > 4 distinct values are treated as continuous.
		VectorIndexer featureIndexer = new VectorIndexer()
		  .setInputCol("features")
		  .setOutputCol("indexedFeatures")
		  .setMaxCategories(4);
//		Dataset<Row> test = featureIndexer.transform(data);
//		test.select("indexedFeatures").show(false);
	
		// Convert indexed labels back to original labels.
//		IndexToString labelConverter = new IndexToString()
//		  .setInputCol("prediction")
//		  .setOutputCol("predictedLabel")
//		  .setLabels(labelIndexer.labels());
	
		// Chain indexers and forest in a Pipeline
		PipelineModel pipeline = new Pipeline()
		  .setStages(new PipelineStage[] {sourceIndexer, assembler, labelIndexer, featureIndexer})
		  .fit(rawData);

		return pipeline;
	}
	
}
