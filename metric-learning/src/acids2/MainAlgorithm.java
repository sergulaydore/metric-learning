package acids2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import org.math.array.LinearAlgebra;

import utility.GammaComparator;

import com.aliasi.util.CollectionUtils;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import metriclearning.Couple1;

import acids2.test.Test;
import filters.StandardFilter;
import filters.mahalanobis.MahalaFilter;
import filters.reeded.ReededFilter;

// XXX Should we really keep on working on distances, while everybody is working on similarities?
// Placing the correct initial classifier seems to be a tough task on distances.

public class MainAlgorithm {
	
	// SVM parameters
	private static svm_model model;
	private static int KERNEL = svm_parameter.LINEAR;
	private static int DEGREE = 2;
	private static int COEF0 = 1;
	
	private static double[] w;
	private static double theta = 3.0; // TODO what's the "correct" theta???
	private static double beta = 0.5;
	private static int k = 5;
	private static int n = 0;
	
	private static int counter = 0; // TODO remove me after linking class to a real oracle
	
	public static void start(TreeSet<Resource> sources, TreeSet<Resource> targets) {
		
		// initialization start
		
		String[] propertyNames = new String[0];
		try {
			propertyNames = sources.first().getPropertyNames().toArray(propertyNames);
		} catch (NoSuchElementException e) {
			System.err.println("Source set is empty!");
			return;
		}
		
		n = propertyNames.length;
		w = new double[n];
		for(int i=0; i<n; i++)
			w[i] = 1.0;
		
		// initialization end
		
		TreeSet<Couple> intersection = new TreeSet<Couple>();
		while(true) {
			intersection.clear();
			for(int i=0; i<n; i++) {
				String pname = propertyNames[i];
				System.out.println("Processing property: "+pname);
				/*
				 * TODO
				 * - integrate svm
				 * - weight update
				 */
				TreeSet<Couple> join;
				double theta_i = (theta + beta) / w[i];
				System.out.println("theta_i = "+theta_i+"\tbeta = "+beta);
				// TODO datatype check.
				// TODO group all numeric values together and compute them at the end.
				if(i<2)
					join = ReededFilter.filter(sources, targets, pname, theta_i);
				else
					join = MahalaFilter.filter(intersection, pname, theta_i);
				
				if(i==0)
					intersection.addAll(join);
				else
					merge(intersection, join);
				System.out.println("intersection size: "+intersection.size());
			}
			if(blockingEndCondition(intersection))
				break;
			else {
				beta = beta + 1.0;
				System.out.println("broadening... beta = "+beta);
			}
		}
		
		ArrayList<Couple> posInformative = new ArrayList<Couple>();
		ArrayList<Couple> negInformative = new ArrayList<Couple>();
		for(Couple c : intersection)
			if(isInformative(c)) {
				double gamma = computeGamma(c.getDistances());
		        c.setGamma(gamma); // XXX do we really need this?
				if(c.isPositive())
					posInformative.add(c);
				else
					negInformative.add(c);
			}
		
		// TODO check if pos/negInformative are empty...
		
		Collections.sort(posInformative, new GammaComparator());
		Collections.sort(negInformative, new GammaComparator());
		
		TreeSet<Couple> posMostInformative = new TreeSet<Couple>();
		TreeSet<Couple> negMostInformative = new TreeSet<Couple>();
		for(int i=0; i<k; i++) {
			Couple c1 = posInformative.get(i);
			posMostInformative.add(c1);
			System.out.println("P: "+c1+" -> "+c1.getMeanDist()+" -> "+c1.getGamma());
			Couple c2 = negInformative.get(i);
			negMostInformative.add(c2);
			System.out.println("N: "+c2+" -> "+c2.getMeanDist()+" -> "+c2.getGamma());
		}
		
		ArrayList<Couple> poslbl = new ArrayList<Couple>();
		ArrayList<Couple> neglbl = new ArrayList<Couple>();
		for(Couple c : posMostInformative)
			if(askOracle(c))
				poslbl.add(c);
			else
				neglbl.add(c);
		for(Couple c : negMostInformative)
			if(askOracle(c))
				poslbl.add(c);
			else
				neglbl.add(c);
		
		// search for one more pos/neg example if the set is empty
		if(poslbl.size() == 0)
			for(Couple c : posInformative)
				if(!posMostInformative.contains(c))
					if(askOracle(c) == true) {
						poslbl.add(c);
						break;
					}
		// if(poslbl.size() == 0)
		// 		repeat everything with a lower theta...
		if(neglbl.size() == 0)
			for(Couple c : negInformative)
				if(!negMostInformative.contains(c))
					if(askOracle(c) == false) {
						neglbl.add(c);
						break;
					}
		// if(neglbl.size() == 0)
		// 		repeat everything with a higher theta...
		
		traceSvm(poslbl, neglbl);
		
		double tp = 0, tn = 0, fp = 0, fn = 0;
		for(Couple c : poslbl)
			if(classify(c)) tp++;
			else fn++;
		for(Couple c : neglbl)
			if(classify(c)) fp++;
			else tn++;
        double pre = tp+fp != 0 ? tp / (tp + fp) : 0;
        double rec = tp+fn != 0 ? tp / (tp + fn) : 0;
        double f1 = pre+rec != 0 ? 2 * pre * rec / (pre + rec) : 0;
        System.out.println("pre = "+pre+", rec = "+rec);
        System.out.println("f1 = "+f1+" (tp="+tp+", fp="+fp+", tn="+tn+", fn="+fn+")");
        System.out.println("questions submitted: "+counter);
        
		// testing...
		
		System.out.println("");
		
		testWholeDataset(sources, targets);
		
//			System.out.println(c.getSource().getPropertyValue("authors")+" | "+
//					c.getTarget().getPropertyValue("authors")+" -> "+
//					c.getDistances().get(0)+" -> "+classify(c,w,theta)+"\t"+askOracle(c.toString()));
		
	}

	private static void testWholeDataset(TreeSet<Resource> sources, TreeSet<Resource> targets) {
		double tp = 0, tn = 0, fp = 0, fn = 0;
		for(Resource s : sources) {
			for(Resource t : targets) {
//				Couple c = new Couple(s, t);
				double[] val = new double[n];
				int i = 0;
				for(String p : s.getPropertyNames()) {
//					c.addDistance(StandardFilter.wed.proximity(
//					s.getPropertyValue(p), t.getPropertyValue(p) ));
					val[i] = StandardFilter.wed.proximity(s.getPropertyValue(p), t.getPropertyValue(p));
					i++;
				}
				if(askOracle(s.getID()+"#"+t.getID())) {
					if(classify(val)) tp++;
					else fn++;
				} else {
					if(classify(val)) fp++;
					else tn++;
				}
			}
			System.out.print(".");
		}
		System.out.println();
        double pre = tp+fp != 0 ? tp / (tp + fp) : 0;
        double rec = tp+fn != 0 ? tp / (tp + fn) : 0;
        double f1 = pre+rec != 0 ? 2 * pre * rec / (pre + rec) : 0;
        System.out.println("pre = "+pre+", rec = "+rec);
        System.out.println("f1 = "+f1+" (tp="+tp+", fp="+fp+", tn="+tn+", fn="+fn+")");
	}

	private static boolean blockingEndCondition(TreeSet<Couple> intersection) {
		int pos = 0;
		for(Couple c : intersection)
			if(classify(c)) {
				pos++;
				c.setPositive(true);
			} else
				c.setPositive(false);
		int neg = intersection.size() - pos;
		return pos >= k && neg >= k;
	}

	private static void merge(TreeSet<Couple> intersection, TreeSet<Couple> join) {
	    Iterator<Couple> e = intersection.iterator();
	    while (e.hasNext()) {
	    	Couple c = e.next();
	        if (!join.contains(c))
		        e.remove();
	        else {
	        	for(Couple cj : join)
	        		if(cj.equals(c))
	        			c.addDistance(cj.getDistances().get(0));
	        }	
	    }
	}
	
	private static boolean classify(Couple c) {
		if(model == null) {
			// Default classifier is always set as linear.
			double sum = 0.0;
			ArrayList<Double> dist = c.getDistances();
			for(int i=0; i<dist.size(); i++)
				sum += dist.get(i) * w[i];
			return sum <= theta;
		}
        svm_node[] node = new svm_node[n];
        for(int i=0; i<n; i++) {
        	node[i] = new svm_node();
        	node[i].index = i;
        	node[i].value = c.getDistances().get(i);
        }
        if(svm.svm_predict(model, node) == 1.0)
        	return true;
        else return false;
	}

	private static boolean classify(double[] val) {
        svm_node[] node = new svm_node[n];
        for(int i=0; i<n; i++) {
        	node[i] = new svm_node();
        	node[i].index = i;
        	node[i].value = val[i];
        }
        if(svm.svm_predict(model, node) == 1.0)
        	return true;
        else return false;
	}
	
	
	private static boolean isInformative(Couple c) {
		double sum = 0.0;
		ArrayList<Double> dist = c.getDistances();
		for(int i=0; i<dist.size(); i++)
			sum += dist.get(i) * w[i];
		return (theta-beta) <= sum && sum <= (theta+beta);
	}
	
	// XXX How to compute gamma (and informative examples) for polynomial classifiers? :)
    private static double computeGamma(ArrayList<Double> dist) {
        double numer = 0.0, denom = 0.0;
        for(int i=0; i<dist.size(); i++) {
            numer += dist.get(i) * w[i];
            denom += Math.pow(w[i], 2);
        }
        numer += theta;
        denom = Math.sqrt(denom);
        return Math.abs(denom/numer); // gamma = 1/D
    }

    private static boolean askOracle(Couple c) {
    	counter++;
    	String ids = c.getSource().getID()+"#"+c.getTarget().getID();
		return Test.askOracle(ids); // TODO remove me & add interaction
	}

    private static boolean askOracle(String ids) {
		return Test.askOracle(ids); // TODO remove me & add interaction
	}

    private static void traceSvm(ArrayList<Couple> poslbl, ArrayList<Couple> neglbl) {
    	
        int size = poslbl.size() + neglbl.size();

        // build x,y vectors
        svm_node[][] x = new svm_node[size][n];
        double[] y = new double[size];
        for(int i=0; i<poslbl.size(); i++) {
            ArrayList<Double> arr = poslbl.get(i).getDistances();
            for(int j=0; j<arr.size(); j++) {
                x[i][j] = new svm_node();
                x[i][j].index = j;
                x[i][j].value = arr.get(j);
            }
            y[i] = 1;
        }
        for(int i=poslbl.size(); i<size; i++) {
            ArrayList<Double> arr = neglbl.get(i-poslbl.size()).getDistances();
            for(int j=0; j<arr.size(); j++) {
                x[i][j] = new svm_node();
                x[i][j].index = j;
                x[i][j].value = arr.get(j);
            }
            y[i] = -1;
        }
        
        // configure model
        // POLY: (gamma*u'*v + coef0)^degree
        svm_problem problem = new svm_problem();
        problem.l = size;
        problem.x = x;
        problem.y = y;
        svm_parameter parameter = new svm_parameter();
        parameter.C = 1E+6;
        parameter.svm_type = svm_parameter.C_SVC;
        parameter.kernel_type = KERNEL;
        if(KERNEL == svm_parameter.POLY) {
			parameter.degree = DEGREE; // default: 3
			parameter.coef0  = COEF0; // default: 0
			parameter.gamma  = 1; // default: 1/n
        } 
        parameter.eps = 0.001;
        model = svm.svm_train(problem, parameter);
        // sv = ( nSV ; n )
        svm_node[][] sv = model.SV;
        // sv_coef = ( 1 ; nSV )
        double[][] sv_coef = model.sv_coef;
        
        // vec = sv' * sv_coef' = (sv_coef * sv)' = ( n ; 1 )
        double[][] vec = new double[sv[0].length][sv_coef.length];
        // converting sv to double -> sv_d
        double[][] sv_d = new double[sv.length][sv[0].length];
        for(int i=0; i<sv.length; i++)
            for(int j=0; j<sv[i].length; j++)
                sv_d[i][j] = sv[i][j].value;
        vec = LinearAlgebra.transpose(LinearAlgebra.times(sv_coef, sv_d));
        
        int signum = (model.label[0] == -1.0) ? 1 : -1;
        for(int i=0; i<n; i++) {
            w[i] = signum * vec[i][0];
            System.out.println("w_"+i+" = "+w[i]);
        }
        
        theta = signum * model.rho[0];
        System.out.println("theta = "+theta);
        
    }
	
}
