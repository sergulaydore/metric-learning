package filters.mahalanobis;

import java.util.ArrayList;
import java.util.TreeSet;

import utility.ValueParser;
import acids2.Couple;
import acids2.Resource;
import filters.StandardFilter;

public class MahalaFilter implements StandardFilter {

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
		// TODO transform threshold into similarity: theta_sigma = phi(theta_delta)
//		if(d <= theta) {
			Couple cpl = new Couple(s, t);
			cpl.addDistance(d);
			results.add(cpl);
//		}
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

	public void setExtrema(ArrayList<Double> ext) {
		this.extrema = ext;
	}

	public ArrayList<Double> getExtrema() {
		return extrema;
	}

}
