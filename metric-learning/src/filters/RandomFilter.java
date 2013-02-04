package filters;

import java.util.ArrayList;
import java.util.TreeSet;

import acids2.Couple;
import acids2.Resource;

// TODO move to dead code
public class RandomFilter extends WeightedEditDistanceFilter {

	@Override
	public TreeSet<Couple> filter(TreeSet<Resource> sources,
			TreeSet<Resource> targets, String propertyName, double theta) {
		TreeSet<Couple> results = new TreeSet<Couple>();
		ArrayList<Resource> src = new ArrayList<Resource>(sources);
		ArrayList<Resource> tgt = new ArrayList<Resource>(targets);
		for(int i=0; i<10000; i++) {
			Resource s = src.get((int)(src.size()*Math.random()));
			Resource t = tgt.get((int)(tgt.size()*Math.random()));
			Couple c = new Couple(s, t);
			c.addDistance(getDistance(s.getPropertyValue(propertyName), t.getPropertyValue(propertyName)));
			results.add(c);
		}
		return results;
	}

	@Override
	public TreeSet<Couple> filter(TreeSet<Couple> intersection,
			String propertyName, double theta) {
		TreeSet<Couple> results = new TreeSet<Couple>();
		for(Couple c : intersection) {
			Couple c1 = new Couple(c.getSource(), c.getTarget());
			c1.addDistance(getDistance(c.getSource().getPropertyValue(propertyName),
					c.getTarget().getPropertyValue(propertyName)));
			results.add(c1);
		}
		return results;
	}

}
