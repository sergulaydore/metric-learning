package distances;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

public class WeightedNgram {

	/**
	 * Update interval for weights.
	 */
	private static final double DELTA = 0.2;
	private static HashMap<String, Double> weights = new HashMap<String, Double>();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		double d = distance("Ngonga A.", "Ngonga A., Soru T.", 3);
		System.out.println("distance = "+d);
	}
	
	public static double distance(String s1, String s2) {
		// default is trigram
		return distance(s1, s2, 3);
	}

	public static double distance(String s1, String s2, int n) {
		s1 = s1.toLowerCase().trim();
		s2 = s2.toLowerCase().trim();
		TreeSet<String> ng1 = getNgrams(s1, n);
		TreeSet<String> ng2 = getNgrams(s2, n);
		TreeSet<String> ngint = intersect(ng1, ng2);
		double w1 = 0.0, w2 = 0.0, wint = 0.0;
		for(String ng : ng1)
			w1 += getWeight(ng);
		for(String ng : ng2)
			w2 += getWeight(ng);
		for(String ng : ngint) {
//			System.out.println(ng);
			wint += getWeight(ng);
		}
		double sigma = 2 * wint / (w1 + w2);
		return sigma;
	}

	private static TreeSet<String> intersect(TreeSet<String> set1, TreeSet<String> set2) {
		TreeSet<String> intset = new TreeSet<String>(set1);
	    Iterator<String> e = intset.iterator();
	    while (e.hasNext()) {
	    	String item = e.next();
	        if (!set2.contains(item))
		        e.remove();
	    }
	    return intset;
	}
	
	private static TreeSet<String> getNgrams(String s, int n) {
		TreeSet<String> ngrams = new TreeSet<String>();
		for(int i=0; i<n-1; i++)
			s = "-" + s + "-";
		for(int i=0; i<=s.length()-n; i++)
			ngrams.add(s.substring(i, i+n));
		return ngrams;
	}


	private static double getWeight(String ng) {
		Double d = weights.get(ng);
		if(d == null)
			return 1.0;
		else
			return d;
	}
	
	public static void increaseWeight(String ng) {
		Double d = weights.get(ng);
		double dnew = d + DELTA;
		weights.put(ng, dnew);
		System.out.println("COST("+ng+") = "+dnew);
	}
	
	public static void decreaseWeight(String ng) {
		Double d = weights.get(ng);
		double dnew = d - DELTA;
		if(dnew < 0) dnew = 0;
		weights.put(ng, dnew);
		System.out.println("COST("+ng+") = "+dnew);
	}

}
