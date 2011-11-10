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
    
    private double gamma;
    
    private int[][][] count;

    public int[][][] getCount() {
        return count;
    }
    
    public static final int TP = 1;
    public static final int FP = 2;
    public static final int TN = 3;
    public static final int FN = 4;
    private int classification;

    public int getClassification() {
        return classification;
    }

    public void setClassification(int classification) {
        this.classification = classification;
    }
    
    public void initializeCount(int n) {
        count = new int[64][64][n];
        for(int i=0; i<64; i++)
            for(int j=0; j<64; j++)
                for(int k=0; k<n; k++)
                    count[i][j][k] = 0;
    }
    
    public void resetCount() {
        for(int i=0; i<count.length; i++)
            for(int j=0; j<count[i].length; j++)
                for(int k=0; k<count[i][j].length; k++)
                    count[i][j][k] = 0;
    }
    
    public void count(int i, int j, int k) {
        count[i][j][k] ++;
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
    
    public int[] getCountMatrixAsArray(int k) {
        int[] cArr = new int[4032];
        int h = 0;
        for(int i=0; i<count.length; i++)
            for(int j=0; j<count[i].length; j++)
                if(i != j) {
                    cArr[h] = count[i][j][k];
                    h++;
                }
        return cArr;
    }
}
