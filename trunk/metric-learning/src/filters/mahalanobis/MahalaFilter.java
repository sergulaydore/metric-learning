package filters.mahalanobis;

import java.util.TreeSet;

import acids2.Couple;
import acids2.Resource;

public class MahalaFilter {

	// TODO introduce weights and true Mahalanobis calculation
	
	public static TreeSet<Couple> filter(TreeSet<Couple> input, String propertyName, double θ) {
		
		TreeSet<Couple> results = new TreeSet<Couple>();

		for(Couple c : input) {
			Resource s = c.getSource();
			Resource t = c.getTarget();
			double sp = Double.parseDouble(s.getPropertyValue(propertyName));
			double tp = Double.parseDouble(t.getPropertyValue(propertyName));
			double d = Math.abs(sp-tp);
			if(d <= θ) {
				Couple cpl = new Couple(s, t);
				cpl.addDistance(d);
				results.add(cpl);
			}
		}

		return results;
		
	}

}
