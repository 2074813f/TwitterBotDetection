package features;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import models.GoogleSBBody;
import models.GoogleSBClient;
import models.GoogleSBThreatInfo;
import models.GoogleSBMatch;

public class URLChecker {
	
	private String urlTemplate = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=%s";
	private String apiKey = System.getenv("GOOGLE_SB_KEY");
	private ObjectMapper mapper = new ObjectMapper();
	
	//TODO: check <=500 URLs
	public List<String> isSpamURLs(Collection<String> unknownURLs) {

		HttpResponse response;
		
		//Build the components for the POST body, to be serialized.
		GoogleSBClient client = new GoogleSBClient("aflemingtbd", "1.0");
		//Initialize and populate threat info with urls.
		GoogleSBThreatInfo threatInfo = new GoogleSBThreatInfo();
		unknownURLs.forEach(url -> threatInfo.addUrlEntry(url));
		
		GoogleSBBody body = new GoogleSBBody(client, threatInfo);	
		
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()){
			//Serialize body of request.
			String httpBody = mapper.writeValueAsString(body);
			//Build request URL.
			HttpPost request = new HttpPost(String.format(urlTemplate, apiKey));
			//Construct body entity.
			StringEntity entity = new StringEntity(httpBody);
			
			request.addHeader("Content-Type", "application/json");
			request.setEntity(entity);
			
			response = httpClient.execute(request);
			String responseBody = response.getEntity().toString();
			
			//Process response.
			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() == 200) {
				GoogleSBMatch[] matches = mapper.readValue(responseBody, GoogleSBMatch[].class);
				
				//Return the resulting urls from the response entity.
				List<String> matchingUrls = new ArrayList<String>();
				for (GoogleSBMatch match : matches) {
					matchingUrls.add(match.getThreat().get("url"));
				}
				return matchingUrls;
			}
			else {
				//TODO: throw exception.
				return null;
			}
		}
		catch (IOException e) {
			
		}
		
		return null;
	}
	
	//TODO: Get current threat lists dynamically.
	//see- https://developers.google.com/safe-browsing/v4/lists
	//(Currently just match to static defaults e.g. "Malware"

}
