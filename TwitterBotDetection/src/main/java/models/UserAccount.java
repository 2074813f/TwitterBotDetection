package models;

import java.util.List;

import twitter4j.Status;
import twitter4j.User;

/**
 * Models a twitter user and a set of their statuses.
 * @author Adam
 *
 */
public class UserAccount {
	
	private User user;
	private List<Status> statuses;
	
	public UserAccount(User user, Status status) {
		this.user = user;
		this.statuses.add(status);
	}
	
	public UserAccount() {}
	
	public void addStatus(Status status) {
		statuses.add(status);
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

}
