import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

public class Core {
	public static HashMap<String, Cluster> clusterMap;
	static {
		clusterMap = new HashMap<String, Cluster>();
	}

	public static ArrayList<Association> getIncrementalFRange(
			DataPoint newPoint, ArrayList<DataPoint> points) {
		ArrayList<DataPoint> affectedPoints = new ArrayList<DataPoint>();
		double min = Global.MAX_DISTANCE_POSSIBLE;
		for (DataPoint current : points) {
			if (!current.equals(newPoint)) {
				double distance = current.calculateDistance(newPoint);
				if (distance < min)
					min = distance;
				if (distance < current.getMin()) {
					affectedPoints.add(current);
					current.setMin(distance);
				}
				if (distance <= Global.F * current.getMin()) {
					current.setAverage((current.getAverage()
							* current.getCount() + distance)
							/ (current.getCount() + 1));
					current.setCount(current.getCount() + 1);
				}
			}
		}
		double range = Global.MAX_DISTANCE_POSSIBLE;
		if (min != Global.MAX_DISTANCE_POSSIBLE)
			range = Global.F * min;
		newPoint.setMin(min);
		ArrayList<Association> incrementalFRange = new ArrayList<Association>();
		double sum = 0;
		int count = 0;
		for (DataPoint current : points) {
			double distance = current.calculateDistance(newPoint);
			if (distance <= range && distance < Global.MAX_ASSOCIATION_DISTANCE) {
				newPoint.getFRangeDistance().add(distance);
				Association association = new Association(newPoint.getID(),
						current.getID(), distance);
				incrementalFRange.add(association);
				sum += distance;
				count++;
			} else if (current.getMin() != Global.MAX_DISTANCE_POSSIBLE
					&& distance <= Global.F * current.getMin()&& distance < Global.MAX_ASSOCIATION_DISTANCE) {
				Association association = new Association(current.getID(),
						newPoint.getID(), distance);
				incrementalFRange.add(association);
			}
			if (current.getMin() != Global.MAX_DISTANCE_POSSIBLE
					&& distance <= Global.F * current.getMin()&& distance < Global.MAX_ASSOCIATION_DISTANCE)
				current.getFRangeDistance().add(distance);
		}
		if (count != 0)
			newPoint.setAverage(sum * 1.0 / count * 1.0);
		else
			newPoint.setAverage(Global.MAX_DISTANCE_POSSIBLE);
		newPoint.setCount(count);

		for (DataPoint changed : affectedPoints) {
			TreeSet<Double> distances = changed.getFRangeDistance();
			double rSum = 0;
			int rCount = 0;
			while (!distances.isEmpty()
					&& distances.last() > Global.F * changed.getMin()) {
				rSum += distances.pollLast();
				rCount++;
			}
			if (rCount > 0) {
				changed.setAverage(((changed.getAverage() * changed.getCount()) - rSum)
						/ (changed.getCount() - rCount));
				changed.setCount(changed.getCount() - rCount);
			}
		}

		return incrementalFRange;
	}

	public static ArrayList<Association> getFRangeClusterAssociation(
			ArrayList<Association> associations, DataPoint newPoint) {
		ArrayList<Association> result = new ArrayList<Association>();
		for (Association association : associations) {
			DataPoint p = Global.DBSIMULATOR.get(association.getPID());
			DataPoint q = Global.DBSIMULATOR.get(association.getQID());
			DataPoint current = p.equals(newPoint) ? q : p;

			if (clusterMap.containsKey(current.getClusterID())) {
				Cluster currentCluster = clusterMap.get(current.getClusterID());
				if (currentCluster.getAssociationsList().size() == 0)
					continue;
				currentCluster.setChanged(true);
				int startIndex = 0;
				for (int i = 0; i < currentCluster.getAssociationsList().size(); i++) {
					Association a = currentCluster.getAssociationsList().get(i);
					DataPoint aP = Global.DBSIMULATOR.get(a.getPID());
					DataPoint aQ = Global.DBSIMULATOR.get(a.getQID());
					double distance = a.getDistance();
					if (distance > aP.getMin() && distance > aQ.getMin())
						break;
					if (a.isMerge())
						startIndex = i;
					if (distance > newPoint.getMin())
						break;
				}

				for (int i = startIndex; i < currentCluster
						.getAssociationsList().size(); i++)
					result.add(currentCluster.getAssociationsList().get(i));

				HashSet<String> remainingDataPoints = new HashSet<String>();
				ArrayList<Association> remainingAssociations = new ArrayList<Association>();

				for (int i = 0; i < startIndex; i++) {
					Association currentAssociation = currentCluster
							.getAssociationsList().get(i);
					remainingDataPoints.add(currentAssociation.getPID());
					remainingDataPoints.add(currentAssociation.getQID());
					remainingAssociations.add(currentAssociation);
				}
				// BREAK THE CLUSTER...
				if (startIndex == 0)
					clusterMap.remove(currentCluster.getID());
				for (String dataID : currentCluster.getDataPointSet()) {
					if (!remainingDataPoints.contains(dataID))
						Global.DBSIMULATOR.get(dataID).setClusterID(dataID);
				}
				currentCluster.setDataPointSet(remainingDataPoints);
				currentCluster.clearAssociations();
				for (Association a : remainingAssociations)
					currentCluster.addAssociation(a);
			}
		}
		return result;
	}

	public static ArrayList<Association> incremantalMergeCluster(
			TreeSet<Association> associations) {
		double newAvg;
		ArrayList<Association> temp = new ArrayList<Association>();
		for (Association current : associations) {
			DataPoint p = Global.DBSIMULATOR.get(current.getPID());
			DataPoint q = Global.DBSIMULATOR.get(current.getQID());
			double distance = current.getDistance();
			if ((distance <= p.getMin() * Global.F)
					|| (distance <= q.getMin() * Global.F)&& distance < Global.MAX_ASSOCIATION_DISTANCE) {
				Cluster c1 = null, c2 = null;
				if (!clusterMap.containsKey(p.getClusterID()))
					clusterMap.put(p.getClusterID(), new Cluster(p));
				c1 = clusterMap.get(p.getClusterID());
				if (c1.getDataPointSet().size() == 1)
					c1.setAverageDistance(p.getAverage());

				if (!clusterMap.containsKey(q.getClusterID()))
					clusterMap.put(q.getClusterID(), new Cluster(q));
				c2 = clusterMap.get(q.getClusterID());
				if (c2.getDataPointSet().size() == 1)
					c2.setAverageDistance(q.getAverage());
				double max = Math.max(c1.getAverageDistance(),
						c2.getAverageDistance());
				double minByK = Global.K
						* Math.min(c1.getAverageDistance(),
								c2.getAverageDistance());

				if ((distance <= minByK) && (max <= minByK)&& distance < Global.MAX_ASSOCIATION_DISTANCE) {
					c1.setChanged(true);
					c2.setChanged(true);
					if (!c1.getID().equals(c2.getID())) {
						newAvg = distance
								+ (c1.getAverageDistance() * c1.getSize())
								+ (c2.getAverageDistance() * c2.getSize());
						// int c1ID = Integer.parseInt(c1.getID());
						// int c2ID = Integer.parseInt(c2.getID());
						if (c1.getID().compareTo(c2.getID()) > 0) {
							// if (c1ID>c2ID) {
							current.setMerge(true);
							current.setUpClusterAvg(c1.getAverageDistance());
							c1.addAssociation(current);
							ArrayList<Association> c2Associations = c2
									.getAssociationsList();
							for (Association s : c2Associations)
								c1.addAssociation(s);

							c1.setAverageDistance(newAvg / c1.getSize());
							HashSet<String> c1DataPoints = c1.getDataPointSet();
							HashSet<String> c2DataPoints = c2.getDataPointSet();
							for (String s : c2DataPoints) {
								c1DataPoints.add(s);
								Global.DBSIMULATOR.get(s).setClusterID(
										c1.getID());
							}
							clusterMap.remove(c2.getID());

						} else {
							current.setMerge(true);
							current.setUpClusterAvg(c2.getAverageDistance());
							c2.addAssociation(current);
							ArrayList<Association> c1Associations = c1
									.getAssociationsList();
							for (Association s : c1Associations)
								c2.addAssociation(s);
							c2.setAverageDistance(newAvg / c2.getSize());
							HashSet<String> c1DataPointsIDs = c1
									.getDataPointSet();
							HashSet<String> c2DataPointsIDs = c2
									.getDataPointSet();
							for (String s : c1DataPointsIDs) {
								c2DataPointsIDs.add(s);
								Global.DBSIMULATOR.get(s).setClusterID(
										c2.getID());
							}
							clusterMap.remove(c1.getID());

						}
					} else {
						newAvg = distance
								+ (c1.getAverageDistance() * c1.getSize());
						c1.addAssociation(current);
						c1.setAverageDistance(newAvg / c1.getSize());
					}
				} else
					temp.add(current);
			}
		}
		return temp;
	}

	public static TreeSet<Association> mergeArrays(
			ArrayList<Association> associations,
			ArrayList<Association> fClusterAssociations,
			ArrayList<Association> left) {
		TreeSet<Association> combine = new TreeSet<Association>();
		for (Association a : associations)
			combine.add(a);
		for (Association a : fClusterAssociations)
			combine.add(a);
		for (Association a : left)
			combine.add(a);
		return combine;
	}

	public static ArrayList<Association> refineStepOne(
			ArrayList<Association> associations) {
		ArrayList<Association> temp = new ArrayList<Association>();
		double newAvg = 0;
		for (int i = 0; i < associations.size(); i++) {
			Association current = associations.get(i);
			DataPoint p = Global.DBSIMULATOR.get(current.getPID());
			DataPoint q = Global.DBSIMULATOR.get(current.getQID());
			double d = current.getDistance();
			Cluster c1 = null, c2 = null;
			if (!clusterMap.containsKey(p.getClusterID()))
				clusterMap.put(p.getClusterID(), new Cluster(p));
			c1 = clusterMap.get(p.getClusterID());
			if (c1.getDataPointSet().size() == 1)
				c1.setAverageDistance(p.getAverage());

			if (!clusterMap.containsKey(q.getClusterID()))
				clusterMap.put(q.getClusterID(), new Cluster(q));
			c2 = clusterMap.get(q.getClusterID());
			if (c2.getDataPointSet().size() == 1)
				c2.setAverageDistance(q.getAverage());
			if (c1.getID().equals(c2.getID())
					&& d <= Global.K * c1.getAverageDistance()&& d < Global.MAX_ASSOCIATION_DISTANCE) {
				c1.setChanged(true);
				newAvg = d + (c1.getAverageDistance() * c1.getSize());
				c1.addAssociation(current);
				c1.setAverageDistance(newAvg / c1.getSize());
			} else
				temp.add(current);
		}
		return temp;
	}

	public static void refineClusters() {
		HashMap<String, Cluster> newMap = new HashMap<String, Cluster>();
		// ArrayList<String> clusterList = new ArrayList<String>();
		// for (String currentClusterId : Core.clusterMap.keySet()) {
		// clusterList.add(currentClusterId);
		// }
		// Collections.sort(clusterList,new Comparator<String>() {
		//
		// @Override
		// public int compare(String o1, String o2) {
		// // TODO Auto-generated method stub
		// int id1=Integer.parseInt(Core.clusterMap.get(o1).getID());
		// int id2 = Integer.parseInt(Core.clusterMap.get(o2).getID());
		// return id1-id2;
		// }
		// });
		for (String currentClusterId : Core.clusterMap.keySet()) {
			Cluster currentCluster = Core.clusterMap.get(currentClusterId);
			if (currentCluster.isChanged()) {
				currentCluster.setChanged(false);
				double harmonicAverage = currentCluster.getHarmonicAverage();
				ArrayList<Association> originalAssociations = currentCluster
						.getAssociationsList();
				Association[] sortedAssociation = new Association[originalAssociations
						.size()];
				int i = 0;
				for (Association a : originalAssociations) {
					// Association copy = new Association(a.getPID(),
					// a.getQID(),a.getDistance());
					a.reset();
					sortedAssociation[i++] = a;
				}
				Arrays.sort(sortedAssociation);
				HashSet<String> pointsIdSet = new HashSet<String>();
				for (Association current : sortedAssociation) {
					DataPoint p = Global.DBSIMULATOR.get(current.getPID());
					DataPoint q = Global.DBSIMULATOR.get(current.getQID());
					double distance = current.getDistance();
					if ((distance <= Global.F * p.getMin()
							|| distance <= Global.F * q.getMin())&& distance < Global.MAX_ASSOCIATION_DISTANCE) {
						if (!pointsIdSet.contains(p.getID())) {
							p.setClusterID(p.getID());
							pointsIdSet.add(p.getID());
						}
						if (!pointsIdSet.contains(q.getID())) {
							q.setClusterID(q.getID());
							pointsIdSet.add(q.getID());
						}
						Cluster c1 = null, c2 = null;
						if (!newMap.containsKey(p.getClusterID())) {
							newMap.put(p.getClusterID(), new Cluster(p));
							p.setClusterID(p.getID());
						}
						c1 = newMap.get(p.getClusterID());
						if (c1.getDataPointSet().size() == 1)
							c1.setAverageDistance(p.getAverage());

						if (!newMap.containsKey(q.getClusterID())) {
							newMap.put(q.getClusterID(), new Cluster(q));
							q.setClusterID(q.getID());
						}
						c2 = newMap.get(q.getClusterID());
						if (c2.getDataPointSet().size() == 1)
							c2.setAverageDistance(q.getAverage());
						if (distance <= Global.K * harmonicAverage&& distance < Global.MAX_ASSOCIATION_DISTANCE) {
							double newAvg = 0;
							if (!c1.getID().equals(c2.getID())) {
								newAvg = distance
										+ (c1.getAverageDistance() * c1
												.getSize())
										+ (c2.getAverageDistance() * c2
												.getSize());
								// int c1ID = Integer.parseInt(c1.getID());
								// int c2ID = Integer.parseInt(c2.getID());
								if (c1.getID().compareTo(c2.getID()) > 0) {
									// if (c1ID>c2ID) {
									current.setMerge(true);
									current.setUpClusterAvg(c1
											.getAverageDistance());
									c1.addAssociation(current);
									ArrayList<Association> c2Associations = c2
											.getAssociationsList();
									for (Association s : c2Associations)
										c1.addAssociation(s);

									c1.setAverageDistance(newAvg / c1.getSize());
									HashSet<String> c1DataPoints = c1
											.getDataPointSet();
									HashSet<String> c2DataPoints = c2
											.getDataPointSet();
									for (String s : c2DataPoints) {
										c1DataPoints.add(s);
										Global.DBSIMULATOR.get(s).setClusterID(
												c1.getID());
									}
									newMap.remove(c2.getID());

								} else {
									current.setMerge(true);
									current.setUpClusterAvg(c2
											.getAverageDistance());
									c2.addAssociation(current);
									ArrayList<Association> c1Associations = c1
											.getAssociationsList();
									for (Association s : c1Associations)
										c2.addAssociation(s);
									c2.setAverageDistance(newAvg / c2.getSize());
									HashSet<String> c1DataPointsIDs = c1
											.getDataPointSet();
									HashSet<String> c2DataPointsIDs = c2
											.getDataPointSet();
									for (String s : c1DataPointsIDs) {
										c2DataPointsIDs.add(s);
										Global.DBSIMULATOR.get(s).setClusterID(
												c2.getID());
									}
									newMap.remove(c1.getID());

								}
							} else {
								newAvg = distance
										+ (c1.getAverageDistance() * c1
												.getSize());
								c1.addAssociation(current);
								c1.setAverageDistance(newAvg / c1.getSize());
							}

						}

					}

				}
			} else
				newMap.put(currentClusterId, currentCluster);
		}
		clusterMap = newMap;
	}


	public static void main(String[] args) throws Exception {
		String fileName = "vectors.txt";
		// Global.initializeDataBaseSimulator(fileName);
		ArrayList<DataPoint> points = new ArrayList<DataPoint>();
		ArrayList<Association> left = new ArrayList<Association>();
		ArrayList<Association> associations = new ArrayList<Association>();
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line;
		int i = 1;
		Global.DBSIMULATOR  = new HashMap<String, DataPoint>();
		while ((line = reader.readLine()) != null) {
			System.out.println(i++);
			DataPoint point = new DataPoint(line);
			Cluster seed = new Cluster(point);
			Core.clusterMap.put(seed.getID(),seed);
			Global.DBSIMULATOR.put(point.getID(), point);
			associations = Core.getIncrementalFRange(point, points);
			ArrayList<Association> fClusterAssociations = Core
					.getFRangeClusterAssociation(associations, point);
			points.add(point);
			TreeSet<Association> result = Core.mergeArrays(associations,
					fClusterAssociations, left);
			left = Core.incremantalMergeCluster(result);
			left = Core.refineStepOne(left);
			Core.refineClusters();
		}
		int counter=0;
		for (String clusterId : clusterMap.keySet()) {
			Cluster current = Core.clusterMap.get(clusterId);
			System.out.println(current.getDataPointSet().size());
			if(current.getDataPointSet().size()>1)
				counter+=current.getDataPointSet().size();
			for (String s : current.getDataPointSet()) {
//				DataPoint sPoint = Global.DBSIMULATOR.get(s);
//				System.out.println(sPoint.getID()+" || "+sPoint.getMin());
//				for(String t : current.getDataPointSet())
//				{
//					if(!s.equals(t))
//					{
//						DataPoint tPoint = Global.DBSIMULATOR.get(t);
//						System.out.println(sPoint.getID()+" || "+tPoint.getID()+" || "+sPoint.calculateDistance(tPoint));
//					}
//				}
				System.out.println(Global.DBSIMULATOR.get(s).getUrl());
			}
			System.out.println("------------------------");
		}
		System.out.println(counter);
		//
		// ArrayList<Cluster> clusters = new ArrayList<Cluster>();
		// for (String clusterId : clusterMap.keySet()){
		// clusters.add(Core.clusterMap.get(clusterId));
		// }
		// Collections.sort(clusters,new Comparator<Cluster>() {
		//
		// @Override
		// public int compare(Cluster o1, Cluster o2) {
		// // TODO Auto-generated method stub
		// return (o1.getSize()-o2.getSize());
		// }
		// });
		// ArrayList<String> temp = new ArrayList<String>();
		//
		// for(Cluster c : clusters)
		// temp.add(c.getID()+" || "+c.getSize());
		// Collections.sort(temp);
		// for(String s : temp)
		// System.out.println(s);

		// HashMap<Integer, String> patternsMap =
		// Core.loadDataFromFile(fileName);
		//
		// Display display = new Display();
		// JFreeChart chart1 = SWTBarChartDemo.createChart(clusters,
		// patternsMap,
		// 8000);
		// Shell shell1 = new Shell(display);
		// shell1.setSize(1000, 800);
		// shell1.setLayout(new FillLayout());
		// shell1.setText("Draw Mitosis Clusters");
		// ChartComposite frame1 = new ChartComposite(shell1, SWT.NONE, chart1,
		// true);
		//
		// frame1.pack();
		// shell1.open();
		// while (!shell1.isDisposed()) {
		// if (!display.readAndDispatch())
		// display.sleep();
		// }

	}
}
