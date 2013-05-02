package filters.reeding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import acids2.Couple;
import acids2.Property;
import acids2.Resource;
import filters.WeightedNgramFilter;

/**
 * 
 * @author Axel Ngonga <ngonga@informatik.uni-leipzig.de>
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class NewNgFilter extends WeightedNgramFilter {
		
	public NewNgFilter(Property p) {
		super();
		property = p;
//		getWeights().put("abc", 0.2);
//		getWeights().put("xyz", 0.4);
	}
	
	@Override
	public TreeSet<Couple> filter(TreeSet<Couple> intersection,
			String propertyName, double theta) {
		
		TreeSet<Couple> results = new TreeSet<Couple>();

		long start = System.currentTimeMillis();
		
		double tmu = theta * getMinWeight();
		double upper = (2 - tmu) / tmu;
		double lower = tmu / (2 - tmu);
		
		for(Couple c : intersection) {
			Resource s = c.getSource(), t = c.getTarget();
			ArrayList<String> ng0s = removeZeros(getNgrams(s.getPropertyValue(propertyName), n));
			ArrayList<String> ng0t = removeZeros(getNgrams(t.getPropertyValue(propertyName), n));
			// length-aware filter
			String src = s.getPropertyValue(propertyName), tgt = t.getPropertyValue(propertyName);
			int ngs = ng0s.size(), ngt = ng0t.size();
			double s1 = mw(src), s0 = sw(src);
			if(ngs <= ngt * upper && ngs >= ngt * lower) {
				// refined length-aware filter
				double t1 = mw(tgt), t0 = sw(tgt);
				double upper2 = (2 * t0 - theta * s1) / (theta * t1);
				double lower2 = (theta * t1) / (2 * t0 - theta * s1);
				if(ngs <= ngt * upper2 && ngs >= ngt * lower2) {
					// index-based filter
					double k = theta * Math.min(s0, t0) / 2 * (ngs + ngt);
					ArrayList<String> share = intersect(ng0s, ng0t);
					if(share.size() >= k) {
						// similarity calculation
						double sim = this.getDistance(src, tgt);
						if(sim >= theta) {
							c.setDistance(sim, this.property.getIndex());
							results.add(c);
						}
					}
				}
			}
		}
		
		if(this.isVerbose()) {
			double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
			System.out.println("NewNG: Join done in "+compTime+" seconds.");
		}	
		
		return results;
	}

	@Override
	public TreeSet<Couple> filter(TreeSet<Resource> sources,
			TreeSet<Resource> targets, String propertyName, double theta) {

		TreeSet<Couple> results = new TreeSet<Couple>();

		long start = System.currentTimeMillis();
				
		double tmu = theta * getMinWeight();
		double upper = (2 - tmu) / tmu;
		double lower = tmu / (2 - tmu);
		
		HashMap<Resource, ArrayList<String>> ngLs = new HashMap<Resource, ArrayList<String>>();
		for(Resource s : sources) {
			ArrayList<String> ng0s = removeZeros(getNgrams(s.getPropertyValue(propertyName), n));
			ngLs.put(s, ng0s);
		}
		HashMap<Resource, ArrayList<String>> ngLt = new HashMap<Resource, ArrayList<String>>();
		for(Resource t : targets) {
			ArrayList<String> ng0t = removeZeros(getNgrams(t.getPropertyValue(propertyName), n));
			ngLt.put(t, ng0t);
		}
		for(Resource s : sources) {
			// length-aware filter
			String src = s.getPropertyValue(propertyName);
			ArrayList<String> ng0s = ngLs.get(s);
			int ngs = ng0s.size();
			double s1 = mw(src), s0 = sw(src);
			for(Resource t : targets) {
				String tgt = t.getPropertyValue(propertyName);
				ArrayList<String> ng0t = ngLt.get(t);
				int ngt = ng0t.size();
				if(ngs <= ngt * upper && ngs >= ngt * lower) {
					// refined length-aware filter
					double t1 = mw(tgt), t0 = sw(tgt);
					double upper2 = (2 * t0 - theta * s1) / (theta * t1);
					double lower2 = (theta * t1) / (2 * t0 - theta * s1);
					if(ngs <= ngt * upper2 && ngs >= ngt * lower2) {
						// index-based filter
						double k = theta * Math.min(s0, t0) / 2 * (ngs + ngt);
						ArrayList<String> share = intersect(ng0s, ng0t);
						if(share.size() >= k) {
							// similarity calculation
							double sim = this.getDistance(src, tgt);
							if(sim >= theta) {
								Couple c = new Couple(s, t);
								c.setDistance(sim, this.property.getIndex());
								results.add(c);
							}
						}
					}
				}
			}
		}
		
		if(this.isVerbose()) {
			double compTime = (System.currentTimeMillis()-start)/1000.0;
			System.out.println("NewNG: Join done in "+compTime+" seconds.");
		}
		
		return results;
	}
	
	private ArrayList<String> removeZeros(ArrayList<String> ngrams) {
		ArrayList<String> output = new ArrayList<String>();
		for(String ng : ngrams)
			if(getWeight(ng) > 0)
				output.add(ng);
		return output;
	}


	private double sw(String s) {
		ArrayList<String> ngs = getNgrams(s, n);
		double min = 1;
		for(String ng : ngs) {
			double w = getWeight(ng);
			if(w < min)
				min = w;
		}
		return min;
	}
	
	private double mw(String s) {
		ArrayList<String> ngs = getNgrams(s, n);
		double max = 1;
		for(String ng : ngs) {
			double w = getWeight(ng);
			if(w > max)
				max = w;
		}
		return max;
	}


}
