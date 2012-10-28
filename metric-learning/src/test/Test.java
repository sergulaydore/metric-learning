package test;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import com.aliasi.spell.WeightedEditDistance;

import metriclearning.Couple;
import metriclearning.Resource;
import similarities.WeightedEditDistanceExtended;
import utility.SystemOutHandler;
import algorithms.edjoin.EdJoinPlus;
import algorithms.edjoin.Entry;
import au.com.bytecode.opencsv.CSVReader;
import filters.ourapproach.OurApproachFilter;
import filters.passjoin.PassJoin;


public class Test {

	private static ArrayList<Resource> sources = new ArrayList<Resource>();
	private static ArrayList<Resource> targets = new ArrayList<Resource>();

    private static void loadKnowledgeBases(String sourcePath, String targetPath) throws IOException {
    	
    	sources.clear();
    	targets.clear();
    	
	    String[] ignoredList = {"id", "ID"};
    	
        CSVReader reader = new CSVReader(new FileReader(sourcePath));
        String [] titles = reader.readNext(); // gets the column titles
        String [] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            Resource r = new Resource(nextLine[0]);
            for(int i=0; i<nextLine.length; i++)
                if(!isIgnored(titles[i].toLowerCase(), ignoredList)) {
                    if(nextLine[i] != null)
                        r.setPropertyValue(titles[i], nextLine[i]);
                    else
                        r.setPropertyValue(titles[i], "");
                }
            sources.add(r);
        }
        
        reader = new CSVReader(new FileReader(targetPath));
        titles = reader.readNext(); // gets the column titles
        while ((nextLine = reader.readNext()) != null) {
            Resource r = new Resource(nextLine[0]);
            for(int i=0; i<nextLine.length; i++)
                if(!isIgnored(titles[i].toLowerCase(), ignoredList)) {
                    if(nextLine[i] != null)
                        r.setPropertyValue(titles[i], nextLine[i]);
                    else
                        r.setPropertyValue(titles[i], "");
                }
            targets.add(r);
        }
        
        reader.close();
    }
    
    private static boolean isIgnored(String title, String[] ignoredList) {
        for(String ign : ignoredList) {
            if(title.equals(ign))
                return true;
        }
        return false;
    }

    private static void testPassJoinThresholds(String dataset, String propertyName) throws IOException {
		
		System.out.println(sources.size());

	    TreeSet<Couple> passjResults = null;
	    
		for(int θ=0; θ<=5; θ++) {
	
			long start = System.currentTimeMillis();
			
			passjResults = PassJoin.passJoin(sources, targets, propertyName, θ);
			
			double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
			System.out.println("θ = "+θ+"\t\tΔt = "+compTime+"\t\t|R| = "+passjResults.size());
		}
		
	}

	private static void testEDJoinThresholds(String dataset, String propertyName) throws IOException {

        TreeSet<Entry> sTree = new TreeSet<Entry>();
        for(Resource s : sources)
            sTree.add(new Entry(s.getID(), s.getPropertyValue(propertyName)));
        TreeSet<Entry> tTree = new TreeSet<Entry>();
        for(Resource s : targets)
            tTree.add(new Entry(s.getID(), s.getPropertyValue(propertyName)));

		System.out.println(sources.size());
		
		TreeSet<String> edjResults = null;
		
		for(int θ=0; θ<=5; θ++) {
			
			long start = System.currentTimeMillis();

			SystemOutHandler.shutDown();
            edjResults = EdJoinPlus.runOnEntries(0, θ, sTree, tTree);
            SystemOutHandler.turnOn();

    		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
    		System.out.println("θ = "+θ+"\t\tΔt = "+compTime+"\t\t|R| = "+edjResults.size());
		}
	}

	private static void crossValidation(String dataset, String propertyName) throws IOException {
		System.out.println("PassJoin");
		TreeSet<Couple> pj = testPassJoinOnce(propertyName, 1);
		
		System.out.println("EDJoin");
		TreeSet<String> ej = testEdJoinOnce(propertyName, 1);

		// Cross-validation.
		int i = 0;
		for(Couple c : pj) {
			i++;
			if(!ej.contains( c.getSource().getID()+"#"+c.getTarget().getID() ))
				System.out.println(i+". "+c.getSource().getID()
						+ "\t" + c.getTarget().getID()
						+ "\t" + c.getSource().getPropertyValue(propertyName)
						+ "\t" + c.getTarget().getPropertyValue(propertyName)
						+ "\t" + c.getSimilarities().get(0));
		}
		
		// (!) Cannot directly check the other way, because we would like to print the titles
		// and EDJoin returns only the IDs.
		
		for(String s : ej) {
			String[] ss = s.split("#");
			Couple c = new Couple(new Resource(ss[0]), new Resource(ss[1]));
			if(!pj.contains(c))
				System.out.println(c.getSource().getID()+"#"+c.getTarget().getID());
		}
	}

	private static TreeSet<String> testEdJoinOnce(String propertyName, int θ) throws IOException {

        TreeSet<Entry> sTree = new TreeSet<Entry>();
        for(Resource s : sources)
            sTree.add(new Entry(s.getID(), s.getPropertyValue(propertyName)));
        TreeSet<Entry> tTree = new TreeSet<Entry>();
        for(Resource s : targets)
            tTree.add(new Entry(s.getID(), s.getPropertyValue(propertyName)));

		System.out.println(sources.size());
		
		TreeSet<String> edjResults = null;
					
		long start = System.currentTimeMillis();

		SystemOutHandler.shutDown();
        edjResults = EdJoinPlus.runOnEntries(θ, θ, sTree, tTree);
        SystemOutHandler.turnOn();

		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
		System.out.println("θ = "+θ+"\t\tΔt = "+compTime+"\t\t|R| = "+edjResults.size());
		
		return edjResults;
	}

	private static TreeSet<Couple> testPassJoinOnce(String propertyName, int θ) throws IOException {

		System.out.println(sources.size());

	    TreeSet<Couple> passjResults = null;
	    
		long start = System.currentTimeMillis();
		
		passjResults = PassJoin.passJoin(sources, targets, propertyName, θ);
		
		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
		System.out.println("θ = "+θ+"\t\tΔt = "+compTime+"\t\t|R| = "+passjResults.size());
		
		return passjResults;
	}

	private static TreeSet<Couple> testOurApproachFilter(String propertyName, double θ) throws IOException {

		System.out.println(sources.size());

	    TreeSet<Couple> oafResults = null;
	    
	    long start = System.currentTimeMillis();
		
		oafResults = OurApproachFilter.ourApproachFilter(sources, targets, propertyName, θ);
		
		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
		System.out.println("θ = "+θ+"\t\tΔt = "+compTime+"\t\t|R| = "+oafResults.size());
		
		return oafResults;
	}

	private static TreeSet<Couple> testPassJoin(String propertyName, double θ) throws IOException {

		System.out.println(sources.size());

	    TreeSet<Couple> passjResults = null;
	    
	    long start = System.currentTimeMillis();
		
		passjResults = PassJoin.passJoin(sources, targets, propertyName, θ);
		
		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
		System.out.println("θ = "+θ+"\t\tΔt = "+compTime+"\t\t|R| = "+passjResults.size());
		
		return passjResults;
	}

	private static TreeSet<Couple> testQuadraticJoin(String propertyName, double θ) throws IOException {
		
		System.out.println(sources.size());

	    TreeSet<Couple> quadrResults = new TreeSet<Couple>();
	    
	    long start = System.currentTimeMillis();
		
	    WeightedEditDistanceExtended wed = new WeightedEditDistanceExtended() {
			@Override
			public double transposeWeight(char cFirst, char cSecond) {
				return Double.POSITIVE_INFINITY;
			}
			@Override
			public double substituteWeight(char cDeleted, char cInserted) {
				if((cDeleted >= 'A' && cDeleted <= 'Z' && cDeleted+32 == cInserted) || 
						(cDeleted >= 'a' && cDeleted <= 'z' && cDeleted-32 == cInserted))
					return 0.5;
				else return 1.0;
			}
			@Override
			public double matchWeight(char cMatched) {
				return 0.0;
			}
			@Override
			public double insertWeight(char cInserted) {
				return 1.0;
			}
			@Override
			public double deleteWeight(char cDeleted) {
				switch(cDeleted) {
				case 'i': case 'r': case 's': case 't': return 0.5;
				}
				return 1.0;
			}
		};  
	    
		for(Resource s : sources)
			for(Resource t : targets) {
				double d = wed.proximity(s.getPropertyValue(propertyName), t.getPropertyValue(propertyName));
				if(d <= θ)
					quadrResults.add(new Couple(s,t));
			}
		System.out.println(sources.size()*targets.size());
				
		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
		System.out.println("θ = "+θ+"\t\tΔt = "+compTime+"\t\t|R| = "+quadrResults.size());
		
		return quadrResults;
	}
	
	
	/**
		 * @param args
		 * @throws IOException 
		 */
		public static void main(String[] args) throws IOException {
			
//			Vector<Character> cs = new Vector<Character>();
//			cs.add('c'); cs.add('a'); cs.add('l'); cs.add('l');
//			Vector<Character> ct = new Vector<Character>();
//			ct.add('l');
//			 
//			Vector<Character> ctemp = new Vector<Character>(cs);
//			cs.retainAll(ct);
//			
//			for(char c:cs)
//				System.out.println(c);
//			
//			System.exit(0);
			
			String dataset = "data/1-dblp-acm/sources.csv", pname = "title";
		    loadKnowledgeBases(dataset, dataset);
			
			TreeSet<Couple> pj = testPassJoin(pname, 1.0);
			TreeSet<String> pjs = new TreeSet<String>();
			for(Couple c : pj) {
//				System.out.println(c.getSource().getID()
//						+ "\t" + c.getTarget().getID()
//						+ "\t" + c.getSource().getOriginalPropertyValue(pname)
//						+ "\t" + c.getTarget().getOriginalPropertyValue(pname)
//						+ "\td = " + c.getDistances().get(0));
				pjs.add(c.getSource().getOriginalPropertyValue(pname)+"#"+
						c.getTarget().getOriginalPropertyValue(pname));
			}
			
			TreeSet<Couple> oaf = testOurApproachFilter(pname, 1.0);
			TreeSet<String> oafs = new TreeSet<String>();
			for(Couple c : oaf) {
//				System.out.println(c.getSource().getID()
//						+ "\t" + c.getTarget().getID()
//						+ "\t" + c.getSource().getOriginalPropertyValue(pname)
//						+ "\t" + c.getTarget().getOriginalPropertyValue(pname)
//						+ "\td = " + c.getDistances().get(0));
				oafs.add(c.getSource().getOriginalPropertyValue(pname)+"#"+
						c.getTarget().getOriginalPropertyValue(pname));
			}

			System.out.println("\nPJ but not OAF");
			for(String s : pjs)
				if(!oafs.contains(s))
					System.out.println(s);

			System.out.println("\nOAF but not PJ");
			for(String s : oafs)
				if(!pjs.contains(s))
					System.out.println(s);

	//		testPassJoin("data/0-paper/sources.csv", "name");
	//		testEDJoin("data/2-dblp-scholar/Scholar.csv", "title");
	//		crossValidation("data/2-dblp-scholar/Scholar.csv", "title");
			
//			testQuadraticJoin(pname, 1.0);
			
		}

}
