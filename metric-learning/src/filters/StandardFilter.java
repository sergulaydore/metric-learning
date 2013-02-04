package filters;

import java.util.TreeSet;

import acids2.Couple;
import acids2.Resource;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public interface StandardFilter {
	
	public TreeSet<Couple> filter(TreeSet<Resource> sources,
			TreeSet<Resource> targets, String propertyName, double theta);

	public TreeSet<Couple> filter(TreeSet<Couple> intersection, String propertyName, double theta);

	public double getDistance(String sp, String tp);
	
}
