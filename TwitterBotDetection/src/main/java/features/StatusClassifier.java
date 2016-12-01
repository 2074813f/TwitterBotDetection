package features;

import java.util.ArrayList;
import java.util.List;

import org.apache.spark.ml.classification.NaiveBayes;
import org.apache.spark.ml.classification.NaiveBayesModel;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.ml.feature.IDF;
import org.apache.spark.ml.feature.IDFModel;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.ml.linalg.SparseVector;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import models.LabelledStatus;
import models.UserProfile;

public class StatusClassifier {
	
	public static void trainClassifier(SparkSession spark, List<UserProfile> users) {
		
		//Map UserProfiles to label:status for training.
		//NOTE: converts human->0 else->1
		List<LabelledStatus> statuses = new ArrayList<LabelledStatus>();
		users.forEach(user -> user.getStatuses()
				.forEach(status -> statuses.add(
						new LabelledStatus(
								((user.getLabel().compareTo("human") == 0) ? 0.0 : 1.0),
								status.getText()
								))));
		
		Encoder<LabelledStatus> statusEncoder = Encoders.bean(LabelledStatus.class);
		Dataset<LabelledStatus> ds = spark.createDataset(statuses, statusEncoder);
		
		/* In the following code segment, we start with a set of sentences. 
		We split each sentence into words using Tokenizer. For each sentence 
		(bag of words), we use HashingTF to hash the sentence into a feature vector.*/
		
		Tokenizer tokenizer = new Tokenizer().setInputCol("statusText").setOutputCol("words");
		Dataset<Row> wordDataFrame = tokenizer.transform(ds);

		int numFeatures = 100;
		HashingTF hashingTF = new HashingTF()
		  .setInputCol("words")
		  .setOutputCol("rawFeatures")
		  .setNumFeatures(numFeatures);
		
		Dataset<Row> featurizedData = hashingTF.transform(wordDataFrame);
		// alternatively, CountVectorizer can also be used to get term frequency vectors
		
		/* We use IDF to rescale the feature vectors; this generally improves performance 
		when using text as features. Our feature vectors could then be passed to a learning algorithm.*/

		IDF idf = new IDF().setInputCol("rawFeatures").setOutputCol("features");
		IDFModel idfModel = idf.fit(featurizedData);
		Dataset<Row> rescaledData = idfModel.transform(featurizedData);
		for (Row r : rescaledData.select("features", "label").takeAsList(3)) {
		  SparseVector features = r.getAs(0);
		  double label = r.getDouble(1);
		  System.out.println(features);
		  System.out.println(label);
		}
		
		Dataset<Row>[] splits = rescaledData.randomSplit(new double[]{0.50, 0.50});
		Dataset<Row> train = splits[0];
		Dataset<Row> test = splits[1];
		
		NaiveBayes nb = new NaiveBayes();
		
		NaiveBayesModel model = nb.fit(train);
		Dataset<Row> result = model.transform(test);
		
		Dataset<Row> predictionAndLabels = result.select("prediction", "label");
		MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
				  .setMetricName("accuracy");
		System.out.println("Accuracy = " + evaluator.evaluate(predictionAndLabels));
	}

}
