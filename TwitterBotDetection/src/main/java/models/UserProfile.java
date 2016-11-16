package models;

import java.util.ArrayList;
import java.util.List;

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
	
	private String label;
	private User user;
	private List<Status> statuses;
	
	public UserProfile(User user, Status status) {
		this.user = user;
		
		this.statuses = new ArrayList<Status>(25);
		this.statuses.add(status);
	}
	
	public UserProfile() {}
	
	public void addStatus(Status status) {
		statuses.add(status);
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
	public List<Status> getStatuses() {
		return statuses;
	}
	public void setStatuses(List<Status> statuses) {
		this.statuses = statuses;
	}
	public String getClassifierClass() {
		return label;
	}
	public void setClassifierClass(String classifierClass) {
		this.label = classifierClass;
	}
}
