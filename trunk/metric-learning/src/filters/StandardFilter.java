package filters;

import java.util.HashMap;
import java.util.TreeSet;

import acids2.Couple;
import acids2.Property;
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

	protected boolean verbose = true;
	
	protected Property property = null;

	public Property getProperty() {
		return property;
	}

	public void setProperty(Property property) {
		this.property = property;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
}
