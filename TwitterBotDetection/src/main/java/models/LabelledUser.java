package models;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to model lines from a Classified File, i.e. one that
 * has labelled users, and has ids for the user and a number of
 * their statuses.
 * 
 * @author Adam
 *
 */
public class LabelledUser {

	String label;
	long userId;
	List<Long> statusIds;
	
	public LabelledUser() {
		statusIds = new ArrayList<Long>();
	}

	
	public void addStatus(long status) {
		statusIds.add(status);
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(userId);
	}

	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public List<Long> getStatusIds() {
		return statusIds;
	}
	public void setStatusIds(List<Long> statusIds) {
		this.statusIds = statusIds;
	}
}
