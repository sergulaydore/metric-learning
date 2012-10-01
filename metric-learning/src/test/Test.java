package test;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeSet;

import metriclearning.Couple;
import metriclearning.Resource;
import utility.SystemOutHandler;
import algorithms.edjoin.EdJoinPlus;
import algorithms.edjoin.Entry;
import au.com.bytecode.opencsv.CSVReader;
import filters.passjoin.PassJoin;


public class Test {

	private static ArrayList<Resource> sources = new ArrayList<Resource>();
	private static ArrayList<Resource> targets = new ArrayList<Resource>();

    private static void loadKnowledgeBases(String sourcePath, String targetPath) throws IOException {
    	
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

    private static void testPassJoin(String dataset, String propertyName) throws IOException {
		
	    loadKnowledgeBases(dataset, dataset);

		System.out.println(sources.size());

	    TreeSet<Couple> passjResults = null;
	    
		for(int tau=0; tau<=5; tau++) {
	
			long start = System.currentTimeMillis();
			
			passjResults = PassJoin.passJoin(sources, targets, propertyName, tau, 0);
			
			double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
			System.out.println(tau+"\t"+compTime+"\t"+passjResults.size());
		}
	}

	private static void testEDJoin(String dataset, String propertyName) throws IOException {

	    loadKnowledgeBases(dataset, dataset);

        TreeSet<Entry> sTree = new TreeSet<Entry>();
        for(Resource s : sources)
            sTree.add(new Entry(s.getID(), s.getPropertyValue(propertyName)));
        TreeSet<Entry> tTree = new TreeSet<Entry>();
        for(Resource s : targets)
            tTree.add(new Entry(s.getID(), s.getPropertyValue(propertyName)));

		System.out.println(sources.size());
		
		TreeSet<String> edjResults = null;
		
		for(int tau=0; tau<=5; tau++) {
			
			long start = System.currentTimeMillis();

			SystemOutHandler.shutDown();
            edjResults = EdJoinPlus.runOnEntries(0, tau, sTree, tTree);
            SystemOutHandler.turnOn();

    		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
    		System.out.println(tau+"\t"+compTime+"\t"+edjResults.size());
		}
	}

	private static void crossValidation(String dataset, String propertyName) throws IOException {
		System.out.println("PassJoin");
		TreeSet<Couple> pj = testPassJoinOnce(dataset, propertyName, 1);
		
		System.out.println("EDJoin");
		TreeSet<String> ej = testEdJoinOnce(dataset, propertyName, 1);

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

	private static TreeSet<String> testEdJoinOnce(String dataset,
			String propertyName, int tau) throws IOException {
	    loadKnowledgeBases(dataset, dataset);

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
        edjResults = EdJoinPlus.runOnEntries(tau, tau, sTree, tTree);
        SystemOutHandler.turnOn();

		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
		System.out.println(tau+"\t"+compTime+"\t"+edjResults.size());
		
		sources.clear();
		targets.clear();

		return edjResults;
	}

	private static TreeSet<Couple> testPassJoinOnce(String dataset,
			String propertyName, int tau) throws IOException {
	    loadKnowledgeBases(dataset, dataset);

		System.out.println(sources.size());

	    TreeSet<Couple> passjResults = null;
	    
		long start = System.currentTimeMillis();
		
		passjResults = PassJoin.passJoin(sources, targets, propertyName, tau, tau);
		
		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
		System.out.println(tau+"\t"+compTime+"\t"+passjResults.size());
		
		sources.clear();
		targets.clear();

		return passjResults;
	}

	private static TreeSet<Couple> testPassJoin(String dataset,
			String propertyName, double theta, double beta) throws IOException {
		
		double tmin = theta-beta;
		double tmax = theta+beta;
		
	    loadKnowledgeBases(dataset, dataset);

		System.out.println(sources.size());

	    TreeSet<Couple> passjResults = null;
	    
		long start = System.currentTimeMillis();
		
		passjResults = PassJoin.passJoin(sources, targets, propertyName, theta-beta, theta+beta);
		
		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
		System.out.println("("+tmin+","+tmax+")\t"+compTime+"\t"+passjResults.size());
		
		sources.clear();
		targets.clear();

		return passjResults;
	}

	/**
		 * @param args
		 * @throws IOException 
		 */
		public static void main(String[] args) throws IOException {
			
			String pname = "title";
			TreeSet<Couple> pj = testPassJoin("data/1-dblp-acm/sources.csv", pname, 0.5, 0.1);
			
			int i = 0;
			System.out.println("printing non-equal strings only");
			for(Couple c : pj) {
				i++;
				if(c.getSimilarities().get(0) < 1.0)
					System.out.println(i+". "+c.getSource().getID()
							+ "\t" + c.getTarget().getID()
							+ "\t" + c.getSource().getOriginalPropertyValue(pname)
							+ "\t" + c.getTarget().getOriginalPropertyValue(pname)
							+ "\t" + c.getSimilarities().get(0));
			}
	//		testPassJoin("data/0-paper/sources.csv", "name");
	//		testEDJoin("data/2-dblp-scholar/Scholar.csv", "title");
	//		crossValidation("data/2-dblp-scholar/Scholar.csv", "title");
			
		}

}
