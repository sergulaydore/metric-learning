package utility;

public class ValueParser {

	public static double parse(String s) {
		if(s.equals(""))
			return Double.NaN;
//			return 0.0;
		return Double.parseDouble(s);
	}
	
}
