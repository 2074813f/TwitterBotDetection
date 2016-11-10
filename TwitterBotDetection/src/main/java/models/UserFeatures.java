package models;

/**
 * Contains a feature vector for a given user, referenced by id.
 * @author Adam
 *
 */
public class UserFeatures {

	private long id;					//Twitter user id
	private int screenNameLength;		//Length of screen name
	private float followerRatio;		//#Following/#Followers
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public int getScreenNameLength() {
		return screenNameLength;
	}
	public void setScreenNameLength(int screenNameLength) {
		this.screenNameLength = screenNameLength;
	}
	public float getFollowerRatio() {
		return followerRatio;
	}
	public void setFollowerRatio(float followerRatio) {
		this.followerRatio = followerRatio;
	}
}
