package resources;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * JSON request POJO with Jackson annotations for use in
 * response for /rest/label...
 * 
 * @see resources.TBDService
 * @author Adam
 *
 */
@XmlRootElement
public class UserLabel {
	
	@XmlElement
	long userid;
	
	@XmlElement
	String label;
	
	public UserLabel() {
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
