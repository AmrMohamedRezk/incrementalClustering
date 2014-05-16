import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;


public class Global {
	public final static double K = 1.275;
	public final static double F = 1.275;
	public final static double MAX_DISTANCE_POSSIBLE= 1;
	public final static double MAX_ASSOCIATION_DISTANCE =0.65 ;
	public static HashMap<String, DataPoint> DBSIMULATOR;
	
	public static void initializeDataBaseSimulator(String fileName) throws Exception
	{
		DBSIMULATOR = new HashMap<String, DataPoint>();
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line="";
		
		while((line=reader.readLine())!=null)
		{
			DataPoint current = new DataPoint(line);
			DBSIMULATOR.put(current.getID(),current);
		}
	}
}
