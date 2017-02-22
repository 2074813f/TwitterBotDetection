package resources;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * JSON response POJO with Jackson annotations for use in
 * response for /rest/classify...
 * 
 * @author Adam
 *
 */
@XmlRootElement
public class UserClassification {
	
	@XmlElement
	private long userid = 2074813;
	
	@XmlElement
	private String label = "human";
	
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
}
