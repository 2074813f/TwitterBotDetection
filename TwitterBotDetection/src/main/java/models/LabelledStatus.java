package models;

public class LabelledStatus {
	
	double label;
	String statusText;
	
	public LabelledStatus(double label, String statusText) {
		this.label = label;
		this.statusText = statusText;
	}
	
	public LabelledStatus() {
	}
	
	public double getLabel() {
		return label;
	}
	public void setLabel(double label) {
		this.label = label;
	}
	public String getStatusText() {
		return statusText;
	}
	public void setStatusText(String statusText) {
		this.statusText = statusText;
	}
}
