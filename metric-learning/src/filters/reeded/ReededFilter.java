package filters.reeded;

import java.util.TreeSet;
import java.util.Vector;

import acids2.Couple;
import acids2.Resource;
import filters.WeightedEditDistanceFilter;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class ReededFilter extends WeightedEditDistanceFilter {
	
	public ReededFilter() {
		super();
	}
	
	@Override
	public TreeSet<Couple> filter(TreeSet<Couple> intersection,
			String propertyName, double theta) {
		
		TreeSet<Couple> results = new TreeSet<Couple>();

		double mw = getMinWeight();
		double tau = theta / mw;
		
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

		double mw = getMinWeight();
		double tau = theta / mw;
		
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
			Vector<Character> c = subtract(cs, ct);
			c.addAll(subtract(ct, cs));
			double minGap = (int)(c.size() / 2);// + (size % 2) * minInsdel;
			if(minGap <= tau) {
				//  Verification.
				double d = this.getDistance(sp, tp);
				if(d <= theta) {
					Couple cpl = new Couple(s, t);
					cpl.addDistance(d);
					results.add(cpl);
				}
			}
		}
		
	}

	
}
