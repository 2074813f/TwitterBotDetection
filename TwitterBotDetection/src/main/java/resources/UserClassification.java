package resources;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import models.Features;

/**
 * JSON response POJO with Jackson annotations for use in
 * response for /rest/classify...
 * 
 * @see resources.TBDService
 * @author Adam
 *
 */
@XmlRootElement
public class UserClassification {
	
	@XmlElement
	private long userid = 2074813;
	
	@XmlElement
	private String label = "human";
	
	@XmlElement
	String probability = "";
	
	@XmlElement
	Features features = null;
	
	public UserClassification() {
	}
	
	@Override
	public boolean equals(Object o) {
		//self check
		if (this == o) return true;
		
		//null check
		if (o == null) return false;
		
		//type check and cast
		if (getClass() != o.getClass()) return false;
		
		UserClassification uc = (UserClassification) o;
		return (uc.getUserid() == userid);
	}
	
	public long getUserid() {
		return userid;
	}
	public void setUserid(long userid) {
		this.userid = userid;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getProbability() {
		return probability;
	}
	public void setProbability(String probability) {
		this.probability = probability;
	}
	public Features getFeatures() {
		return features;
	}
	public void setFeatures(Features features) {
		this.features = features;
	}
}
