package filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

public abstract class WeightedNgramFilter extends StandardFilter {

	/**
	 * The "n" of n-grams. Default is trigram.
	 */
	protected int n = 3;
	
	/**
	 * Update interval for weights.
	 */
	private final double DELTA = 0.05;
	
	private HashMap<String, Double> weights = new HashMap<String, Double>();
	private TreeSet<String> ngCacheIncrease = new TreeSet<String>();
	private TreeSet<String> ngCacheDecrease = new TreeSet<String>();
	
	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}

	public double distance(String s1, String s2, int n) {
		ArrayList<String> ng1 = getNgrams(s1, n);
		ArrayList<String> ng2 = getNgrams(s2, n);
		ArrayList<String> ngint = intersect(ng1, ng2);
		double w1 = 0.0, w2 = 0.0, wint = 0.0;
		for(String ng : ng1)
			w1 += getWeight(ng);
		for(String ng : ng2)
			w2 += getWeight(ng);
		for(String ng : ngint)
			wint += getWeight(ng);
		if(w1+w2 == 0)
			return 1.0;
		return 2 * wint / (w1 + w2);
	}

	protected ArrayList<String> intersect(ArrayList<String> set1, ArrayList<String> set2) {
		ArrayList<String> intset = new ArrayList<String>(set1);
		ArrayList<String> temp = new ArrayList<String>();
		for(String s : set2)
			temp.add(s);
	    Iterator<String> e = intset.iterator();
	    while (e.hasNext()) {
	    	String item = e.next();
	        if (!temp.contains(item))
		        e.remove();
	        else
	        	temp.remove(item);
	    }
	    return intset;
	}
	
	public static ArrayList<String> getNgrams(String s, int n) {
		s = s.toLowerCase();
		ArrayList<String> ngrams = new ArrayList<String>();
		for(int i=0; i<n-1; i++)
			s = "-" + s + "-";
		for(int i=0; i<=s.length()-n; i++)
			ngrams.add(s.substring(i, i+n));
		return ngrams;
	}

	protected double getWeight(String ng) {
		Double d = weights.get(ng.toLowerCase());
		if(d == null)
			return 1.0;
		else
			return d;
	}
	
	public void prepareNgCache(String s1, String s2, boolean increase, int n) {
		ArrayList<String> ng1 = getNgrams(s1, n);
		ArrayList<String> ng2 = getNgrams(s2, n);
		if(increase)
			ngCacheIncrease.addAll(intersect(ng1, ng2));
		else
			ngCacheDecrease.addAll(intersect(ng1, ng2));
	}
	
	public void updateWeights() {
		for(String ng : ngCacheIncrease) {
			Double d = getWeight(ng);
			double dnew = d + DELTA;
			weights.put(ng, dnew);
//			System.out.println("COST("+ng+") = "+dnew);
		}
		for(String ng : ngCacheDecrease) {
			Double d = getWeight(ng);
			double dnew = d - DELTA;
			if(dnew < 0) dnew = 0;
			weights.put(ng, dnew);
//			System.out.println("COST("+ng+") = "+dnew);
		}
//		// normalization. may be optimized: if(!ngCacheIncrease.isEmpty()) max = 1.0 + DELTA; else skip norm.
//		double max = Double.NEGATIVE_INFINITY;
//		for(Double d : weights.values())
//			if(d > max)
//				max = d;
//		for(String key : weights.keySet())
//			weights.put(key, weights.get(key) / max);
		
		ngCacheIncrease.clear();
		ngCacheDecrease.clear();
	}

	public HashMap<String, Double> getWeights() {
		return weights;
	}
	
	public double getMinWeight() {
		double min = 1.0;
		for(double d : weights.values())
			if(d < min)
				min = d;
		return min;
	}

	public double getMaxWeight() {
		double max = 1.0;
		for(double d : weights.values())
			if(d > max)
				max = d;
		return max;
	}
	
	public void setWeights(HashMap<String, Double> weights) {
		this.weights = weights;
	}

	
	@Override
	public double getDistance(String sp, String tp) {
		return distance(sp, tp, n);
	}

}
