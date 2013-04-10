package filters;

import java.util.HashMap;
import java.util.TreeSet;

import acids2.Couple;
import acids2.Resource;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public abstract class StandardFilter {
	
	public abstract TreeSet<Couple> filter(TreeSet<Resource> sources,
			TreeSet<Resource> targets, String propertyName, double theta);
	
	public abstract TreeSet<Couple> filter(TreeSet<Couple> intersection, String propertyName, double theta);
	
	public abstract double getDistance(String sp, String tp);
	
	public abstract HashMap<String, Double> getWeights();
	
}
