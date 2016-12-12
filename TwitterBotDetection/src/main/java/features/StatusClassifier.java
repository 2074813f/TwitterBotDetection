package features;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.spark.ml.classification.NaiveBayes;
import org.apache.spark.ml.classification.NaiveBayesModel;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.ml.feature.IDF;
import org.apache.spark.ml.feature.IDFModel;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import models.LabelledStatus;
import models.UserProfile;
import twitter4j.Status;
import twitter4j.URLEntity;

public class StatusClassifier {
	
	static Logger logger = LogManager.getLogger();
	
	public static NaiveBayesModel trainBayesClassifier(SparkSession spark, List<UserProfile> users) {
		
		List<LabelledStatus> statuses = collectSpamStatuses(users);
		
		if (statuses.isEmpty()) {
			logger.error("No training data.");
			return null;
		}
		
		Encoder<LabelledStatus> statusEncoder = Encoders.bean(LabelledStatus.class);
		Dataset<LabelledStatus> ds = spark.createDataset(statuses, statusEncoder);
		
		/* In the following code segment, we start with a set of sentences. 
		We split each sentence into words using Tokenizer. For each sentence 
		(bag of words), we use HashingTF to hash the sentence into a feature vector.*/
		
		Tokenizer tokenizer = new Tokenizer().setInputCol("statusText").setOutputCol("words");
		Dataset<Row> wordDataFrame = tokenizer.transform(ds);

		int numFeatures = 20;
		HashingTF hashingTF = new HashingTF()
		  .setInputCol("words")
		  .setOutputCol("rawFeatures")
		  .setNumFeatures(numFeatures);
		
		Dataset<Row> featurizedData = hashingTF.transform(wordDataFrame);
		// alternatively, CountVectorizer can also be used to get term frequency vectors
		
		/* We use IDF to rescale the feature vectors; this generally improves performance 
		when using text as features. Our feature vectors could then be passed to a learning algorithm.*/

		IDF idf = new IDF().setInputCol("rawFeatures").setOutputCol("features");
		IDFModel idfModel = idf.fit(featurizedData);								//Examine entire corpus and calc IDF
		Dataset<Row> rescaledData = idfModel.transform(featurizedData);				//Convert Statuses to TF-IDF vectors.
		
		Dataset<Row>[] splits = rescaledData.randomSplit(new double[]{0.6, 0.4});
		Dataset<Row> train = splits[0];
		Dataset<Row> test = splits[1];
		
		NaiveBayes nb = new NaiveBayes();
		
		NaiveBayesModel model = nb.fit(train);
		Dataset<Row> result = model.transform(test);
		
		Dataset<Row> predictionAndLabels = result.select("prediction", "label");
		MulticlassMetrics metrics = new MulticlassMetrics(predictionAndLabels);
		
		System.out.println("Accuracy: " + metrics.accuracy());
		System.out.format("Confusion Matrix: \n%s\n\n", metrics.confusionMatrix().toString());

		return model;
	}
	
	/**
	 * Given a list of hydrated users, collect the statuses of these users.
	 * 
	 * @return
	 */
	private static List<LabelledStatus> collectStatuses(List<UserProfile> users) {
		
		//Map UserProfiles to label:status for training.
		//NOTE: converts human->0, else->1 (need double labels)
		List<LabelledStatus> statuses = new ArrayList<LabelledStatus>();
		users.forEach(user -> user.getStatuses()
				.forEach(status -> 
					statuses.add(
							new LabelledStatus(
									((user.getLabel().compareTo("human") == 0) ? 0.0 : 1.0),
									status.getText()
							)
				)));
		
		return statuses;
	}
	
	private static List<LabelledStatus> collectSpamStatuses(List<UserProfile> users) {
		//TODO:Consider that we should only use bots here, as this is for the training set.
		
		List<LabelledStatus> result = new ArrayList<LabelledStatus>();
		
		//Collect all the status objects.
		//Partition into humans and bots for implicit labelling.
		//XXX:Consideration: duplication of code but simplification of processing.
		List<Status> human_statuses = new ArrayList<Status>();
		List<Status> bot_statuses = new ArrayList<Status>();
		users.forEach(user -> {
			if (user.getLabel().compareTo("human") == 0) user.getStatuses().forEach(status -> human_statuses.add(status));
			else user.getStatuses().forEach(status -> bot_statuses.add(status));
		});
		
		//For each list map urls.
		
		//Map URLs to statuses, so we can lookup urls and then
		//retrieve the relevant status. {url:Status}
		Map<String, Status> human_urls = new HashMap<String, Status>();
		Map<String, Status> bot_urls = new HashMap<String, Status>();
		
		//TODO:partition to 500 urls per request.
		for (Status st : human_statuses) {
			URLEntity[] urlents = st.getURLEntities();
			for(URLEntity ent : urlents) {
				human_urls.put(ent.getURL(), st);
			}
		}
		
		for (Status st : bot_statuses) {
			URLEntity[] urlents = st.getURLEntities();
			for(URLEntity ent : urlents) {
				bot_urls.put(ent.getExpandedURL(), st);
			}
		}
		
		logger.info("Found {} human urls.", human_urls.size());
		logger.info("Found {} bot urls.", bot_urls.size());
		
		//Check the urls.
		URLChecker checker = new URLChecker();
		//Check human URLs
		List<String> human_results = checker.isSpamURLs(human_urls.keySet());
		if (human_results != null) {
			logger.info("Found {} human spam urls.", human_results.size());
			human_results.forEach(r -> result.add(new LabelledStatus(0.0, human_urls.get(r).getText())));
		}
		
		//Check bot URLs
		List<String> bot_results = checker.isSpamURLs(bot_urls.keySet());
		if (bot_results != null) {
			logger.info("Found {} bot spam urls.", bot_results.size());
			bot_results.forEach(r -> result.add(new LabelledStatus(1.0, bot_urls.get(r).getText())));
		}

		logger.info("Found {} spam statuses.", result.size());
		
		return result;
	}
	
	/**
	 * Simply prints out each classification result in a
	 * dataset. Prints each entries "features", "prediction"
	 * and "label"
	 * 
	 * @param results - the result dataset to print
	 */
	private static void debugResults(Dataset<Row> result) {
		System.out.println("Full results for debugging:");
		result.select("features", "prediction", "label")
			.collectAsList().forEach(entry ->
			System.out.println(entry.toString()));
	}
	
	/**
	 * Take a labelled status classification and report if the status has
	 * some spam-like characteristic(s) or not.
	 * 
	 * Spam-like:
	 *   - Contains one or more blacklisted URLs
	 * 
	 * @param status
	 * @return
	 */
	private static boolean isSpam(LabelledStatus status) {
		return false;
	}
	

	
}
