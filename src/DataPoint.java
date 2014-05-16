import java.util.HashMap;
import java.util.TreeSet;

public class DataPoint implements Comparable<DataPoint> {

	private String ID;
	private double average;
	private String clusterID;
	private double min;
	private int count;
	private String finalClusterID;
	private TreeSet<Double> FRangeDistance;
	private HashMap<Integer, Double> vector;
	private String url;
	private double value;


	public DataPoint(String inputLine) {
		// String[] line = inputLine.split(" ");
		// HashMap<Integer, Double> map = new HashMap<Integer, Double> ();
		// int id = 0;
		// for(String token : line){
		// map.put(id, Double.parseDouble(token));
		// id++;
		// }
		// setVector(map);
		// setID(""+GID);
		// setClusterID(""+GID);
		// setFinalClusterID(""+GID);
		// GID++;
		// FRangeDistance = new TreeSet<Double>();

		String[] attributes = inputLine.split("&");
		for (int j = 0; j < attributes.length; j++) {
			String key = attributes[j].substring(0, attributes[j].indexOf(':'));
			String value = "";
			if (key.equals("ID"))
				setID(attributes[j].substring(attributes[j].indexOf(':') + 1));
			else if (key.equals("features")) {
				HashMap<Integer, Double> map = new HashMap<Integer, Double>();
				value = attributes[j].substring(attributes[j].indexOf(':') + 2,
						attributes[j].length() - 1);
				// value --> id=value ,
				String[] hashMap = value.split(",");
				for (String s : hashMap)
					map.put(Integer.parseInt(s.substring(0, s.indexOf('='))
							.trim()), Double.parseDouble(s.substring(
							s.indexOf('=') + 1).trim()));
				setVector(map);
			} else if (key.equals("url"))
				setUrl(attributes[j].substring(attributes[j].indexOf(':') + 1));

		}
		FRangeDistance = new TreeSet<Double>();
		setClusterID(getID());
		
		calculateValue();

	}

	public String toString() {
		return ID;
	}

	public void setID(String iD) {
		ID = iD;
	}

	public String getID() {
		return ID;
	}

	public void setAverage(double average) {
		this.average = average;
	}

	public double getAverage() {
		return average;
	}

	public void setClusterID(String clusterID) {
		this.clusterID = clusterID;
	}

	public String getClusterID() {
		return clusterID;
	}

	public void setMin(double min) {
		this.min = min;
	}

	public double getMin() {
		return min;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getCount() {
		return count;
	}

	public void setFinalClusterID(String finalClusterID) {
		this.finalClusterID = finalClusterID;
	}

	public String getFinalClusterID() {
		return finalClusterID;
	}

	public void setFRangeDistance(TreeSet<Double> fRangeDistance) {
		FRangeDistance = fRangeDistance;
	}

	public TreeSet<Double> getFRangeDistance() {
		return FRangeDistance;
	}

	public void setVector(HashMap<Integer, Double> vector) {
		this.vector = vector;
	}

	public HashMap<Integer, Double> getVector() {
		return vector;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public int compareTo(DataPoint other) {
		return ID.compareTo(other.getID());
	}

	@Override
	public boolean equals(Object o) {
		return ID.equals(((DataPoint) o).getID());
	}

	public void setValue(double value) {
		this.value = value;
	}

	public double getValue() {
		return value;
	}

	public void calculateValue() {
		HashMap<Integer, Double> featuresVector = getVector();
		value = 0;
		for (int key : featuresVector.keySet()) {
			value += Math.pow(featuresVector.get(key), 2);
		}
		value = Math.sqrt(value);
	}

	public double calculateDistance(DataPoint other) {
		if (this.equals(other))
			return 1;
		HashMap<Integer, Double> minHash, maxHash;
		HashMap<Integer, Double> current = getVector(), otherHashMap = other
				.getVector();

		if (current.size() < otherHashMap.size()) {
			minHash = current;
			maxHash = otherHashMap;
		} else {
			minHash = otherHashMap;
			maxHash = current;
		}
		double d1_d2 = 0;
		for (int key : minHash.keySet()) {
			if (maxHash.containsKey(key))
				d1_d2 += maxHash.get(key) * minHash.get(key);
		}
		if (this.value == 0)
			calculateValue();
		if (other.value == 0)
			other.calculateValue();
		double denum = this.value * other.value;
		return 1 - (d1_d2 / denum);
		// HashMap<Integer, Double> dim1 = getVector();
		// HashMap<Integer, Double> dim2 = other.getVector();
		// double ECDis = 0.0;
		// int dimSize = dim1.size();
		// for (int k = 0; k < dimSize; k++) {
		// ECDis += (dim1.get(k) - dim2.get(k))
		// * ((dim1.get(k) - dim2.get(k)));
		// }
		// ECDis = Math.sqrt(ECDis);
		// return ECDis;
	}
}
