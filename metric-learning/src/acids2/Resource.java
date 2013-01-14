/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package acids2;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class Resource implements Comparable<Resource> {
    
    private String ID;
    
    public static final int DATATYPE_STRING = 1;
    public static final int DATATYPE_NUMERIC = 2;
    public static final int DATATYPE_DATE = 3;
    
    /**
     * originalProperties include symbols.
     * properties are for weighted edit distance calculation.
     */
    private TreeMap<String, String> originalProperties = new TreeMap<String, String>();
    private TreeMap<String, String> properties = new TreeMap<String, String>();
    private TreeMap<String, Integer> datatypes = new TreeMap<String, Integer>();

    public Resource(String ID) {
        this.ID = ID;
    }

    public String getID() {
        return ID;
    }
    
    public String getPropertyValue(String p) {
        return properties.get(p);
    }
    
    public String getOriginalPropertyValue(String p) {
        return originalProperties.get(p);
    }

    public Set<String> getPropertyNames() {
        return properties.keySet();
    }
    
    public void setPropertyValue(String p, String v, int datatype) {
        originalProperties.put(p, v);
        properties.put(p, normalize(v));
        datatypes.put(p, datatype);
    }

	public TreeMap<String, Integer> getDatatypes() {
		return datatypes;
	}

	@Override
	public int compareTo(Resource o) {
		return this.getID().compareTo(o.getID());
	}
    
	/**
	 * This method filters out all the characters that are different from:
	 * digits (ASCII code 48-57), upper case (65-90), lower-case letters (97-122) and space (32).
	 * @param in
	 * @return
	 */
    private static String normalize(String in) {
        in = Normalizer.normalize(in, Form.NFD).trim();
        String out = "";
        for(int i=0; i<in.length(); i++) {
            char c = in.charAt(i);
            if((48 <= c && c <= 57) || (65 <= c && c <= 90) || (97 <= c && c <= 122) || c == 32)
                out += c;
        }
        return out;
    }
    
}
