package models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import serializer.StatusSerializer;
import serializer.StatusesSerializer;
import serializer.UserSerializer;
import twitter4j.Status;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;

/**
 * Models a twitter user and a set of their statuses.
 * 
 * Compares with, and assumes a unique constant long id for any
 * given user.
 * 
 * @author Adam
 *
 */
public class UserProfile {
	
	private String label;						//Class label (i.e. "Human", "Bot").
	private Features features;					//Extracted features.

	private User user;							//User profile object.
	
	private List<Status> trainingStatuses;		//Statuses associated with the User gathered from ground-truth dataset.
	private List<Status> userTimeline;			//Sequence of recent user statuses
	
	/*
	 * Twitter4J discards raw json if a different interface method is used,
	 * hence we must store the raw json for later caching.
	 */
	private String marshalledUser;
	private List<String> marshalledTrainingStatuses;
	private List<String> marshalledUserTimeLine;
	
	public UserProfile() {
		this.label = null;
		this.features = null;
		this.user = null;
		
		this.trainingStatuses = new ArrayList<Status>();
		this.userTimeline = new ArrayList<Status>();
	}
	
	public void addTrainingStatus(Status status) {
		trainingStatuses.add(status);
	}
	
	@Override
	public boolean equals(Object obj) {
		
		// Check if an instance of UserAccount, return false if not.
		if (!(obj instanceof UserProfile)) {
			return false;
		}
		
		//Cast obj to UserAccount for comparison
		UserProfile comp = (UserProfile) obj;
		
		return user.getId() == comp.getUser().getId();
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(user.getId());
	}
	
	
	public User getUser() {
		return user;
	}
	public void setUser(User user, String marshalledUser) {
		this.user = user;
		this.marshalledUser = marshalledUser;
	}
	public List<Status> getTrainingStatuses() {
		return trainingStatuses;
	}
	public void setTrainingStatuses(List<Status> statuses, List<String> marshalledStatuses) {
		this.trainingStatuses = statuses;
		this.marshalledTrainingStatuses = marshalledStatuses;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public Features getFeatures() {
		return features;
	}
	public void setFeatures(Features features) {
		this.features = features;
	}
	public List<Status> getUserTimeline() {
		return userTimeline;
	}
	public void setUserTimeline(List<Status> userTimeline, List<String> marshalledUserTimeline) {
		this.userTimeline = userTimeline;
		this.marshalledUserTimeLine = marshalledUserTimeline;
	}

	public String getMarshalledUser() {
		return marshalledUser;
	}
	public List<String> getMarshalledTrainingStatuses() {
		return marshalledTrainingStatuses;
	}
	public List<String> getMarshalledUserTimeLine() {
		return marshalledUserTimeLine;
	}

	public void setMarshalledUser(String marshalledUser) {
		this.marshalledUser = marshalledUser;
	}

	public void setMarshalledTrainingStatuses(List<String> marshalledTrainingStatuses) {
		this.marshalledTrainingStatuses = marshalledTrainingStatuses;
	}

	public void setMarshalledUserTimeLine(List<String> marshalledUserTimeLine) {
		this.marshalledUserTimeLine = marshalledUserTimeLine;
	}
	
	
}
