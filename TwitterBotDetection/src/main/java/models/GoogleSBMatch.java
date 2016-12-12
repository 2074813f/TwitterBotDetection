package models;

import java.util.Map;

/**
 * Model for a match object of a response
 * from the Google Safe Browsing API.
 * 
 * @author Adam
 *
 */
public class GoogleSBMatch {
	
	String threatType;
	String platformType;
	String threatEntryType;
	Map<String, String> threat;
	ThreatEntryMetadata threatEntryMetadata;
	String cacheDuration;
	
	public static class ThreatEntryMetadata {
		KVPair[] entries;

		public KVPair[] getEntries() {
			return entries;
		}
		public void setEntries(KVPair[] entries) {
			this.entries = entries;
		}
	}
	
	public static class KVPair {
		String key;
		String value;
		
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}

	public String getThreatType() {
		return threatType;
	}
	public void setThreatType(String threatType) {
		this.threatType = threatType;
	}
	public String getPlatformType() {
		return platformType;
	}
	public void setPlatformType(String platformType) {
		this.platformType = platformType;
	}
	public String getThreatEntryType() {
		return threatEntryType;
	}
	public void setThreatEntryType(String threatEntryType) {
		this.threatEntryType = threatEntryType;
	}
	public Map<String, String> getThreat() {
		return threat;
	}
	public void setThreat(Map<String, String> threat) {
		this.threat = threat;
	}
	public ThreatEntryMetadata getThreatEntryMetadata() {
		return threatEntryMetadata;
	}
	public void setThreatEntryMetadata(ThreatEntryMetadata threatEntryMetadata) {
		this.threatEntryMetadata = threatEntryMetadata;
	}
	public String getCacheDuration() {
		return cacheDuration;
	}
	public void setCacheDuration(String cacheDuration) {
		this.cacheDuration = cacheDuration;
	}
}
