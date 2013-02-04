package filters.test;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.TreeSet;

import distances.WeightedEditDistanceExtended;

import utility.SystemOutHandler;
import acids2.Couple;
import acids2.Resource;
import algorithms.edjoin.EdJoinPlus;
import algorithms.edjoin.Entry;
import au.com.bytecode.opencsv.CSVReader;
import filters.edjoin.EdJoinFilter;
import filters.passjoin.PassJoin;
import filters.reeded.ReededFilter;


public class FiltersTest {

	private static TreeSet<Resource> sources = new TreeSet<Resource>();
	private static TreeSet<Resource> targets = new TreeSet<Resource>();
	
	private static String sys_out = "\n";
	private static double THETA_MAX;

    private static void loadKnowledgeBases(String sourcePath, String targetPath) throws IOException {
    	loadKnowledgeBases(sourcePath, targetPath, 0, Integer.MAX_VALUE);
    }
    
    private static void clearKnowledgeBases() {
    	sources.clear();
    	targets.clear();
    	System.gc();
    }
    
    private static void loadKnowledgeBases(String sourcePath, String targetPath, int startOffset, int endOffset) throws IOException {
    	
	    String[] ignoredList = {"id", "ID"};
    	
        CSVReader reader = new CSVReader(new FileReader(sourcePath));
        String [] titles = reader.readNext(); // gets the column titles
        for(int i=0; i<startOffset; i++) // skips start offset
        	reader.readNext();
        String [] nextLine;
        int count = 0;
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
            if(++count >= endOffset)
            	break;
        }
        
        reader = new CSVReader(new FileReader(targetPath));
        titles = reader.readNext(); // gets the column titles
        for(int i=0; i<startOffset; i++) // skips offset
        	reader.readNext();
        count = 0;
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
            if(++count >= endOffset)
            	break;
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
	    
	    PassJoin pj = new PassJoin();
	    
		for(int θ=0; θ<=5; θ++) {
	
			long start = System.currentTimeMillis();
			
			passjResults = pj.passJoin(new ArrayList<Resource>(sources), new ArrayList<Resource>(targets),
					propertyName, θ);
			
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

	private static TreeSet<String> testEdJoinOnce(String propertyName, double θ) throws IOException {
		
		long start = System.currentTimeMillis();
		
//		System.out.println(sources.size());
		
		TreeSet<String> edjResults = null;
		
		EdJoinFilter ed = new EdJoinFilter();
		edjResults = ed.edJoinFilter(sources, targets, propertyName, θ);
		
        long now = System.currentTimeMillis();
		double compTime = (double)(now-start)/1000.0;
		
		System.out.println(θ+"\t"+compTime+"\t"+edjResults.size());
		sys_out += θ+"\t"+compTime+"\t"+edjResults.size()+"\n";
		
		return edjResults;
	}

	private static TreeSet<Couple> testPassJoinOnce(String propertyName, int θ) throws IOException {

		System.out.println(sources.size());

	    TreeSet<Couple> passjResults = null;
	    
		long start = System.currentTimeMillis();
		
		PassJoin pj = new PassJoin();
		passjResults = pj.passJoin(new ArrayList<Resource>(sources), new ArrayList<Resource>(targets),
				propertyName, θ);
		
		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
		System.out.println("θ = "+θ+"\t\tΔt = "+compTime+"\t\t|R| = "+passjResults.size());
		
		return passjResults;
	}

	private static TreeSet<Couple> testOurApproachFilter(String propertyName, double θ) throws IOException {

//		System.out.println(sources.size());

	    TreeSet<Couple> oafResults = null;
	    
	    long start = System.currentTimeMillis();
		
	    ReededFilter rf = new ReededFilter();
		oafResults = rf.filter(sources, targets, propertyName, θ);
		
		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
//		System.out.println("θ = "+θ+"\t\tΔt = "+compTime+"\t\t|R| = "+oafResults.size());
		System.out.println(θ+"\t"+compTime+"\t"+oafResults.size());
		sys_out += θ+"\t"+compTime+"\t"+oafResults.size()+"\n";
		
		return oafResults;
	}

	private static TreeSet<Couple> testPassJoin(String propertyName, double θ) throws IOException {

//		System.out.println(sources.size());

	    TreeSet<Couple> passjResults = null;
	    
	    long start = System.currentTimeMillis();
	    
	    PassJoin pj = new PassJoin();
		passjResults = pj.passJoin(new ArrayList<Resource>(sources), new ArrayList<Resource>(targets),
				propertyName, θ);
		
		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
//		System.out.println("θ = "+θ+"\t\tΔt = "+compTime+"\t\t|R| = "+passjResults.size());
		System.out.println(θ+"\t"+compTime+"\t"+passjResults.size());
		sys_out += θ+"\t"+compTime+"\t"+passjResults.size()+"\n";
		
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
	
    private static void notify(String s) {
    	
  		String sysout = "";
		try {
			sysout = URLEncoder.encode(s, "ISO-8859-1");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
        try {
            // Create a URL for the desired page
            URL url = new URL("http://mommi84.altervista.org/notifier/index.php?"
                    + "sysout="+sysout);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Read all the text returned by the server
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            in.close();
        } catch (IOException e) {
                e.printStackTrace();
        }
    }

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String[] dataset = { 
//				"data/1-dblp-acm/sources.csv",
//				"data/1-dblp-acm/targets.csv",
//				"data/3-amazon-googleproducts/targets.csv",
//				"data/4-abt-buy/sources.csv",
//				"data/5-person1/sources.csv",
//				"data/6-restaurant/sources.csv",
			"data/8-scalability/persons.csv",
			"data/8-scalability/places.csv",
			"data/8-scalability/works.csv",
		};
		String[] pname = {
//				"title",
//				"authors",
//				"name",
//				"description",
//				"surname",
//				"name",
			"name",
			"name",
			"name",
		};
		
//		launchTests(dataset, pname);
		
		scalabilityTests(dataset, pname);
		
	}

	private static void scalabilityTests(String[] dataset, String[] pname) throws IOException {
		
		int delta = 50000;
		THETA_MAX = 8;
		
		for(int i=0; i<dataset.length; i++) {
			
			clearKnowledgeBases();
			
			for(int j=0; j*delta <= sources.size(); j++) {
				
				loadKnowledgeBases(dataset[i], dataset[i], j*delta, (j+1)*delta);
				
				System.out.println(dataset[i]+" ("+sources.size()+")");
				sys_out += dataset[i]+" ("+sources.size()+")\n";
			
//				for(double theta=1; theta<=THETA_MAX; theta*=2)
//					testPassJoin(pname[i], theta);
			
				for(double theta=1; theta<=THETA_MAX; theta*=2)
					testOurApproachFilter(pname[i], theta);
	
				notify(sys_out);
				sys_out = "\n";
				
			}
		}
	}

	private static void launchTests(String[] dataset, String[] pname) throws IOException {
		
		THETA_MAX = 2;
		
		for(int i=0; i<dataset.length; i++) {
			clearKnowledgeBases();
			loadKnowledgeBases(dataset[i], dataset[i]);
			
			System.out.println(dataset[i]);
			sys_out += dataset[i]+"\n";
		
//				TreeSet<String> pjs = null, oafs = null;

			for(double theta=1; theta<=THETA_MAX; theta++) {
//			    	TreeSet<Couple> pj = 
					testPassJoin(pname[i], theta);
//					pjs = new TreeSet<String>();
//					for(Couple c : pj) {
//		//				System.out.println(c.getSource().getID()
//		//						+ "\t" + c.getTarget().getID()
//		//						+ "\t" + c.getSource().getOriginalPropertyValue(pname)
//		//						+ "\t" + c.getTarget().getOriginalPropertyValue(pname)
//		//						+ "\td = " + c.getDistances().get(0));
//						pjs.add(c.getSource().getOriginalPropertyValue(pname[i])+"#"+
//								c.getTarget().getOriginalPropertyValue(pname[i]));
//					}
			}
		
			for(double theta=1; theta<=THETA_MAX; theta++) {
//			    	TreeSet<Couple> oaf = 
					testOurApproachFilter(pname[i], theta);
//					oafs = new TreeSet<String>();
//					for(Couple c : oaf) {
//		//				System.out.println(c.getSource().getID()
//		//						+ "\t" + c.getTarget().getID()
//		//						+ "\t" + c.getSource().getOriginalPropertyValue(pname)
//		//						+ "\t" + c.getTarget().getOriginalPropertyValue(pname)
//		//						+ "\td = " + c.getDistances().get(0));
//						oafs.add(c.getSource().getOriginalPropertyValue(pname[i])+"#"+
//								c.getTarget().getOriginalPropertyValue(pname[i]));
//					}
			}

//			crossValidate(pjs, oafs);



//				for(double theta=1; theta<=THETA_MAX; theta++)
//						testEdJoinOnce(pname[i], theta);
			
//			notify(sys_out);
			sys_out = "\n";
		}
	}

	private static void crossValidate(TreeSet<String> pjs, TreeSet<String> oafs) {
			System.out.println("\nPJ but not OAF");
			for(String s : pjs)
				if(!oafs.contains(s))
					System.out.println(s);

			System.out.println("\nOAF but not PJ");
			for(String s : oafs)
				if(!pjs.contains(s))
					System.out.println(s);
	}

	public static void append(String s) {
		sys_out += s;
	}
}
