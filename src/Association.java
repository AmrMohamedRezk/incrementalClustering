
public class Association implements Comparable<Association> {
	private String PID;
	private String QID;
	private double distance;
	private boolean isMerge;
	private double upClusterAvg;
	public void reset()
	{
		isMerge = false;
		upClusterAvg = 0;
	}
	public Association(String p,String q,double distance)
	{
		PID = p;
		QID = q;
		this.distance = distance;
	}
	@Override
	public int compareTo(Association o) {
		if(this.equals(o))
			return 0;
		else 
		{
			double temp = this.distance-o.distance;
			return temp>0 ? 1:-1;
		}
	}
	public boolean equals(Object o) {
		Association other = (Association) o;
		return (this.PID.equals(other.PID) && this.QID.equals(other.QID))
				|| (this.PID.equals(other.QID) && this.QID.equals(other.PID));
	}
	public void setPID(String pID) {
		PID = pID;
	}
	public String getPID() {
		return PID;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	public double getDistance() {
		return distance;
	}
	public void setQID(String qID) {
		QID = qID;
	}
	public String getQID() {
		return QID;
	}
	public void setMerge(boolean isMerge) {
		this.isMerge = isMerge;
	}
	public boolean isMerge() {
		return isMerge;
	}
	public void setUpClusterAvg(double upClusterAvg) {
		this.upClusterAvg = upClusterAvg;
	}
	public double getUpClusterAvg() {
		return upClusterAvg;
	}

}
