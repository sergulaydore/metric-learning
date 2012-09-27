/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package metriclearning;

import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author tom
 */
public class Resource implements Comparable<Resource> {
    
    private String ID;
    private HashMap<String, String> properties = new HashMap<String, String>();

    public Resource(String ID) {
        this.ID = ID;
    }

    public String getID() {
        return ID;
    }
    
    public String getPropertyValue(String p) {
        return properties.get(p);
    }
    
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }
    
    public void setPropertyValue(String p, String v) {
        properties.put(p, v);
    }

	@Override
	public int compareTo(Resource o) {
		return this.getID().compareTo(o.getID());
	}
    
}
