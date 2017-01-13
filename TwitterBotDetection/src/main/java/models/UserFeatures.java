package models;

import java.io.Serializable;

/**
 * Contains a feature vector for a given user, referenced by id.
 * @author Adam
 *
 */
//TODO: @Deprecated - just add label and features directly to spark instance.
public class UserFeatures implements Serializable {
	
	private double label;
//	private long id;					//Twitter user id
//	private int screenNameLength;		//Length of screen name
	//TODO: We need a spark vector here somehow.......
	private float followerRatio;		//#Following/#Followers

	public UserFeatures() {
	}
	
	public double getLabel() {
		return label;
	}
	public void setLabel(double label) {
		this.label = label;
	}
//	public long getId() {
//		return id;
//	}
//	public void setId(long id) {
//		this.id = id;
//	}
//	public int getScreenNameLength() {
//		return screenNameLength;
//	}
//	public void setScreenNameLength(int screenNameLength) {
//		this.screenNameLength = screenNameLength;
//	}
	public float getFollowerRatio() {
		return followerRatio;
	}
	public void setFollowerRatio(float followerRatio) {
		this.followerRatio = followerRatio;
	}
}
