/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package metriclearning;

import au.com.bytecode.opencsv.CSVReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import org.math.array.LinearAlgebra;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

/**
 *
 * @author Tommaso Soru
 */
public class MetricLearning {
    
    // the source cache
    static ArrayList<Resource> sources = new ArrayList<Resource>();
    
    // the target cache
    static ArrayList<Resource> targets = new ArrayList<Resource>();
    
    // the source properties
    static String[] sourceProperties;
    
    // the target properties
    static String[] targetProperties;
    
    // the source KB
//    static String sourcePath = "data/1-dblp-acm/DBLP2.csv";
    static String sourcePath = "data/dummy/sources.csv";
    
    // the target KB
//    static String targetPath = "data/1-dblp-acm/ACM.csv";
    static String targetPath = "data/dummy/targets.csv";
    
    // The oracle's knowledge is a mapping among instances of source KB
    // and target KB (oracle's answers).
//    static String mappingPath = "data/1-dblp-acm/DBLP-ACM_perfectMapping.csv";
    static String mappingPath = "data/dummy/couples.csv";
    static ArrayList<Couple> oraclesAnswers = new ArrayList<Couple>();
    
    // all elements in S×T
    static ArrayList<Couple> couples = new ArrayList<Couple>();
    
    // size of the most informative examples sets
    static int MOST_INF_SIZE = 2;
    static ArrayList<Couple> selected = new ArrayList<Couple>();
    static ArrayList<Couple> posSelected = new ArrayList<Couple>();
    static ArrayList<Couple> negSelected = new ArrayList<Couple>();
    
    
    static ArrayList<Couple> pos = new ArrayList<Couple>();
    static ArrayList<Couple> neg = new ArrayList<Couple>();
    
    // The edit distance calculator
    static Levenshtein l;
    
    // the dimensions, ergo the number of similarities applied to the properties
    static int n;
    
    // classifier (normal)
    static double[] C;
    // bias
    static double bias;
    
    // the model
    static svm_model model;
    
    // the similarity lower bound [0,1] for a couple to be a candidate
    static final double TAU = 0.5;

    // training errors upper bound
    static double NU = 0.01;

    // the candidate set size
    static final double V_SIZE = 100;
    static int k_pos = 5, k_neg = 5;
    
    // the weights of the perceptron
    static double[] weights = new double[4032];
    static int[] counts = new int[4032];
    
    // output for the Matlab/Octave file
    static String outString = "";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        
        loadKnowledgeBases();
        loadMappings();
        initializeClassifier();
        
        l = new Levenshtein(n);
        
        // for all sources and for all targets
        for(Resource s : sources) {
            for(Resource t : targets) {
                Couple c = new Couple(s, t);
                // compute the similarity
                computeSimilarity(c);
                // compute the quantity of information (1 / distance from the classifier)
                computeGamma(c);
                w(""+s.getID()+","+t.getID()+"\tsim = "+c.getSimsum()+"\tgamma = "+c.getGamma()+"\t"+classify(c));
                couples.add(c);
                updateBestInformatives(c, classify(c));
            }
        }
        w("MOST INF:");
        for(Couple c : posSelected)
            w(c.getSource().getID()+","+c.getTarget().getID()+" as a POS");
        for(Couple c : negSelected)
            w(c.getSource().getID()+","+c.getTarget().getID()+" as a NEG");
        
        selected.addAll(posSelected);
        selected.addAll(negSelected);
        
        // ask the oracle
        for(int i=0; i<selected.size(); i++) {
            Couple couple = selected.get(i);
            if( isPositive( couple ) ) {
                pos.add( couple );
            } else {
                neg.add( couple );
            }
            w("O("+couple.getSource().getID()+","+couple.getTarget().getID()+") = "+isPositive( couple ));
        }
        
        double eta_plus = 0.5, eta_minus = 0.5; // the learning rate
        double f1 = 0.0;
        for(int iter=0; f1 != 1.0; iter++) {
            // compute the four categories
            double tp = 0, fp = 0, tn = 0, fn = 0;
            for(Couple c : pos)
                if(rho(c, true))
                    fp ++;
                else tp ++;
            for(Couple c : neg)
                if(rho(c, false))
                    fn ++;
                else tn ++;
            // for all properties...
            for(int k=0; k<n; k++) {
                weights = l.getCostsMatrixAsArray(k);
                counts = l.getCountMatrixAsArray(k);
                // update each weight
                for(int i=0; i<weights.length; i++) {
                    double w = weights[i] + counts[i] *
                            (eta_plus * fp + eta_minus * fn);
                    int a = i/63;
                    int b = i%63;
                    int b0 = b + (a<=b ? 1 : 0);
                    l.setWeight(a, b0, k, w);
                }
            }
            
            
            // compute f1
            double pre = tp / (tp + fp);
            double rec = tp / (tp + fn);
            f1 = 2 * pre * rec / (pre + rec);
            
            w((iter+1)+".\tmcs(0) = "+l.getMatrixCheckSum(0)+
//                    "\tmcs(1) = "+l.getMatrixCheckSum(1)+"\tmcs(2) = "+l.getMatrixCheckSum(2)+
                    "\tf1 = "+f1+" (tp="+tp+", fp="+fp+", tn="+tn+", fn="+fn+")");
            
            // compute the new similarity according with the updated weights
            for(Couple c : selected) {
                computeSimilarity(c);
//                w(c.getSimsum()/(double)n+"");
            }
            
            // TODO update the classifier fields (static double[] C, static double bias)
            // launch the SVM
//            updateClassifier();
        }
        
        // train classifier
        
        // TODO ask oracle about most informative examples
        
        // TODO update weights of M (copy the code above)
        
//        saveToFile(0);
        
    }
    
    /**
     * The function returns true iff the couple hasn't been classified correctly.
     * @param c
     * @return 
     */
    private static boolean rho(Couple c, boolean positive) {
        boolean clax = classify(c);
        if(positive) { // O(x) = POS
            if(clax) // C(x) = POS
                return false;
            else // C(x) = NEG
                return true;
        } else { // O(x) = NEG
            if(clax) // C(x) = POS
                return true;
            else // C(x) = NEG
                return false;
        }
    }
    
    /**
     * W·X - b
     * @param c the couple
     * @return true if positive, false if negative
     */
    private static boolean classify(Couple c) {
        ArrayList<Double> sim = c.getSimilarities();
        double sum = 0.0;
        for(int i=0; i<n; i++)
            sum += sim.get(i) * C[i];
        sum = sum + bias;
//        w("sum is "+sum+"");
        if(sum <= 0)
            return true;
        else
            return false;
    }
    
    /**
     * This function returns true iff the source and the target have been mapped
     * together, so iff the couple (s,t) is positive.
     */
    private static boolean isPositive(Couple c) {
        String s = c.getSource().getID();
        String t = c.getTarget().getID();
        for(Couple pc : oraclesAnswers)
            if(pc.getSource().getID().equals(s) && pc.getTarget().getID().equals(t))
                return true;
        return false;
    }

    private static void loadKnowledgeBases() throws IOException {
        CSVReader reader = new CSVReader(new FileReader(sourcePath));
        String [] titles = reader.readNext(); // gets the column titles
        String [] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            Resource r = new Resource(nextLine[0]);
            for(int i=0; i<nextLine.length; i++)
                if(!titles[i].toLowerCase().equals("id") && !titles[i].toLowerCase().equals("year")
//                         && !titles[i].toLowerCase().equals("venue")
                        )
                    r.setPropertyValue(titles[i], nextLine[i]);
            sources.add(r);
            n = r.getPropertyNames().size();
        }

        reader = new CSVReader(new FileReader(targetPath));
        titles = reader.readNext(); // gets the column titles
        while ((nextLine = reader.readNext()) != null) {
            Resource r = new Resource(nextLine[0]);
            for(int i=0; i<nextLine.length; i++)
                if(!titles[i].toLowerCase().equals("id") && !titles[i].toLowerCase().equals("year")
//                         && !titles[i].toLowerCase().equals("venue")
                        )
                    r.setPropertyValue(titles[i], nextLine[i]);
            targets.add(r);
        }
    }

    private static void loadMappings() throws IOException {
        CSVReader reader = new CSVReader(new FileReader(mappingPath));
        reader.readNext(); // skips the column titles
        String [] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            oraclesAnswers.add(new Couple(new Resource(nextLine[0]), new Resource(nextLine[1])));
        }
    }

    private static double editDistance(String sourceStringValue, String targetStringValue, int k) {
        return l.getSimilarity(sourceStringValue, targetStringValue, k);
    }

    private static double mahalanobisDistance(double sourceDoubleValue, double targetDoubleValue, int k) {
        // TODO Mahalanobis distance algorithm
        return 0.0;
    }

    /** 
     * The initial classifier C should be the hyperplane that is equidistant
     * from points (0, ..., 0) and (1, ..., 1). Analytically, it's the vector
     * [-1, ..., -1, n/2].
     */
    private static void initializeClassifier() {
        C = new double[n];
        for(int i=0; i<C.length; i++) {
            C[i] = -1.0;
        }
        bias = (double)n / 2.0;
    }

    /**
     * Updates the classifier and builds a Matlab/Octave script to visualize
     * 2D or 3D graphs. SVM statements have been implemented for 2 classes only.
     */
    private static void updateClassifier() {
        svm_problem problem = new svm_problem();
        problem.l = pos.size()+neg.size();
        svm_node[][] x = new svm_node[pos.size()+neg.size()][n];
        double[] y = new double[pos.size()+neg.size()];
        for(int i=0; i<pos.size(); i++) {
            ArrayList<Double> p = pos.get(i).getSimilarities();
            for(int j=0; j<p.size(); j++) {
                x[i][j] = new svm_node();
                x[i][j].index = j;
                x[i][j].value = p.get(j);
//                w(p.get(j)+", ");
            }
            y[i] = 1;
//            w("TRUE");
        }
        for(int i=pos.size(); i<pos.size()+neg.size(); i++) {
            ArrayList<Double> p = neg.get(i-pos.size()).getSimilarities();
            for(int j=0; j<p.size(); j++) {
                x[i][j] = new svm_node();
                x[i][j].index = j;
                x[i][j].value = p.get(j);
//                w(p.get(j)+", ");
            }
            y[i] = -1;
//            w("FALSE");
        }
        problem.x = x;
        problem.y = y;
        svm_parameter parameter = new svm_parameter();
        parameter.nu = NU;
        parameter.svm_type = svm_parameter.NU_SVC;
        parameter.kernel_type = svm_parameter.LINEAR;
        parameter.eps = 0.001;
        model = svm.svm_train(problem, parameter);
        // sv = ( nSV ; n )
        svm_node[][] sv = model.SV;
        // sv_coef = ( 1 ; nSV )
        double[][] sv_coef = model.sv_coef;
        
        s("hold off;",true);
        // for each dimension...
        for(int j=0; j<x[0].length; j++) {
            // all positives...
            s("x"+j+"p = [",false);
            for(int i=0; i<pos.size(); i++)
                s(x[i][j].value+" ",false);
            s("];",true);
            // all negatives...
            s("x"+j+"n = [",false);
            for(int i=pos.size(); i<x.length; i++)
                s(x[i][j].value+" ",false);
            s("];",true);
        }
        
        if(sv.length > 0) {
            
            // calculating w
            // w = sv' * sv_coef' = (sv_coef * sv)' = ( n ; 1 )
            // b = -rho
            double[][] w = new double[sv[0].length][sv_coef.length];
            w = LinearAlgebra.transpose(LinearAlgebra.times(sv_coef,toDouble(sv)));
            int signum = (model.label[0] == -1.0) ? 1 : -1;
            double b = (model.label[0] == -1.0) ? model.rho[0] : -(model.rho[0]);

    //        double[][] xd = toDouble(x);
    //        for(int i=0; i<x.length; i++)
    //            s("#"+i+".\tprediction = "+predict(xd[i])+"\tactual = "+y[i],true);

            for(int i=0; i<w.length; i++)
                s("w"+i+" = "+w[i][0]+";",true);

            switch(n) {
                case 2:
                    s("q = -(" + b + ")/w1;",true);
                    s("plot(x0p,x1p,'xb'); hold on; "
                            + "plot(x0n,x1n,'xr');",true);
                    s("xl = [0:0.025:1];",true);
                    s("yl = "+signum+" * xl * w0/w1 + q;",true);
                    s("plot(xl,yl,'k');",true);
                    s("axis([0 1 0 1]);",true);
                    break;
                case 3:
                    s("if w2 == 0\nw2 = w1;\nw1 = 0;\nq = -("+b+")/w2;\n"
                            + "xT = x2;\nx2 = x1;\nx1 = xT;\nelse\nq = -(" + b + ")/w2;\nend",true);
                    s("plot3(x0p,x1p,x2p,'xb'); hold on; "
                            + "plot3(x0n,x1n,x2n,'xr');",true);
                    s("x = [0:0.025:1];",true);
                    s("[xx,yy] = meshgrid(x,x);",true);
                    s("zz = "+signum+" * w0/w2 * xx + "+signum+" * w1/w2 * yy + q;",true);
                    s("mesh(xx,yy,zz);",true);
                    s("axis([0 1 0 1 0 1]);",true);
                    break;
            }
           
            w(svm.svm_check_parameter(problem, parameter));
            w(""+parameter.C);
        } else {
            // TODO what to do when the svm fails?
        }
    }

    private static void s(String out, boolean newline) {
        System.out.print(out+(newline ? "\n" : ""));
//        outString += out+(newline ? "\n" : "");
    }

    private static double[][] toDouble(svm_node[][] sv) {
        double[][] t = new double[sv.length][sv[0].length];
        for(int i=0; i<sv.length; i++)
            for(int j=0; j<sv[i].length; j++)
                t[i][j] = sv[i][j].value;
        return t;
    }
    
    private static double predict(double[] values) {
        svm_node[] svm_nds = new svm_node[values.length];
        for(int i=0; i<values.length; i++) {
            svm_nds[i] = new svm_node();
            svm_nds[i].index = i;
            svm_nds[i].value = values[i];
        }

        return svm.svm_predict(model, svm_nds);
    }

    private static void saveToFile(int i) {
        try{
            // Create file 
            FileWriter fstream = new FileWriter("svmplot"+i+".m");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(outString);
            //Close the output stream
            out.close();
            outString = "";
        } catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    private static void w(String string) {
        System.out.println(string);
    }

    /** 
     * The method returns true if the points are correctly separable, i.e.
     * linearly separable and sorted.
     * TODO change method name
     */
    private static boolean isCorrectlySeparable() {
        for(Couple x : pos) {
            ArrayList<Double> x_sim = x.getSimilarities();
            for(Couple y : neg) {
                ArrayList<Double> y_sim = y.getSimilarities();
                for(int i=0; i<n; i++)
                    if(x_sim.get(i) <= y_sim.get(i))
                        return false;
            }
        }
        return true;
    }

    private static void computeSimilarity(Couple couple) {
        Resource source = couple.getSource();
        Resource target = couple.getTarget();
        
        // get couple similarities
        Set<String> propNames = source.getPropertyNames();
        couple.clearSimilarities();
        int k = 0;
        for(String prop : propNames) {
            String sourceStringValue = source.getPropertyValue(prop);
            String targetStringValue = target.getPropertyValue(prop);
            double sourceNumericValue = 0.0, targetNumericValue = 0.0;
            boolean isNumeric = true;
            try {
                sourceNumericValue = Double.parseDouble(sourceStringValue);
                targetNumericValue = Double.parseDouble(targetStringValue);
            } catch(NumberFormatException e) {
//                    System.out.println(sourceStringValue + " OR " + targetStringValue + " isn't numeric.");
                isNumeric = false;
            }
            if(isNumeric) {
                couple.addSimilarity( mahalanobisDistance(sourceNumericValue, targetNumericValue, k) );
//                    System.out.println("sim(" + prop + ") = " + d);
            } else {
                couple.addSimilarity( editDistance(sourceStringValue, targetStringValue, k) );
//                    System.out.println("sim(" + prop + ") = " + d);
            }
            k ++;
        }
    }

    /**
     * Computes the measure of how a couple c is informative.
     * Given a point Q and a classifier C:
     * D = |(Q-P)·C| / ||C|| = (|C·Q| + bias) / ||C||
     * where P is a point on the hyperplane. The measure is
     * gamma = 1/D
     * TODO normalize the gamma
     * @param c The couple.
     */
    private static void computeGamma(Couple c) {
        ArrayList<Double> Q = c.getSimilarities();
        double numer = 0.0, denom = 0.0;
        for(int i=0; i<n; i++) {
            numer += Q.get(i) * C[i];
            denom += Math.pow(C[i], 2);
        }
        numer += bias;
        denom = Math.sqrt(denom);
        c.setGamma(Math.abs(denom/numer)); // gamma = 1/D
    }

    private static void updateBestInformatives(Couple c, boolean positive) {
        if(positive) {
            if(posSelected.size() < MOST_INF_SIZE) {
                posSelected.add(c);
                return;
            }
            double min = 99999;
            Couple c_min = null;
            for(Couple c1 : posSelected)
                if(c1.getGamma() < min) {
                    min = c1.getGamma();
                    c_min = c1;
                }
            if(c.getGamma() > min) {
                posSelected.add(c);
                posSelected.remove(c_min);
            }
        } else {
            if(negSelected.size() < MOST_INF_SIZE) {
                negSelected.add(c);
                return;
            }
            double min = 99999;
            Couple c_min = null;
            for(Couple c1 : negSelected)
                if(c1.getGamma() < min) {
                    min = c1.getGamma();
                    c_min = c1;
                }
            if(c.getGamma() > min) {
                negSelected.add(c);
                negSelected.remove(c_min);
            }
        }
    }
}
