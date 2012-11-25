package filters.edjoin;

import java.util.ArrayList;
import java.util.TreeSet;

import metriclearning.Resource;
import test.Test;
import utility.SystemOutHandler;
import algorithms.edjoin.EdJoinPlus;
import algorithms.edjoin.Entry;
import filters.StandardFilter;

public class EdJoinFilter extends StandardFilter {

	public static TreeSet<String> edJoinFilter(ArrayList<Resource> sources,
			ArrayList<Resource> targets, String propertyName, double θ) {
		
		loadCaseWeights();
		loadConfusionMatrix();
		
		TreeSet<Entry> sTree = new TreeSet<Entry>();
        for(Resource s : sources)
            sTree.add(new Entry(s.getID(), s.getPropertyValue(propertyName)));
        TreeSet<Entry> tTree = new TreeSet<Entry>();
        for(Resource s : targets)
            tTree.add(new Entry(s.getID(), s.getPropertyValue(propertyName)));
        
		TreeSet<String> results = new TreeSet<String>();

		int tau = (int) (θ / getMinWeight());
		
	    long start = System.currentTimeMillis();

		SystemOutHandler.shutDown();
        results = EdJoinPlus.runOnEntries(0, tau, sTree, tTree);
        SystemOutHandler.turnOn();
        
		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
		System.out.print(compTime+"\t");
		Test.append(compTime+"\t");
		
//		System.out.println("count = "+count);
		return results;
	}

}
