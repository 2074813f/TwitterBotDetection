package features;

import java.io.IOException;
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

public class URLChecker {
	
	private String urlTemplate = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=%s";
	private String apiKey = System.getenv("GOOGLE_SB_KEY");
	private ObjectMapper mapper = new ObjectMapper();
	
	public List<String> isSpamURLs(List<String> unknownURLs) {

		HttpResponse response;
		
		//Build the components for the POST body, to be serialized.
		GoogleSBClient client = new GoogleSBClient("aflemingtbd", "1.0");
		GoogleSBThreatInfo threatInfo = new GoogleSBThreatInfo();
		unknownURLs.stream().forEach(url -> threatInfo.addUrlEntry(url));
		GoogleSBBody body = new GoogleSBBody(client, threatInfo);	
		
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()){
			//Serialize body of request.
			String httpBody = mapper.writeValueAsString(body);
			//Build request URL.
			HttpPost request = new HttpPost(String.format(urlTemplate, apiKey));
			//Construct body entity.
			StringEntity entity = new StringEntity(httpBody);
			
			request.addHeader("content-type", "application/json");
			request.setEntity(entity);
			
			response = httpClient.execute(request);
			
			//Process response.
			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() != 200) {
				//TODO: deserialize, parse and return urls.
				return null;
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

}
