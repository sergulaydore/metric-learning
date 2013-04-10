package filters.mahalanobis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import utility.Transform;
import utility.ValueParser;
import acids2.Couple;
import acids2.Resource;
import filters.StandardFilter;

public class MahalaFilter extends StandardFilter {

	private ArrayList<Double> extrema = new ArrayList<Double>();
	
	// TODO introduce weights and true Mahalanobis calculation
	
	public MahalaFilter() {
	}
	
	@Override
	public TreeSet<Couple> filter(TreeSet<Resource> sources, TreeSet<Resource> targets, 
			String pname, double theta) {
		TreeSet<Couple> results = new TreeSet<Couple>();
		for(Resource s : sources)
			for(Resource t : targets)
				mahalaCore(s, t, pname, theta, results);
		return results;
	}

	@Override
	public TreeSet<Couple> filter(TreeSet<Couple> intersection, String pname, double theta) {
		TreeSet<Couple> results = new TreeSet<Couple>();
		for(Couple c : intersection)
			mahalaCore(c.getSource(), c.getTarget(), pname, theta, results);
		return results;
	}

	private void mahalaCore(Resource s, Resource t, String pname, double theta, TreeSet<Couple> results) {
		String sp = s.getPropertyValue(pname);
		String tp = t.getPropertyValue(pname);
		double d = getDistance(sp, tp);
		double theta_d = Transform.toDistance(theta);
		// couples should be discarded only when there is information about them
		if(d <= theta_d || Double.isNaN(d)) {
			Couple c = new Couple(s, t);
			// distance values are then normalized into [0,1]
			c.addDistance(d);
			results.add(c);
		}
	}

	@Override
	public double getDistance(String sp, String tp) {
		double sd = ValueParser.parse(sp);
		double td = ValueParser.parse(tp);
		if(Double.isNaN(sd) || Double.isNaN(td))
			return Double.NaN;
		else
			return Math.abs(sd-td);
	}

	@Override
	public HashMap<String, Double> getWeights() {
		return new HashMap<String, Double>();
	}
	
	public void setExtrema(ArrayList<Double> ext) {
		this.extrema = ext;
	}

	public ArrayList<Double> getExtrema() {
		return extrema;
	}

}
