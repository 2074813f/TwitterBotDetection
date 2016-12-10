package models;

/**
 * Model for the client part of a POST request
 * to Google Safe Browsing lookup API. 
 * 
 * @author Adam
 *
 */
public class GoogleSBClient {
	
	private String clientId;
	private String clientVersion;
	
	public GoogleSBClient(String clientId, String clientVersion) {
		this.clientId = clientId;
		this.clientVersion = clientVersion;
	}
	
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public String getClientVersion() {
		return clientVersion;
	}
	public void setClientVersion(String clientVersion) {
		this.clientVersion = clientVersion;
	}
}
