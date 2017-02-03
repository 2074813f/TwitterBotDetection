package models;

import java.io.Serializable;

/**
 * Contains a feature vector for a given user.
 * @author Adam
 *
 */
//TODO: @Deprecated - just add label and features directly to spark instance.
public class Features implements Serializable {
	
	private long id;
	private String label;
	
	private int screenNameLength;		//Length of screen name
	private float followerRatio;		//#Following/#Followers
	private float urlRatio;
	private float hashtagRatio;
	private float mentionRatio;
	private int uniqueDevices;
	private String mainDevice;

	public Features() {
	}
	
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
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
	public float getUrlRatio() {
		return urlRatio;
	}
	public void setUrlRatio(float urlRatio) {
		this.urlRatio = urlRatio;
	}
	public float getHashtagRatio() {
		return hashtagRatio;
	}
	public void setHashtagRatio(float hashtagRatio) {
		this.hashtagRatio = hashtagRatio;
	}
	public float getMentionRatio() {
		return mentionRatio;
	}
	public void setMentionRatio(float mentionRatio) {
		this.mentionRatio = mentionRatio;
	}
	public String getMainDevice() {
		return mainDevice;
	}
	public void setMainDevice(String mainDevice) {
		this.mainDevice = mainDevice;
	}


	public int getUniqueDevices() {
		return uniqueDevices;
	}


	public void setUniqueDevices(int uniqueDevices) {
		this.uniqueDevices = uniqueDevices;
	}
}
