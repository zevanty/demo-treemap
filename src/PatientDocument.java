/**
 * Object that holds information about the patient
 */
public class PatientDocument {
	//Note: the variable names are also the field names
	private String area = "";
	private String date = "";
	private String authname = "";
	private String title = "";
	private String type = "";
	private String pid = "";
	private String parsed = "";
	private String unparsed = "";
	private String firstName = "";
	private String lastName = "";
	private String sex = "";
	private String race = "";
	private String dob = "";
	
	public String getArea() {
		return area;
	}
	
	public String getDate() {
		return date;
	}
	
	public String getAuthname() {
		return authname;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getType() {
		return type;
	}
	
	public String getPID() {
		return pid;
	}
	
	public String getParsed() {
		return parsed;
	}
	
	public String getUnparsed() {
		return unparsed;
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public String getSex() {
		return sex;
	}
	
	public String getRace() {
		return race;
	}
	
	public String getDOB() {
		return dob;
	}
	
	public void setArea(String area) {
		this.area = area;
	}
	
	public void setDate(String date) {
		this.date = date;
	}
	
	public void setAuthname(String authname) {
		this.authname = authname;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void setPID(String pid) {
		this.pid = pid;
	}
	
	public void setParsed(String parsed) {
		this.parsed = parsed;
	}
	
	public void setUnparsed(String unparsed) {
		this.unparsed = unparsed;
	}
	
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public void setSex(String sex) {
		this.sex = sex;
	}
	
	public void setRace(String race) {
		this.race = race;
	}
	
	public void setDOB(String dob) {
		this.dob = dob;
	}
}
