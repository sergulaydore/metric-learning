/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package metriclearning;

import java.util.ArrayList;

/**
 *
 * @author tom
 */
public class Couple {
    
    private Resource source;
    private Resource target;
    
    private ArrayList<Double> similarities;

    public double getSimsum() {
        double sum = 0.0;
        for(Double sim : similarities)
            sum += sim;
        return sum;
    }

    public Resource getSource() {
        return source;
    }

    public Resource getTarget() {
        return target;
    }

    public ArrayList<Double> getSimilarities() {
        return similarities;
    }
    
    public void addSimilarity(double d) {
        similarities.add(d);
    }

    public Couple(Resource source, Resource target) {
        this.source = source;
        this.target = target;
        similarities = new ArrayList<Double>();
    }

    public void clearSimilarities() {
        similarities.clear();
    }
    
}
