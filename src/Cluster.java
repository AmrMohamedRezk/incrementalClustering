import java.util.ArrayList;
import java.util.HashSet;

public class Cluster {
	private ArrayList<Association> associationsList;
	private HashSet<Association> associationsSet;
	private HashSet<String> pointSet;
	private String ID;
	private double averageDistance;
	private boolean changed;
	
	
	public Cluster(DataPoint point)
	{
		ID = point.getID();
		averageDistance = point.getAverage();
		associationsList = new ArrayList<Association>();
		associationsSet = new HashSet<Association>();
		pointSet = new HashSet<String>();
		pointSet.add(point.getID());
	}
	public double getHarmonicAverage() {
		double sum = 0;
		for (Association association : associationsSet) 
			sum += (1 / association.getDistance());
		

		return (associationsList.size() / sum);
	}
	public void setID(String iD) {
		ID = iD;
	}
	public String getID() {
		return ID;
	}
	public void setAssociationsSet(HashSet<Association> associationsSet) {
		this.associationsSet = associationsSet;
	}
	public HashSet<Association> getAssociationsSet() {
		return associationsSet;
	}
	public void setAssociationsList(ArrayList<Association> associationsList) {
		this.associationsList = associationsList;
	}
	public ArrayList<Association> getAssociationsList() {
		return associationsList;
	}
	public void setAverageDistance(double averageDistance) {
		this.averageDistance = averageDistance;
	}
	public double getAverageDistance() {
		return averageDistance;
	}
	public void setChanged(boolean changed) {
		this.changed = changed;
	}
	public boolean isChanged() {
		return changed;
	}
	public HashSet<String> getDataPointSet()
	{
		return pointSet;
	}
	public void setDataPointSet(HashSet<String> newDataPoints)
	{
		pointSet = newDataPoints;
	}
	public void clearAssociations()
	{
		associationsList = new ArrayList<Association>();
		associationsSet = new HashSet<Association>();
	}
	public void addAssociation(Association a)
	{
		if(!associationsSet.contains(a))
		{
			associationsList.add(a);
			associationsSet.add(a);
		}
		
	}
	public int getSize()
	{
		return associationsList.size();
	}
	
}
