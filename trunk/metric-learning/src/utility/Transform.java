package utility;

public class Transform {

	private static double degree = 1.0;
	
	public static double toSimilarity(double d) {
		return 1.0 / Math.pow(1.0 + d, 1.0 / degree);
	}
	
	public static double toDistance(double s) {
		return 1.0 / Math.pow(s, degree) - 1.0;
	}
	
	public static double toDistanceInterval(double ds, double s0, double s1) {
		if(ds == 0.0)
			return 0.0;
		return ds / s0 / s1;
	}
	
}
