package filters.reeding;

import java.util.TreeSet;
import java.util.Vector;

import utility.Transform;

import acids2.Couple;
import acids2.Resource;
import filters.WeightedEditDistanceFilter;
import filters.WeightedNgramFilter;

/**
 * Rapid Execution of weightED N-Grams (REEDiNG) filter.
 *  
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class HybridFilter extends WeightedNgramFilter {
	
	public HybridFilter() {
		super();
	}
	
	@Override
	public TreeSet<Couple> filter(TreeSet<Couple> intersection,
			String propertyName, double theta) {
		
		TreeSet<Couple> results = new TreeSet<Couple>();

		double tau = Transform.toDistance(theta) / WeightedEditDistanceFilter.INIT_CASE_WEIGHT; // this.getMinWeight();
		System.out.println("theta = "+theta+"\ttau = "+tau);
		
		long start = System.currentTimeMillis();
		for(Couple c : intersection)
			reededCore(c.getSource(), c.getTarget(), propertyName, tau, theta, results);
		
		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
		System.out.println("REEDED: Join done in "+compTime+" seconds.");
		
		return results;
	}

	@Override
	public TreeSet<Couple> filter(TreeSet<Resource> sources,
			TreeSet<Resource> targets, String propertyName, double theta) {
		
		TreeSet<Couple> results = new TreeSet<Couple>();

		double tau = Transform.toDistance(theta) / WeightedEditDistanceFilter.INIT_CASE_WEIGHT; // this.getMinWeight();
		System.out.println("theta = "+theta+"\ttau = "+tau);
		
		long start = System.currentTimeMillis();
		
		for(Resource s : sources)
			for(Resource t : targets)
				reededCore(s, t, propertyName, tau, theta, results);
		
		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
		System.out.println("REEDED: Join done in "+compTime+" seconds.");
		
		return results;
	}
	
	private static Vector<Character> subtract(Vector<Character> cs, Vector<Character> ct) {
		Vector<Character> res = new Vector<Character>(cs);
		for(Character c1 : ct)
			res.remove(c1);
		return res;
	}
	
	private void reededCore(Resource s, Resource t, String propertyName, 
			double tau, double theta, TreeSet<Couple> results) {
		String sp = s.getPropertyValue(propertyName);
		String tp = t.getPropertyValue(propertyName);
		if(Math.abs(sp.length() - tp.length()) <= tau) {
			Vector<Character> cs = new Vector<Character>();
			for(int i=0; i<sp.length(); i++)
				cs.add(sp.charAt(i));
			Vector<Character> ct = new Vector<Character>();
			for(int i=0; i<tp.length(); i++)
				ct.add(tp.charAt(i));
			Vector<Character> ctot = subtract(cs, ct);
			ctot.addAll(subtract(ct, cs));
			double minGap = (int)(ctot.size() / 2);// + (size % 2) * minInsdel;
			if(minGap <= tau) {
				//  Verification.
				double sim = this.getDistance(sp, tp);
				if(sim >= theta) {
					Couple c = new Couple(s, t);
					c.addDistance(sim);
					results.add(c);
				}
			}
		}
	}


}
