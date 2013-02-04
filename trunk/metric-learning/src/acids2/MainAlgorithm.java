package acids2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.math.array.LinearAlgebra;

import utility.GammaComparator;
import utility.Transform;
import acids2.plot.Svm3D;
import acids2.test.Test;
import filters.StandardFilter;
import filters.WeightedEditDistanceFilter;
import filters.mahalanobis.MahalaFilter;
import filters.reeded.ReededFilter;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class MainAlgorithm {
	
	// SVM parameters
	private static svm_model model;
	private static svm_problem problem;
	private static int KERNEL = svm_parameter.POLY;
	private static int DEGREE = 2;
	private static int COEF0 = 0;
	
	// classifier properties
	private static double[][] sv_d;
	private static double[] w;
	private static double theta0;
	private static double theta;
	private static double beta = 1.0;

	private static int k = 20;
	private static int n;
	
	// support variables
	private static int TOTAL_MAX_QUESTIONS = 50; // TODO
	private static int counter = 0; // TODO remove me after linking class to a real oracle
	private static double tp0 = 0, tn0 = 0, fp0 = 0, fn0 = 0;
	private static svm_node[][] orig_x; 

	private static ArrayList<Property> props = new ArrayList<Property>();
	
	/*
	 * TODO
	 * - Work with distances. Why? Because we can (almost) always discard a lot of examples.
	 * - Weight matrix update.
	 */
	public static void start(TreeSet<Resource> sources, TreeSet<Resource> targets) {
		
		// initialization
		
		ArrayList<String> propertyNames;
		try {
			propertyNames = sources.first().getPropertyNames();
		} catch (NoSuchElementException e) {
			System.err.println("Source set is empty!");
			return;
		}
		
		for(String pn : propertyNames) {
			int cnt = 0, type = Property.TYPE_NUMERIC;
			for(Resource s : sources) {
				if(s.checkDatatype(pn) == Property.TYPE_STRING) {
					type = Property.TYPE_STRING;
					break;
				}
				cnt++;
				if(cnt == 10) {
					type = Property.TYPE_NUMERIC;
					break;
				}
			}
			props.add(new Property(pn, type));
		}
		
		for(Property p : props)
			System.out.println(p.getName()+"\t"+p.getDatatype());
		
		n = propertyNames.size();
		w = new double[n];
		for(int i=0; i<n; i++)
			w[i] = 1.0;
		theta0 = (double) n / 2.0;
		theta = theta0;
		
		// XXX Group all numeric values together and compute them at the end (?)
		//   MahalaFilter should be called 0 or 1 time outside the for(i=0 to n).
		
		// algorithm
		
		TreeSet<Couple> intersection = new TreeSet<Couple>();
		while(true) {
			intersection.clear();
			// first property works on the entire Cartesian product.
			Property p0 = props.get(0);
			String pname0 = p0.getName();
			System.out.println("Processing property: "+pname0);
			double thr_0 = computeThreshold(0);
			System.out.println("thr_0 = "+thr_0);
			intersection = p0.getFilter().filter(sources, targets, pname0, thr_0);
			System.out.println("intersection size: "+intersection.size());
		
			for(int i=1; i<n; i++) {
				String pname = propertyNames.get(i);
				System.out.println("Processing property: "+pname);
				double thr_i = computeThreshold(i);
				System.out.println("thr_"+i+" = "+thr_i);
				TreeSet<Couple> join = props.get(i).getFilter().filter(intersection, pname, thr_i);
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
		/*
		 * TODO & FIXME Classifiers are still awful.
		 * 
		 * Since the lowest values of gamma tend to select the "weirdest" points,
		 * such as positives having x,y distances greater than the negatives,
		 * I'd suggest to select k more points with the lowest cumulative distance
		 * (likely positives) and k with the highest (likely negatives).
		 */
		ArrayList<Couple> posInformative = new ArrayList<Couple>();
		ArrayList<Couple> negInformative = new ArrayList<Couple>();
		for(Couple c : intersection)
			if(isInformative(c)) {
				double gamma = computeGamma(c.getDistances());
		        c.setGamma(gamma);
				if(c.isPositive()) {
					posInformative.add(c);
				} else
					negInformative.add(c);
			}
		
		// TODO check if pos/negInformative cardinality is < k...
		// insert the code above into the blockingEndCondition (and if |·|<k then move theta)
		
		Collections.sort(posInformative, new GammaComparator());
		Collections.sort(negInformative, new GammaComparator());
				
		TreeSet<Couple> posMostInformative = new TreeSet<Couple>();
		TreeSet<Couple> negMostInformative = new TreeSet<Couple>();
		for(int i=0; i<k; i++) {
			Couple c1 = posInformative.get(i);
			posMostInformative.add(c1);
//			System.out.println("Pos? "+c1+" -> "+c1.getMeanDist()+" -> "+c1.getGamma());
			Couple c2 = negInformative.get(i);
			negMostInformative.add(c2);
//			System.out.println("Neg? "+c2+" -> "+c2.getMeanDist()+" -> "+c2.getGamma());
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

		// search for one more pos/neg example if they're < k
		if(poslbl.size() < k)
			for(Couple c : posInformative)
				if(!posMostInformative.contains(c))
					if(askOracle(c) == true) {
						poslbl.add(c);
						if(poslbl.size() == k)
							break;
					} else neglbl.add(c);
		if(poslbl.size() < k) { // TODO repeat everything...
			System.out.println(poslbl.size()+" pos labeled. Broadening beta...");
			beta += 0.5;
			props.clear(); // TODO find a more elegant way.
			start(sources, targets);
			return;
		}
		if(neglbl.size() < k)
			for(Couple c : negInformative)
				if(!negMostInformative.contains(c))
					if(askOracle(c) == false) {
						neglbl.add(c);
						if(neglbl.size() == k)
							break;
					} else poslbl.add(c);
		if(neglbl.size() < k) { // TODO repeat everything...
			System.out.println(neglbl.size()+" neg labeled. Broadening beta...");
			beta += 0.5;
			props.clear();
			start(sources, targets);
			return;
		}
		
		normalizeNumValues(poslbl, neglbl);
		
		System.out.println("Pos Labeled:");
		for(Couple c:poslbl) { 
			for(double d : c.getDistances())
				System.out.print(d+", ");
			System.out.println("\t"+c);
		}
		System.out.println("Neg Labeled:");
		for(Couple c:neglbl) { 
			for(double d : c.getDistances())
				System.out.print(d+", ");
			System.out.println("\t"+c);
		}

		System.out.println("Labeled pos: "+poslbl.size());
		System.out.println("Labeled neg: "+neglbl.size());
        System.out.println("Questions submitted: "+counter);
		
		traceSvm(poslbl, neglbl);
		
		for(Couple c : poslbl) {
			if(classify(c)) tp0++;
			else fn0++;
//			for(double d : c.getDistances())
//				System.out.print(d+"\t");
//			System.out.println();
		}
		for(Couple c : neglbl) {
			if(classify(c)) fp0++;
			else tn0++;
//			for(double d : c.getDistances())
//				System.out.print(d+"\t");
//			System.out.println();
		}
        double pre = tp0+fp0 != 0 ? tp0 / (tp0 + fp0) : 0;
        double rec = tp0+fn0 != 0 ? tp0 / (tp0 + fn0) : 0;
        double f1 = pre+rec != 0 ? 2 * pre * rec / (pre + rec) : 0;
        System.out.println("pre = "+pre+", rec = "+rec);
        System.out.println("f1 = "+f1+" (tp="+tp0+", fp="+fp0+", tn="+tn0+", fn="+fn0+")");
        
		// testing...
		
		System.out.println("");

		testWholeDataset(sources, targets);
		
		Svm3D.draw(model, problem, theta, sv_d, theta0);
		
//			System.out.println(c.getSource().getPropertyValue("authors")+" | "+
//					c.getTarget().getPropertyValue("authors")+" -> "+
//					c.getDistances().get(0)+" -> "+classify(c,w,theta)+"\t"+askOracle(c.toString()));
		
	}

	private static void normalizeNumValues(ArrayList<Couple> poslbl, ArrayList<Couple> neglbl) {
		// normalize numeric values... [0,1]
		for(int i=0; i<props.size(); i++)
			if(props.get(i).getDatatype() == Property.TYPE_NUMERIC) {
				double dmin = Double.POSITIVE_INFINITY;
				double dmax = Double.NEGATIVE_INFINITY;
				for(Couple c : poslbl) {
					double d = c.getDistances().get(i);
					if(d < dmin) dmin = d;
					if(d > dmax) dmax = d;
				}
				for(Couple c : neglbl) {
					double d = c.getDistances().get(i);
					if(d < dmin) dmin = d;
					if(d > dmax) dmax = d;
				}
				System.out.println("dmax = "+dmax+"; dmin = "+dmin);
				for(Couple c : poslbl) {
					double d = c.getDistances().get(i);
					if(dmax - dmin == 0.0)
						c.setDistance(1.0, i);
					else
						c.setDistance( 1.0 - (d-dmin)/(dmax-dmin), i);
				}
				for(Couple c : neglbl) {
					double d = c.getDistances().get(i);
					if(dmax - dmin == 0.0)
						c.setDistance(1.0, i);
					else
						c.setDistance( 1.0 - (d-dmin)/(dmax-dmin), i);
				}
			}
	}

	private static double computeThreshold(int j) {
		return (theta + beta) / w[j];
	}

	private static void testWholeDataset(TreeSet<Resource> sources, TreeSet<Resource> targets) {
		int cnt = 0;
		double tp = tp0, tn = tn0, fp = fp0, fn = fn0;
		
		final int BREAK_AT = 100000;
        svm_node[][] x2 = new svm_node[problem.l+BREAK_AT][n];
        for(int i=0; i<problem.x.length; i++)
        	for(int j=0; j<problem.x[i].length; j++)
        		x2[i][j] = problem.x[i][j];
        double[] y2 = new double[problem.l+BREAK_AT];
        for(int i=0; i<problem.y.length; i++)
    		y2[i] = problem.y[i];
        
        ArrayList<Resource> src = new ArrayList<Resource>(sources);
        ArrayList<Resource> tgt = new ArrayList<Resource>(targets);
        ArrayList<String> ids = new ArrayList<String>();
        
        while(cnt < BREAK_AT) {
        	Resource s = src.get((int)(src.size()*Math.random()));
        	Resource t = tgt.get((int)(tgt.size()*Math.random()));
//		for(Resource s : sources) {
//			for(Resource t : targets) {
//				Couple c = new Couple(s, t);
				double[] val = new double[n];
				int i = 0;
				for(Property prop : props) {
//					c.addDistance(StandardFilter.wed.proximity(
//					s.getPropertyValue(p), t.getPropertyValue(p) ));
					String p = prop.getName();
					val[i] = prop.getFilter().getDistance(s.getPropertyValue(p), t.getPropertyValue(p));
	                x2[problem.l+cnt][i] = new svm_node();
	                x2[problem.l+cnt][i].index = i;
	                x2[problem.l+cnt][i].value = val[i];
					i++;
				}
//				System.out.println();
//				System.out.println(s.getID()+"#"+t.getID()+"\t"
//						+askOracle(s.getID()+"#"+t.getID()));
				ids.add(s.getID()+"#"+t.getID());
				
				cnt++;
//				if(cnt == BREAK_AT) break; // TODO remove me
//			}
			if(cnt % 1000 == 0)
				System.out.print(".");
			
		}
		// TODO normalize numeric values
		for(int j=0; j<props.size(); j++) {
			if(props.get(j).getDatatype() == Property.TYPE_NUMERIC) {
		        for(int i=0; i<orig_x.length; i++)
		    		x2[i][j].value = orig_x[i][j].value;
				double dmin = Double.POSITIVE_INFINITY, dmax = Double.NEGATIVE_INFINITY;
				for(int i=0; i<x2.length; i++) {
		            if(x2[i][j].value < dmin) dmin = x2[i][j].value;
		            if(x2[i][j].value > dmax) dmax = x2[i][j].value;
				}
				for(int i=0; i<x2.length; i++)
		            x2[i][j].value = 1.0 - (x2[i][j].value - dmin) / (dmax - dmin);
			}
		}
		
		// predicts the new points
		for(int i=0; i<ids.size(); i++) {
			String id = ids.get(i);
			if(askOracle(id)) {
				if(classify(x2[problem.l+i])) tp++;
				else fn++;
				y2[problem.l+i] = 1;
			} else {
				if(classify(x2[problem.l+i])) fp++;
				else tn++;
				y2[problem.l+i] = -1;
			}
		}
        
		problem.x = x2;
		problem.y = y2;
		problem.l += cnt;
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
		System.out.println("Found: #pos = "+pos+"\t#neg = "+neg);
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
	        		if(cj.equals(c)) {
	        			c.addDistance(cj.getDistances().get(0));
	        			break;
	        		}
	        }	
	    }
	}
	
	private static boolean classify(Couple c) {
		if(model == null) {
			// Default classifier is always set to linear.
			double sum = 0.0;
			ArrayList<Double> dist = c.getDistances();
			for(int i=0; i<dist.size(); i++)
				sum += dist.get(i) * w[i];
			return sum >= theta;
		}
        svm_node[] node = new svm_node[n];
        for(int i=0; i<n; i++) {
        	node[i] = new svm_node();
        	node[i].index = i;
        	node[i].value = c.getDistances().get(i);
        }
        return classify(node);
	}

	private static boolean classify(svm_node[] val) {
        if(svm.svm_predict(model, val) == 1.0)
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
	
	// gamma = distance from classifier
    private static double computeGamma(ArrayList<Double> dist) {
        double numer = 0.0, denom = 0.0;
        for(int i=0; i<dist.size(); i++) {
            numer += dist.get(i) * w[i];
            denom += Math.pow(w[i], 2);
        }
        numer -= theta;
        denom = Math.sqrt(denom);
        return Math.abs(numer/denom);
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
        problem = new svm_problem();
        problem.l = size;
        problem.x = x;
        problem.y = y;
        orig_x = x.clone();
        svm_parameter parameter = new svm_parameter();
        parameter.C = 1E+3;
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
        sv_d = new double[sv.length][sv[0].length];
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
