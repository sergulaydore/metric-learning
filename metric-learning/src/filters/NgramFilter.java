package filters;

import java.util.TreeSet;

import acids2.Couple;
import acids2.Resource;

public class NgramFilter implements StandardFilter {

	@Override
	public TreeSet<Couple> filter(TreeSet<Resource> sources,
			TreeSet<Resource> targets, String propertyName, double theta) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TreeSet<Couple> filter(TreeSet<Couple> intersection,
			String propertyName, double theta) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getDistance(String sp, String tp) {
		// TODO Auto-generated method stub
		return 0;
	}

}
