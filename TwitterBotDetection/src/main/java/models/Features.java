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
	private float isProtected;
	private float isVerified;
	private float followerRatio;		//#Following/#Followers
	private float urlRatio;
	private float hashtagRatio;
	private float mentionRatio;
	private int uniqueDevices;
	private int mainDeviceCount;
	private String mainDevice;
	private float tweetRate;
	private int maxTweetRate;
	private float meanIA;				//Mean inter-arrival

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
	public int getMainDeviceCount() {
		return mainDeviceCount;
	}
	public void setMainDeviceCount(int mainDeviceCount) {
		this.mainDeviceCount = mainDeviceCount;
	}
	public float getTweetRate() {
		return tweetRate;
	}
	public void setTweetRate(float tweetRate) {
		this.tweetRate = tweetRate;
	}
	public int getMaxTweetRate() {
		return maxTweetRate;
	}
	public void setMaxTweetRate(int maxTweetRate) {
		this.maxTweetRate = maxTweetRate;
	}
	public float getMeanIA() {
		return meanIA;
	}
	public void setMeanIA(float meanIA) {
		this.meanIA = meanIA;
	}
	public float getIsProtected() {
		return isProtected;
	}
	public void setIsProtected(float isProtected) {
		this.isProtected = isProtected;
	}
	public float getIsVerified() {
		return isVerified;
	}
	public void setIsVerified(float isVerified) {
		this.isVerified = isVerified;
	}
}
