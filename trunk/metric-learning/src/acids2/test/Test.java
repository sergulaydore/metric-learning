package acids2.test;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeSet;

import acids2.MainAlgorithm;
import acids2.Resource;
import au.com.bytecode.opencsv.CSVReader;

public class Test {

	private static TreeSet<Resource> sources = new TreeSet<Resource>();
	private static TreeSet<Resource> targets = new TreeSet<Resource>();
	
    private static TreeSet<String> ignoredList = new TreeSet<String>();
    
    static {
    	ignoredList.add("id");
    	ignoredList.add("venue");
    	ignoredList.add("description");
//    	ignoredList.add("price");
    }

    /**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String datasetPath = args[0];
//		String datasetPath = "1-dblp-acm";
//		String datasetPath = "2-dblp-scholar";
//		String datasetPath = "4-abt-buy";
		int k = Integer.parseInt(args[1]);
		double beta = Double.parseDouble(args[2]);
		double th0c = Double.parseDouble(args[3]);
		
		String sourcePath = "data/" + datasetPath + "/sources.csv";
		String targetPath = "data/" + datasetPath + "/targets.csv";
		loadKnowledgeBases(sourcePath, targetPath);
		
		String mappingPath = "data/" + datasetPath + "/mapping.csv";
		loadMappings(mappingPath);
		
		System.out.println("k = "+k+"\tbeta = "+beta);
		MainAlgorithm.start(sources, targets, k, beta, th0c);
		
	}

    private static void loadKnowledgeBases(String sourcePath, String targetPath) throws IOException {
    	loadKnowledgeBases(sourcePath, targetPath, 0, Integer.MAX_VALUE);
    }

    private static void loadKnowledgeBases(String sourcePath, String targetPath, int startOffset, int endOffset) throws IOException {
    	
        CSVReader reader = new CSVReader(new FileReader(sourcePath));
        String [] titles = reader.readNext(); // gets the column titles
        for(int i=0; i<startOffset; i++) // skips start offset
        	reader.readNext();
        String [] nextLine;
        int count = 0;
        while ((nextLine = reader.readNext()) != null) {
            Resource r = new Resource(nextLine[0]);
            for(int i=0; i<nextLine.length; i++) {
                if(!ignoredList.contains( titles[i].toLowerCase() )) {
                    if(nextLine[i] != null)
                        r.setPropertyValue(titles[i], nextLine[i]);
                    else
                        r.setPropertyValue(titles[i], "");
                }
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
                if(!ignoredList.contains( titles[i].toLowerCase() )) {
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

	private static ArrayList<String> oraclesAnswers = new ArrayList<String>();
	
    public static ArrayList<String> getOraclesAnswers() {
		return oraclesAnswers;
	}

	private static void loadMappings(String mappingPath) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(mappingPath));
        reader.readNext(); // skips the column titles
        String [] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            oraclesAnswers.add(nextLine[0] + "#" + nextLine[1]);
        }
        reader.close();
    }
    
	public static boolean askOracle(String ids) {
		return oraclesAnswers.contains(ids);
	}
	

}
