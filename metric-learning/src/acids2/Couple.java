/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package acids2;

import java.util.ArrayList;

/**
 *
 * @author tom
 */
public class Couple implements Comparable<Couple> {
    
    private Resource source;
    private Resource target;
    
    private ArrayList<Double> similarities;
    private ArrayList<Double> distances;
    
    private double gamma;
    
//    private int[][][] count;
    private ArrayList<Operation> ops;

    public static final int TP = 1;
    public static final int FP = 2;
    public static final int TN = 3;
    public static final int FN = 4;
    private int type;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
    
    private boolean positive;
    
    public boolean isPositive() {
		return positive;
	}

	public void setPositive(boolean positive) {
		this.positive = positive;
	}

	public void resetCount() {
//        for(int i=0; i<count.length; i++)
//            for(int j=0; j<count[i].length; j++)
//                for(int k=0; k<count[i][j].length; k++)
//                    count[i][j][k] = 0;
    	ops.clear();
    }
    
    public void count(int i, int j, int k) {
//        count[i][j][k] ++;
    	ops.add(new Operation(i, j, k));
    }
    
    public double getGamma() {
        return gamma;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public double getSimMean() {
        double sum = 0.0;
        for(Double sim : similarities)
            sum += sim;
        return sum/similarities.size();
    }

    public double getMeanDist() {
        double sum = 0.0;
        for(Double sim : distances)
            sum += sim;
        return sum/distances.size();
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

	public ArrayList<Double> getDistances() {
		return distances;
	}
    
    public void addSimilarity(double s) {
        similarities.add(s);
        double d = s==0 ? Double.POSITIVE_INFINITY : (1.0-s)/s;
        distances.add(d);
    }

    public void addDistance(double d) {
    	double s = d / (1.0 + d);
        similarities.add(s);
        distances.add(d);
    }

    public Couple(Resource source, Resource target) {
        this.source = source;
        this.target = target;
        similarities = new ArrayList<Double>();
        distances = new ArrayList<Double>();
        ops = new ArrayList<Operation>();
    }

    public void clearSimilarities() {
        similarities.clear();
        distances.clear();
    }
    
    public int[] getCountMatrixAsArray(int k) {
        int[] cArr = new int[4096];
        for(Operation op : ops) {
        	int n = op.getN();
        	if(k == n) {
	        	int arg1 = op.getArg1();
	        	int arg2 = op.getArg2();
	        	cArr[arg1*64+arg2]++;
        	}
        }        
//        int h = 0;
//        for(int i=0; i<count.length; i++)
//            for(int j=0; j<count[i].length; j++)
//                if(i != j) {
//                    cArr[h] = count[i][j][k];
//                    h++;
//                }
        return cArr;
    }

    @Override
    public int compareTo(Couple c) {
    	// maybe we should replace '#' with another symbol...
        String c1 = this.getSource().getID()+"#"+this.getTarget().getID();
        String c2 = c.getSource().getID()+"#"+c.getTarget().getID();
        return c1.compareTo(c2);
    }
    
    @Override
    public String toString() {
		return this.getSource().getID()+"#"+this.getTarget().getID();
    }

    @Override
    public boolean equals(Object o) {
    	if(!(o instanceof Couple))
    		return false;
    	else {
    		Couple c = (Couple) o;
            String c1 = this.getSource().getID()+"#"+this.getTarget().getID();
            String c2 = c.getSource().getID()+"#"+c.getTarget().getID();
            return c1.equals(c2);
    	}
    }
}
