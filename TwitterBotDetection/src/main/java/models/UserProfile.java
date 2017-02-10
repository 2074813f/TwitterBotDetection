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
	
	@JsonSerialize(using = UserSerializer.class, as=User.class)
	private User user;							//User profile object.
	
	@JsonSerialize(using = StatusesSerializer.class, as=List.class)
	private List<Status> trainingStatuses;		//Statuses associated with the User gathered from ground-truth dataset.
	@JsonSerialize(using = StatusesSerializer.class, as=List.class)
	private List<Status> userTimeline;			//Sequence of recent user statuses
	
	public UserProfile(String label, User user, List<Status> statuses) {
		this.label = label;
		this.features = null;
		this.user = user;
		this.trainingStatuses = statuses;

		this.userTimeline = new ArrayList<Status>();
	}
	
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
	public void setUser(User user) {
		this.user = user;
	}
	public List<Status> getTrainingStatuses() {
		return trainingStatuses;
	}
	public void setTrainingStatuses(List<Status> statuses) {
		this.trainingStatuses = statuses;
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
	public void setUserTimeline(List<Status> userTimeline) {
		this.userTimeline = userTimeline;
	}
}
