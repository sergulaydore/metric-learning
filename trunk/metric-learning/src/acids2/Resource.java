package acids2;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class Resource implements Comparable<Resource> {
    
    private String ID;
    
    /**
     * originalProperties include symbols.
     * properties are for weighted edit distance calculation.
     */
    private TreeMap<String, String> originalProperties = new TreeMap<String, String>();
    private TreeMap<String, String> properties = new TreeMap<String, String>();
    private ArrayList<String> propertyOrder = new ArrayList<String>();

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

    public ArrayList<String> getPropertyNames() {
        return propertyOrder;
    }
    
    public void setPropertyValue(String p, String v) {
    	String vn = normalize(v);
        originalProperties.put(p, v);
        properties.put(p, vn);
        propertyOrder.add(p);
    }
    
    public int checkDatatype(String prop) {
    	try {
			Double.parseDouble(this.getPropertyValue(prop));
		} catch (NumberFormatException e) {
			return Property.TYPE_STRING;
		}
    	return Property.TYPE_NUMERIC;
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
