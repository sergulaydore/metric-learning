package acids2;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.TreeMap;

import utility.StringUtilities;

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
    	String vn = StringUtilities.normalize(v);
        originalProperties.put(p, v);
        properties.put(p, vn);
        propertyOrder.add(p);
    }
    
    public int checkDatatype(String prop) {
    	if(this.getPropertyValue(prop).equals(""))
    		return Property.TYPE_NUMERIC;
    	try {
			Double.parseDouble(this.getPropertyValue(prop));
		} catch (NumberFormatException e) {
//			System.out.println(prop+": "+this.getPropertyValue(prop)+" is not a double.");
			return Property.TYPE_STRING;
		}
    	return Property.TYPE_NUMERIC;
    }

	@Override
	public int compareTo(Resource o) {
		return this.getID().compareTo(o.getID());
	}
    
    
}
