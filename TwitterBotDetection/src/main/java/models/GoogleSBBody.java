package models;

/**
 * Model for body of POST request to the Google
 * SafeBrowsing v4 lookup API.
 * 
 * @author Adam
 *
 */
public class GoogleSBBody {
	
	private GoogleSBClient client;
	private GoogleSBThreatInfo threatInfo;
	
	public GoogleSBBody(GoogleSBClient client, GoogleSBThreatInfo threatInfo) {
		this.client = client;
		this.threatInfo = threatInfo;
	}
	
	public GoogleSBClient getClient() {
		return client;
	}
	public void setClient(GoogleSBClient client) {
		this.client = client;
	}
	public GoogleSBThreatInfo getThreatInfo() {
		return threatInfo;
	}
	public void setThreatInfo(GoogleSBThreatInfo threatInfo) {
		this.threatInfo = threatInfo;
	}
}
