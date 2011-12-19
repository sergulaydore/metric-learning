/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package metriclearning;

import algorithms.edjoin.EdJoinPlus;
import algorithms.edjoin.Entry;
import au.com.bytecode.opencsv.CSVReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.math.array.LinearAlgebra;

/**
 *
 * @author Tommaso Soru
 */
public class MetricLearning {
    
    // the source cache
    static ArrayList<Resource> sources = new ArrayList<Resource>();
    
    // the target cache
    static ArrayList<Resource> targets = new ArrayList<Resource>();
    
    // the source/target/oracle KB
    // The oracle's knowledge is a mapping among instances of source KB
    // and target KB (oracle's answers).
    static String sourcePath = "data/1-dblp-acm/sources.csv";
    static String targetPath = "data/1-dblp-acm/targets.csv";
//    static String mappingPath = "data/1-dblp-acm/DBLP-ACM-small.csv";
//    static String sourcePath = "data/1-dblp-acm/DBLP2.csv";
//    static String targetPath = "data/1-dblp-acm/ACM.csv";
    static String mappingPath = "data/1-dblp-acm/DBLP-ACM_perfectMapping.csv";
//    static String sourcePath = "data/dbpedia-cordis-organizations/source.csv";
//    static String targetPath = "data/dbpedia-cordis-organizations/target.csv";
//    static String mappingPath = "data/dbpedia-cordis-organizations/reference.csv";
//    static String sourcePath = "data/4-abt-buy/Abt.csv";
//    static String targetPath = "data/4-abt-buy/Buy.csv";
//    static String mappingPath = "data/4-abt-buy/abt_buy_perfectMapping.csv";
    
    static String[] ignoredList = {"id", "description"};
    
    static ArrayList<Couple> oraclesAnswers = new ArrayList<Couple>();
    
    // all elements in S×T then filtered by ED-Join
    static LinkedList<Couple> couples = new LinkedList<Couple>();
    
    // size of the most informative examples sets
    static int MOSTINF_POS_CAND = 5;
    static int MOSTINF_NEG_CAND = 5;
    
    static ArrayList<Couple> posSelected = new ArrayList<Couple>();
    static ArrayList<Couple> negSelected = new ArrayList<Couple>();
    
    static ArrayList<Couple> actualPos = new ArrayList<Couple>();
    static ArrayList<Couple> actualNeg = new ArrayList<Couple>();
    
    static ArrayList<Couple> answered = new ArrayList<Couple>();
    
    // the dimensions, ergo the number of similarities applied to the properties
    static int n;
    
    // linear classifier and its bias
    static double[] C;
    static double bias;
    static double signum;
    
    // the initial bias will be n / 2 * bias_factor
    static double bias_factor;
    
    // the model
    static svm_model model;
    
    // training errors upper bound
    static double SVM_C = 1E+10;

    // the Weight for Positive Examples, or 1 minus the probability
    // to have a positive example
    static double wpe;

    // the learning rates. note that only eta_plus is fixed
    static double eta_plus = 0.1, eta_minus;
    
    // weights (and counts) of the perceptron
    static double[] weights = new double[4032];
    static final int MAX_PERCEPTRON_ITER = 500;
    
    // sets to calculate precision and recall
    static double tp = 0, fp = 0, tn = 0, fn = 0;
    
    // vector used to normalize the weights
    static double[] oldmax;
    
    // output for the Matlab/Octave file
    static String outString = "";
    static boolean createOctaveScript = true;
    static boolean sendEmail = false;
    
    // max range of the distance (max iterations of hypercube widening)
    static final int BETA_MIN = 1; // for test only
    static final int BETA_MAX = 50;
    
    static final int MAX_ITERATIONS = 5;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        
        loadKnowledgeBases();
        
        w("|S| = "+sources.size()+"\t"+"|T| = "+targets.size()+"\t"+"n = "+n);
        if(sources.isEmpty() || targets.isEmpty())
            System.exit(0);
        
        loadMappings();
        w("Oracle's knowledge has "+oraclesAnswers.size()+" positive examples.");
        
        // the probability to have a neg is "wpe" times the one to have a pos,
        // then the probability to have a FP is "wpe" times the one to have a FN,
        // so we must balance the etas.
        wpe = ((double) (Math.max(sources.size(), targets.size()))) - 1.0;
        eta_minus = eta_plus * wpe;
        
//        bias_factor = calculateBiasFactor();
        bias_factor = 1.2;
        w("Bias Factor = "+bias_factor);
        initializeClassifier();
        
//        buildDatasets();
//        System.exit(0);
        
        /* DONE
         * - Assigning an initial weight of 0.1 to
         * lowercase-uppercase substitutions?
         * - Normalizing the count multiplier during
         * the weight change?
         * PENDING
         * - Adding a delta to encourage the
         * perceptron learning?
         * 
         * Obs.:
         * - The first classifer is extremely important (bias factor = 1.2)
         * - The "venue" dimension is less reliable because SIGMOD = International...
         * - The perceptron isn't iterating, but f1>0.97:
         * - It works well when we have k actual pos and k actual neg:
         *     we could ask again until we have the first neg that is most inf.
         */
        
        EditSimilarities.initialize(n);
        
        // initialize the weights normalizer
        oldmax = new double[n];
        for(int i=0; i<n; i++)
            oldmax[i] = 1.0;
                
        couples.addAll(edjToCouples(callEdJoin(1)));
//        showCouples();
                
        // compute the similarity
        for(Couple c : couples) {
            c.initializeCount(n);
            computeSimilarity(c);
        }
                
        boolean goForTest = false;
        double f1 = 0.0;
        for(int iter = 1; true; iter ++) {
            w("\n---------- ITERATION #"+iter+" ----------");
            
            if(iter > 1) {
                
                couples.addAll(edjToCouples(callEdJoin(iter)));
                // the hypercube could have moved or not. in both cases,
                // we shall remove all dupes.
                checkDuplicates();
                
                for(Couple c : couples) {
                    c.initializeCount(n);
                    computeSimilarity(c);
                }
                
            }
            
            posSelected.clear();
            negSelected.clear();
            
            // compute gamma and update the set of most informative examples
            for(Couple c : couples) {
                if(!hasBeenAnswered(c)) {
                    computeGamma(c);
                    updateSelected(c);
                }
            }

            ArrayList<Couple> selected = new ArrayList<Couple>();
            selected.addAll(posSelected);
            selected.addAll(negSelected);

            // ask the oracle
            askOracle(selected);
            
            w("\nĈ^"+iter+" classifier");
            updateClassifier(false);
            // calculate the f-score to update the FP and FN sets.
            f1 = computeF1();
            
            // if it isn't separable, change the weights using perceptron learning.
            // the concept of being separable is stronger than having f1 = 1.0.
            // we're checking for FP & FN using a still classifier
            // think it as we're stopping the time!

            boolean separable = isLinearlySeparable();
            w("");
            for(int miter = 0; miter<MAX_PERCEPTRON_ITER && !separable && f1<1.0; miter++) {
                w(miter+".");
                for(int k=0; k<n; k++)
                    if(!isLinearlySeparable(k))
                        computeM(k);

                EditSimilarities.resetCount();
                for(Couple c: couples)
                    c.resetCount();

                // compute the new similarity according with the updated weights
                for(Couple c : couples) {
                    computeSimilarity(c);
                    System.out.print(".");
                }

                // check if couples are linearly separable for all dimensions
                separable = isLinearlySeparable();

            }

            // train classifier
            updateClassifier(true);

            System.out.print(iter+"\t");
            f1 = computeF1();

            createOctaveScript(iter+"");
                        
            // loop break conditions
            if(f1 == 1.0) {
                if(goForTest)
                    break;
                else
                    goForTest = true;
            } else {
                goForTest = false;
            }
            if(iter >= MAX_ITERATIONS)
                break;

        }
        
        for(int i=0; i<n; i++)
            EditSimilarities.showCostsMatrix(i, false);

        adjustClassifier();
        
        plot(true);
        createOctaveScript("final");
        
        testWholeDataset();
        
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
                if(!isIgnored(titles[i].toLowerCase())) {
                    if(nextLine[i] != null)
                        r.setPropertyValue(titles[i], nextLine[i]);
                    else
                        r.setPropertyValue(titles[i], "");
                }
            sources.add(r);
            n = r.getPropertyNames().size();
        }
        
        reader = new CSVReader(new FileReader(targetPath));
        titles = reader.readNext(); // gets the column titles
        while ((nextLine = reader.readNext()) != null) {
            Resource r = new Resource(nextLine[0]);
            for(int i=0; i<nextLine.length; i++)
                if(!isIgnored(titles[i].toLowerCase())) {
                    if(nextLine[i] != null)
                        r.setPropertyValue(titles[i], nextLine[i]);
                    else
                        r.setPropertyValue(titles[i], "");
                }
            targets.add(r);
        }
    }
    
    private static boolean isIgnored(String title) {
        for(String ign : ignoredList) {
            if(title.equals(ign))
                return true;
        }
        return false;
    }

    private static void loadMappings() throws IOException {
        CSVReader reader = new CSVReader(new FileReader(mappingPath));
        reader.readNext(); // skips the column titles
        String [] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            oraclesAnswers.add(new Couple(new Resource(nextLine[0]), new Resource(nextLine[1])));
        }
    }


    /** 
     * The initial classifier C should be the hyperplane that is equidistant
     * from points (0, ..., 0) and (1, ..., 1). Analytically, it's the vector
     * [-1, ..., -1, n/2].
     * By the way, we prefer bias = (double)n * 0.75, because we usually have 
     * more negative than positive examples.
     */
    private static void initializeClassifier() {
        C = new double[n];
        for(int i=0; i<C.length; i++) {
            C[i] = -1.0;
        }
        bias = (double)n / 2 * bias_factor;
    }

    /**
     * Updates the classifier and builds a Matlab/Octave script to visualize
     * 2D or 3D graphs. SVM statements have been implemented for 2 classes only.
     */
    private static void updateClassifier(boolean appendToOctaveScript) {
        svm_problem problem = new svm_problem();
        problem.l = actualPos.size()+actualNeg.size();
        svm_node[][] x = new svm_node[actualPos.size()+actualNeg.size()][n];
        double[] y = new double[actualPos.size()+actualNeg.size()];
        for(int i=0; i<actualPos.size(); i++) {
            ArrayList<Double> p = actualPos.get(i).getSimilarities();
            for(int j=0; j<p.size(); j++) {
                x[i][j] = new svm_node();
                x[i][j].index = j;
                x[i][j].value = p.get(j);
            }
            y[i] = 1;
        }
        for(int i=actualPos.size(); i<actualPos.size()+actualNeg.size(); i++) {
            ArrayList<Double> p = actualNeg.get(i-actualPos.size()).getSimilarities();
            for(int j=0; j<p.size(); j++) {
                x[i][j] = new svm_node();
                x[i][j].index = j;
                x[i][j].value = p.get(j);
            }
            y[i] = -1;
        }
        problem.x = x;
        problem.y = y;
        svm_parameter parameter = new svm_parameter();
        parameter.C = SVM_C;
        parameter.svm_type = svm_parameter.C_SVC;
        parameter.kernel_type = svm_parameter.LINEAR;
        parameter.eps = 0.0001;
        model = svm.svm_train(problem, parameter);
        // sv = ( nSV ; n )
        svm_node[][] sv = model.SV;
        // sv_coef = ( 1 ; nSV )
        double[][] sv_coef = model.sv_coef;
        
        // calculate w and b
        // w = sv' * sv_coef' = (sv_coef * sv)' = ( n ; 1 )
        // b = -rho
        double[][] w = new double[sv[0].length][sv_coef.length];
        signum = (model.label[0] == -1.0) ? 1 : -1;
        
        w = LinearAlgebra.transpose(LinearAlgebra.times(sv_coef,toDouble(sv)));
        double b = -model.rho[0];
        
        for(int i=0; i<C.length; i++) {
            C[i] = signum * w[i][0];
            w("C["+i+"] = "+C[i]);
        }
        
        bias = signum * b;
        w("bias = "+bias);
        
        // if needed, save the Matlab/Octave script
        if(createOctaveScript && appendToOctaveScript) {
            s("hold off;",true);
            // for each dimension...
            for(int j=0; j<x[0].length; j++) {
                // all positives...
                s("x"+j+"p = [",false);
                for(int i=0; i<actualPos.size(); i++)
                    s(x[i][j].value+" ",false);
                s("];",true);
                // all negatives...
                s("x"+j+"n = [",false);
                for(int i=actualPos.size(); i<x.length; i++)
                    s(x[i][j].value+" ",false);
                s("];",true);
            }

            if(sv.length > 0) {

                if(appendToOctaveScript) {
                    for(int i=0; i<w.length; i++)
                        s("w"+i+" = "+w[i][0]+";",true);
                    plot(false);
                }
                
            } else {
                // no SVs?
                w("*** No SVs to plot! ***");
            }
        }
    }

    private static void s(String out, boolean newline) {
        System.out.print(out+(newline ? "\n" : ""));
        outString += out+(newline ? "\n" : "");
    }

    protected static double[][] toDouble(svm_node[][] sv) {
        double[][] t = new double[sv.length][sv[0].length];
        for(int i=0; i<sv.length; i++)
            for(int j=0; j<sv[i].length; j++)
                t[i][j] = sv[i][j].value;
        return t;
    }
    
    private static double svmPredict(double[] values) {
        svm_node[] svm_nds = new svm_node[values.length];
        for(int i=0; i<values.length; i++) {
            svm_nds[i] = new svm_node();
            svm_nds[i].index = i;
            svm_nds[i].value = values[i];
        }

        return svm.svm_predict(model, svm_nds);
    }

    private static void createOctaveScript(String suffix) {
        if(createOctaveScript) {
            try{
                // Create file 
                FileWriter fstream = new FileWriter("svmplot"+suffix+".m");
                BufferedWriter out = new BufferedWriter(fstream);
                out.write(outString);
                //Close the output stream
                out.close();
                outString = "";
            } catch (Exception e){//Catch exception if any
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
    
    protected static void w(String string) {
        System.out.println(string);
    }

    private static void computeSimilarity(Couple couple) {
        Resource source = couple.getSource();
        Resource target = couple.getTarget();
        
        // get couple similarities
        Set<String> propNames = source.getPropertyNames();
        couple.clearSimilarities();
        int k = 0;
//        w("Calculating similarities...");
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
            } catch(NullPointerException e) {
                try {
                    sourceNumericValue = Double.parseDouble(sourceStringValue);
                } catch(NullPointerException e1) {
                    sourceNumericValue = 0.0;
                }
                try {
                    targetNumericValue = Double.parseDouble(targetStringValue);
                } catch(NullPointerException e1) {
                    targetNumericValue = 0.0;
                }
            }
            if(isNumeric) {
                // TODO organize all numerical values
                Mahalanobis mah = new Mahalanobis(1);
                double[] src = {sourceNumericValue};
                double[] tgt = {targetNumericValue};
                double d = mah.getSimilarity(src, tgt);
                couple.addSimilarity( d );
//                    System.out.println("sim(" + prop + ") = " + d);
            } else {
                double d = EditSimilarities.getEditSimilarity(sourceStringValue, targetStringValue, k, couple);
                couple.addSimilarity( d );
//                System.out.println("sim(" + prop + ") = " + d);
            }
            k ++;
        }
    }

    /**
     * Computes the measure of how much a couple is informative.
     * Given a point Q and a classifier C, the distance D is
     * D = |(Q-P)·C| / ||C|| = |C·Q + bias| / ||C||
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
    
    /**
     * Updates the positive and negative sets of best informative examples.
     * @param c 
     */
    private static void updateSelected(Couple c) {
        if(classify(c)) {
            if(posSelected.size() < MOSTINF_POS_CAND) {
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
            if(negSelected.size() < MOSTINF_NEG_CAND) {
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
    
    /**
     * Computes the F-Score, or F1.
     * @return The F-score.
     */
    private static double computeF1() {
        // compute the four categories
        tp = 0; fp = 0; tn = 0; fn = 0;
        for(Couple c : actualPos)
            if(classify(c)) {
                tp ++;
                c.setClassification(Couple.TP);
            } else {
                fn ++;
                c.setClassification(Couple.FN);
            }
        for(Couple c : actualNeg)
            if(classify(c)) {
                fp ++;
                c.setClassification(Couple.FP);
            } else {
                tn ++;
                c.setClassification(Couple.TN);
            }

        // compute f1
        double pre = tp+fp != 0 ? tp / (tp + fp) : 0;
        double rec = tp+fn != 0 ? tp / (tp + fn) : 0;
        double f1 = pre+rec != 0 ? 2 * pre * rec / (pre + rec) : 0;

        w("\nf1 = "+f1+" (tp="+tp+", fp="+fp+", tn="+tn+", fn="+fn+")");
        return f1;
    }
    
    private static void computeM(int k) {
        int[] countSumFalsePos = new int[4032];
        int[] countSumFalseNeg = new int[4032];
        // sum up error weights
        for(int i=0; i<4032; i++) {
            countSumFalsePos[i] = 0;
            countSumFalseNeg[i] = 0;
        }
        for(Couple c : couples) {
            if(c.getClassification() == Couple.FP) {
                int[] cArr = c.getCountMatrixAsArray(k);
                for(int i=0; i<4032; i++)
                    countSumFalsePos[i] += cArr[i];
            }
            if(c.getClassification() == Couple.FN) {
                int[] cArr = c.getCountMatrixAsArray(k);
                for(int i=0; i<4032; i++)
                    countSumFalseNeg[i] += cArr[i];
            }
        }

        weights = EditSimilarities.getCostsMatrixAsArray(k);
        double max = 0.0;
        for(int i=0; i<weights.length; i++) {
            // we should count how many times a weight was used
            // *only* when we have errors (fp and fn).
//                double normFactor = sources.size() != 1 ? sources.size()-1 : 1;
            weights[i] = Math.pow(weights[i], 1) * oldmax[k] +
                    countSumFalsePos[i] * eta_plus +
                    countSumFalseNeg[i] * eta_minus;
            if(weights[i] > max)
                max = weights[i];
        }
        oldmax[k] = max;
        // update and normalize each weight
        // w' = root3(w / M)
        for(int i=0; i<weights.length; i++) {
            int a = i/63;
            int b = i%63;
            int b0 = b + (a<=b ? 1 : 0);
            if(weights[i] < 0)
                weights[i] = 0;
            EditSimilarities.setWeight(a, b0, k, Math.pow(weights[i]/max, 1));
//                w(a+","+b0+","+k+" = "+weights[i]+"/"+max+"^0.3 = "+Math.pow(weights[i]/max, 0.3333));
//                if(countSumTrue[i] > countSumFalse[i])
//                    JOptionPane.showMessageDialog(null,"("+a+","+b0+"): "+
//                            countSumTrue[i] +">"+ countSumFalse[i]+
//                            ", w = "+weights[i]);
        }
    }
    
    private static boolean isLinearlySeparable(int k) {
        double highestNeg = 0.0;
        double lowestPos = 1.0;
        for(Couple c : couples) {
            ArrayList<Double> sims = c.getSimilarities();
                Double sim = sims.get(k);
                if(actualPos.contains(c)) {
                    if(sim < lowestPos)
                        lowestPos = sim;
                }
                if(actualNeg.contains(c)) {
                    if(sim > highestNeg)
                        highestNeg = sim;
                }
        }
        // here "<=" means "strictly separable"
        if(lowestPos < highestNeg)
            return false;
        else
            return true;
    }

    private static boolean isLinearlySeparable() {
        boolean separable = true;
        double[] highestNeg = new double[n];
        double[] lowestPos = new double[n];
        for(int s=0; s<n; s++) {
            highestNeg[s] = 0.0;
            lowestPos[s] = 1.0;
        }
        for(Couple c : couples) {
            ArrayList<Double> sims = c.getSimilarities();
            for(int s=0; s<sims.size(); s++) {
                Double sim = sims.get(s);
                if(actualPos.contains(c)) {
                    if(sim < lowestPos[s])
                        lowestPos[s] = sim;
                }
                if(actualNeg.contains(c)) {
                    if(sim > highestNeg[s])
                        highestNeg[s] = sim;
                }
            }
        }
        w("");
        for(int s=0; s<n; s++) {
            w("low["+s+"]: "+lowestPos[s]+"; high["+s+"]: "+highestNeg[s]);
            // here "<=" means "strictly separable"
            if(lowestPos[s] < highestNeg[s])
                separable = false;
        }
        return separable;
    }

    private static TreeSet<Couple> edjToCouples(TreeSet<String> edjoined) {
        TreeSet<Couple> cpls = new TreeSet<Couple>();
        for(String edj : edjoined) {
            String[] ed = edj.split("#");
            Resource r1 = null, r2 = null;
            for(Resource s : sources)
                if(ed[0].equals(s.getID())) {
                    r1 = s;
                    break;
                }
            for(Resource t : targets)
                if(ed[1].equals(t.getID())) {
                    r2 = t;
                    break;
                }
            cpls.add(new Couple(r1, r2));
        }
        return cpls;
    }
    
    private static int toDistance(double similarity) {
        return similarity == 0.0 ? Integer.MAX_VALUE : (int)((1.0 - similarity) / similarity);
    }

    private static TreeSet<String> callEdJoin(int iter) {
        for(int beta_dist = BETA_MIN; true; beta_dist++) {
            w("beta_dist = "+beta_dist+"\n");
            
            double theta_generic = bias;
            Resource first = sources.get(0);
            int propIter = 0;
            TreeSet<String> intersection = new TreeSet<String>();
            
            for(String p : first.getPropertyNames()) {
                TreeSet<Entry> sTree = new TreeSet<Entry>();
                for(Resource s : sources)
                    sTree.add(new Entry(s.getID(), s.getPropertyValue(p)));
                TreeSet<Entry> tTree = new TreeSet<Entry>();
                for(Resource t : targets)
                    tTree.add(new Entry(t.getID(), t.getPropertyValue(p)));

                // theta & the similarity range (that's not mandatory but useful)
                double sumOfOthers = 0.0;
                for(int j=0; j<n; j++)
                    if(propIter != j)
                        sumOfOthers += C[j];
                double theta =
                        C[propIter] == 0.0 ?
                        (theta_generic - sumOfOthers) / (C[propIter]) : 0.5;
                // the distance range..
                int theta_dist = toDistance(theta);
                
//                // TODO possibility to flag a property with "numeric"
//                if(p.equals("price"))
//                    // TODO do the MahalaJoin thing
//                    section = MahalaJoin.run();
//                else
                int min = (theta_dist-beta_dist)>0 ? (theta_dist-beta_dist) : 0;
                int max = theta_dist+beta_dist;
                
                // FIXME strange behavior with property no.2 (description) of abt-buy
                TreeSet<String> section = EdJoinPlus.runOnEntries(
                        min, max, sTree, tTree);
                
                w("\n#"+propIter+" section = "+section.size()+"\n");
            
                if(propIter == 0) {
                    intersection.addAll(section);
                } else {
                    TreeSet<String> toRemove = new TreeSet<String>();
                    for(String s : intersection)
                        if(!section.contains(s))
                            toRemove.add(s);
                    for(String s : toRemove)
                        intersection.remove(s);
                }
                propIter++;
            }
            
            w("intersection = "+intersection.size()+"\n");
            
            int maybePos = 0, maybeNeg = 0;
            for(String s : intersection) {
                Couple c = buildCouple(s);
                if(classify(c))
                    maybePos++;
                else
                    maybeNeg++;
            }
            
            w("maybe: pos = "+maybePos+", neg = "+maybeNeg+"\n");
            
            if((maybePos >= MOSTINF_POS_CAND*iter && maybeNeg >= MOSTINF_NEG_CAND*iter)
                    || beta_dist == BETA_MAX)
                return intersection;
        }
    }


    private static void checkDuplicates() {
        ArrayList<Couple> toRemove = new ArrayList<Couple>();
        for(int i=0; i<couples.size(); i++) {
            Couple ci = couples.get(i);
            for(int j=0; j<i; j++) {
                Couple cj = couples.get(j);
                if(ci.getSource().getID().equals(cj.getSource().getID()) &&
                        ci.getTarget().getID().equals(cj.getTarget().getID())) {
//                    w("*** duplicate found ***\t"+cj.getSource().getID()+"#"+cj.getTarget().getID());
                    toRemove.add(cj);
                }
            }
        }
        for(Couple c : toRemove)
            couples.remove(c);
    }

    private static boolean hasBeenAnswered(Couple c) {
        for(Couple a : answered) {
            if(a.getSource().getID().equals(c.getSource().getID()) &&
                    a.getTarget().getID().equals(c.getTarget().getID()))
                return true;
        }
        return false;
    }

    private static void showCouples() {
        for(Couple c : couples) {
            Resource s = c.getSource();
            w(s.getID());
            for(String p : s.getPropertyNames())
                w("\t"+s.getPropertyValue(p));
            Resource t = c.getTarget();
            w(t.getID());
            for(String p : t.getPropertyNames())
                w("\t"+t.getPropertyValue(p));
            w("-----");
        }
    }

    private static void testWholeDataset() {
               
        tp = 0; fp = 0; tn = 0; fn = 0;
        int counter = 0;
        if(n == 1) {
            if(C[0] < 0)
                w("\npos(x) iff sim(x) >= "+(-bias/C[0]));
            if(C[0] > 0)
                w("\npos(x) iff sim(x) <= "+(-bias/C[0]));
        }
        w("\nFinal test of "+(sources.size()*targets.size())+" couples.");
        
        String[] posString = new String[n];
        String[] negString = new String[n];
        if(createOctaveScript)
            for(int i=0; i<n; i++) {
                posString[i] = "";
                negString[i] = "";
            }
        
        int size = sources.size()*targets.size();
        for(Resource s : sources) {
            for(Resource t : targets) {
                Couple c = new Couple(s, t);
                c.initializeCount(n);
                computeSimilarity(c);
                if(isPositive(c)) {
                    if(classify(c)) {
                        tp++;
                    } else {
//                        w("pos:\t"+c.getSimMean()+"\t"+s.getPropertyValue("title") +"#"+t.getPropertyValue("title"));
                        fn++;
                    }
                    if(createOctaveScript)
                        for(int j=0; j<n; j++)
                            posString[j] += c.getSimilarities().get(j)+" ";
                } else {
                    if(classify(c)) {
//                        w("neg:\t"+c.getSimMean()+"\t"+s.getPropertyValue("title") +"#"+t.getPropertyValue("title"));
                        fp++;
                    } else {
                        tn++;
                    }
                    if(createOctaveScript)
                        for(int j=0; j<n; j++)
                            negString[j] += c.getSimilarities().get(j)+" ";
                }
                counter++;
                System.out.print(".");
                if(counter % 100 == 0)
                    w(" "+counter);
//                if(counter == size * 0.25) showPartial(25);
//                if(counter == size * 0.50) showPartial(50);
//                if(counter == size * 0.75) showPartial(75);
            }
        }
        
        if(createOctaveScript) {
            for(int j=0; j<n; j++) {
                // all positives...
                s("x"+j+"p = ["+posString[j]+"];",true);
                // all negatives...
                s("x"+j+"n = ["+negString[j]+"];",true);
            }
            createOctaveScript("all");
        }
        
        // compute f1
        double pre = tp+fp != 0 ? tp / (tp + fp) : 0;
        double rec = tp+fn != 0 ? tp / (tp + fn) : 0;
        double f1 = pre+rec != 0 ? 2 * pre * rec / (pre + rec) : 0;

        w("\n\nResults\n"
                + "f1 = "+f1+"\n"
                + "pre = "+pre+"\n"
                + "rec = "+rec+"\n"
                + "tp="+tp+", fp="+fp+", tn="+tn+", fn="+fn);
                
        if(sendEmail)
            Notifier.notify(f1, pre, rec, tp, fp, tn, fn, 100);
    }

    private static void buildDatasets() {
        String src = "", tgt = "";
        for(int i=0; i<100 && i<oraclesAnswers.size(); i++) {
            Couple c = oraclesAnswers.get((oraclesAnswers.size()/100)*i);
            for(Resource s : sources) {
                if(c.getSource().getID().equals(s.getID())) {
                    src += "\""+s.getID()+"\"";
                    for(String p : s.getPropertyNames())
                        src += ",\""+s.getPropertyValue(p)+"\"";
                    src += "\n";
                    break;
                }
            }
            for(Resource t : targets) {
                if(c.getTarget().getID().equals(t.getID())) {
                    tgt += "\""+t.getID()+"\"";
                    for(String p : t.getPropertyNames())
                        tgt += ",\""+t.getPropertyValue(p)+"\"";
                    tgt += "\n";
                    break;
                }
            }
        }
        try {
            FileWriter fstream = new FileWriter("sources.csv");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(src);
            out.close();
        } catch (Exception e){ w("Error: " + e.getMessage()); }
        try {
            FileWriter fstream = new FileWriter("targets.csv");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(tgt);
            out.close();
        } catch (Exception e){ w("Error: " + e.getMessage()); }
    }

    private static void adjustClassifier() {
        double[][] actualPosValues = new double[n][actualPos.size()];
        for(int j=0; j<actualPos.size(); j++) {
            ArrayList<Double> sims = actualPos.get(j).getSimilarities();
            for(int i=0; i<n; i++)
                actualPosValues[i][j] = sims.get(i);
        }
        double[][] actualNegValues = new double[n][actualNeg.size()];
        for(int j=0; j<actualNeg.size(); j++) {
            ArrayList<Double> sims = actualNeg.get(j).getSimilarities();
            for(int i=0; i<n; i++)
                actualNegValues[i][j] = sims.get(i);
        }
        
        double[] baric = new double[n];
        Mean mean = new Mean();
        StandardDeviation std = new StandardDeviation();
        for(int i=0; i<n; i++) {
            double posMean = mean.evaluate(actualPosValues[i]);
            double negMean = mean.evaluate(actualNegValues[i]);
            double posStd = std.evaluate(actualPosValues[i]);
            double negStd = std.evaluate(actualNegValues[i]);
            baric[i] = (posMean - 2*posStd) * 0.5 + (negMean + 2*negStd) * 0.5;
            w("("+posMean+" - 2*"+posStd+") * 0.5 + ("+negMean+" + 2*"+negStd+") * 0.5");
            w("baric["+i+"] = "+baric[i]);
        }
        double newBias = 0.0;
        for(int i=0; i<n; i++)
            newBias -= C[i] * baric[i];
        bias = newBias;
    }

    private static void plot(boolean saveImage) {
        if(createOctaveScript) {
            double b = signum * bias;
            switch(n) {
                case 1:
                    s("hold off;",true);
                    s("zrp = zeros(1,length(x0p));",true);
                    s("zrn = zeros(1,length(x0n));",true);
                    s("plot(x0p,zrp,'xb');",true);
                    s("hold on;",true);
                    s("plot(x0n,zrn,'xr');",true);
                    s("xc=["+(-bias/C[0])+" "+(-bias/C[0])+"];",true);
                    s("yc=[-1 1];",true);
                    s("plot(xc,yc,'g');",true);
                    s("axis([min([x0p x0n]) max([x0p x0n]) -0.1 0.1]);",true);
                    break;
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
                    s("if w2 < 1E-2\nw2 = w1;\nw1 = 0;\nq = -("+b+")/w2;\n"
                            + "xTp = x2p;\nx2p = x1p;\nx1p = xTp;\n"
                            + "xTn = x2n;\nx2n = x1n;\nx1n = xTn;"
                            + "\nelse\nq = -(" + b + ")/w2;\nend",true);
                    s("plot3(x0p,x1p,x2p,'xb'); hold on; "
                            + "plot3(x0n,x1n,x2n,'xr');",true);
                    s("x = [0:0.025:1];",true);
                    s("[xx,yy] = meshgrid(x,x);",true);
                    s("zz = "+signum+" * w0/w2 * xx + "+signum+" * w1/w2 * yy + q;",true);
                    s("mesh(xx,yy,zz);",true);
                    s("axis([0 1 0 1 0 1]);",true);
                    s("view(-37.5, 10);",true);
                    break;
            }
            if(saveImage)
                s("print -dpng screen"+(int)(System.currentTimeMillis()/1000)+".png",true);
        }
    }

    /**
     * It's a good method, but it works only when we have enough positive examples
     * with *all* similarities next to 1.0
     */ 
    private static double calculateBiasFactor() {
        double min_q = (double)n / 2.0;
        double max_q = 1.0 + Math.sqrt((double)n - 1.0);
        double q_0 = min_q + wpe * (max_q - min_q);
        w("q0 = "+q_0);
        return 1.0 + wpe * (max_q / min_q - 1.0);
    }

    private static Couple buildCouple(String str) {
        String[] ids = str.split("#");
        Resource r1 = null, r2 = null;
        for(Resource s : sources)
            if(ids[0].equals(s.getID())) {
                r1 = s;
                break;
            }
        for(Resource t : targets)
            if(ids[1].equals(t.getID())) {
                r2 = t;
                break;
            }
        Couple c = new Couple(r1, r2);
        c.initializeCount(n);
        computeSimilarity(c);
        return c;
    }

    private static void showPartial(int part) {
        double pre = tp+fp != 0 ? tp / (tp + fp) : 0;
        double rec = tp+fn != 0 ? tp / (tp + fn) : 0;
        double f1 = pre+rec != 0 ? 2 * pre * rec / (pre + rec) : 0;
        w("\n"+part+"% Results\n"
                + "f1 = "+f1+"\n"
                + "pre = "+pre+"\n"
                + "rec = "+rec+"\n"
                + "tp="+tp+", fp="+fp+", tn="+tn+", fn="+fn);
        if(sendEmail)
            Notifier.notify(f1, pre, rec, tp, fp, tn, fn, part);
    }

    private static void askOracle(ArrayList<Couple> selected) {
        for(int i=0; i<selected.size(); i++) {
            Couple couple = selected.get(i);
            if( isPositive( couple ) ) {
                actualPos.add( couple );
            } else {
                actualNeg.add( couple );
            }
            answered.add(couple);
            w("<"+couple.getSource().getID()+","+couple.getTarget().getID()+">");
            w("O(x) = "+isPositive( couple )+"\tC(x) = "+classify(couple));
        }
//                w("\nCan't launch SVM without positive or negative couples.");
//                w("Asking the oracle a few more questions...\n");
        if(actualPos.isEmpty()) {
            while(actualPos.isEmpty()) {
                // generates a fake positive example
                // TODO ask for some c=(s,t) with sim(c) > 0.8 or so
                Resource s = new Resource("GeneratedPositiveSourceExample");
                Resource t = new Resource("GeneratedPositiveTargetExample");
                sources.add(s);
                targets.add(t);
                for(String p : sources.get(0).getPropertyNames()) {
                    s.setPropertyValue(p, "GeneratedValue");
                    t.setPropertyValue(p, "GeneratedValue");
                }
                Couple c = new Couple(s, t);
                c.initializeCount(n);
                computeSimilarity(c);
                couples.add(c);
                actualPos.add(c);
                oraclesAnswers.add(c);
            }
        }
        if(actualNeg.isEmpty()) {
            ArrayList<Couple> added = new ArrayList<Couple>();
            while(actualNeg.size() < MOSTINF_NEG_CAND) {
                Couple c = new Couple(sources.get((int)Math.random()*sources.size()),
                        targets.get((int)(Math.random()*targets.size())));
                c.initializeCount(n);
                computeSimilarity(c);
                if(!isPositive(c) && !added.contains(c)) {
                    actualNeg.add(c);
                    couples.add(c);
                    added.add(c);
                }
            }
        }
    }

    
}
