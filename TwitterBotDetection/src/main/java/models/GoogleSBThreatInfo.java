package models;

import java.util.Map;

public class GoogleSBThreatInfo {
	
	private String[] threatTypes = {"MALWARE", "SOCIAL_ENGINEERING"};
	private String[] platformTypes = {"WINDOWS"};
	private String[] threatEntryTypes = {"URL"};
	private Map<String, String> threatEntries;
	
	//TODO: Consider builder pattern.
	public GoogleSBThreatInfo() {
	}
	
	/**
	 * Add a threat entry of type: url to the threat entries
	 * for this request body.
	 * 
	 * @param url - a url to be checked.
	 */
	public void addUrlEntry(String url) {
		this.threatEntries.put("url", url);
	}
	
	public String[] getThreatTypes() {
		return threatTypes;
	}
	public void setThreatTypes(String[] threatTypes) {
		this.threatTypes = threatTypes;
	}
	public String[] getPlatformTypes() {
		return platformTypes;
	}
	public void setPlatformTypes(String[] platformTypes) {
		this.platformTypes = platformTypes;
	}
	public String[] getThreatEntryTypes() {
		return threatEntryTypes;
	}
	public void setThreatEntryTypes(String[] threatEntryTypes) {
		this.threatEntryTypes = threatEntryTypes;
	}
	public Map<String, String> getThreatEntries() {
		return threatEntries;
	}
	public void setThreatEntries(Map<String, String> threatEntries) {
		this.threatEntries = threatEntries;
	}
}
