package utility;

public class Transform {

	private static double degree = 1;
	
	public static double toSimilarity(double d) {
		return 1.0 / Math.pow(1.0 + d, 1.0 / degree);
	}
	
	public static double toDistance(double s) {
		return 1.0 / Math.pow(s, degree) - 1.0;
	}
	
}
