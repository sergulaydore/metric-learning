package acids2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import utility.ValueParser;
import acids2.plot.Svm3D;
import acids2.test.Test;
import filters.StandardFilter;
import filters.WeightedNgramFilter;
import filters.mahalanobis.MahalaFilter;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class MainAlgorithm {
		
	// SVM parameters
	private static svm_model model;
	private static svm_problem problem;
	private static int KERNEL = svm_parameter.POLY;
	private static final int DEGREE = 2;
	private static final int COEF0 = 0;
	
	// classifier properties
	private static double[][] sv_d;
	private static double[] w;
	private static double theta0;
	private static double theta;
	private static double beta; // TODO call it Delta (Δ)
	private static int n;
	/*
	 * k = 20, theta0 = n-0.5
	 * 		dblp-acm		~96%
	 * 		dblp-scholar	~97%
	 * 		abt-buy			~75%
	 * 
	 * XXX is there a correlation between th0c and the similarity mean for each property?
	 * 
	 * TODO create Settings static class
	 * TODO save similarity to array for Octave
	 */
	private static double th0c;
	private static int k;
	
	// support variables
	private static final int MAX_QUERIES = 50;
	private static final int MAX_ITER_PERCEPTRON = 100;
	private static final int BACKUP_SIZE_RATE = 10;
	private static int counter = 0; // TODO remove me after linking class to a real oracle
	private static double tp0 = 0, tn0 = 0, fp0 = 0, fn0 = 0;
	private static svm_node[][] orig_x; 

	private static ArrayList<Property> props = new ArrayList<Property>();
    private static HashMap<Integer, ArrayList<Double>> extrema = new HashMap<Integer, ArrayList<Double>>();


    public static void start(TreeSet<Resource> sources, TreeSet<Resource> targets, double _th0c, int _k, double _beta) {
		long t0 = System.currentTimeMillis(); 
				
    	th0c = _th0c;
    	k = _k;
    	beta = _beta;
    	
		// initialization
		
		ArrayList<String> propertyNames;
		try {
			propertyNames = sources.first().getPropertyNames();
		} catch (NoSuchElementException e) {
			System.err.println("Source set is empty!");
			return;
		}
		
		int index = 0;
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
			Property p = new Property(pn, type, index);
			props.add(p);
			
			index++;
		}
		
		for(Property p : props) {
			System.out.println(p.getName()+"\t"+p.getDatatypeAsString());
			if(p.getDatatype() == Property.TYPE_NUMERIC)
				computeExtrema(p.getIndex(), sources, targets);
		}
		
		n = propertyNames.size();
		w = new double[n];
		for(int i=0; i<n; i++)
			w[i] = 1.0;
		theta0 = (double) n - 0.5; // / 2.0 * th0c;
		theta = theta0;
		
		// algorithm
		
		TreeSet<Couple> intersection = new TreeSet<Couple>();
		while(true) {
			intersection.clear();
			for(int i=0; i<n; i++) {
				String pname = propertyNames.get(i);
				System.out.println("Processing property: "+pname);
				double thr_i = computeThreshold(i);
				System.out.println("thr_"+i+" = "+thr_i);
				if(i==0) // first property works on the entire Cartesian product.
					intersection = props.get(i).getFilter().filter(sources, targets, pname, thr_i);
				else
					merge(intersection, props.get(i).getFilter().filter(intersection, pname, thr_i));
				System.out.println("intersection size: "+intersection.size());
			}
			if(blockingEndCondition(intersection, sources.size()*targets.size()))
				break;
			else {
				beta = beta + 0.1;
				System.out.println("broadening... beta = "+beta);
			}
		}

		normalizeNumValues(intersection);
		
		ArrayList<Couple> posInformative = new ArrayList<Couple>();
		ArrayList<Couple> negInformative = new ArrayList<Couple>();
		
		for(Couple c : intersection) {
	        c.setGamma( computeGamma( c.getDistances() ) );
			if(classify(c))
				posInformative.add(c);
			else
				negInformative.add(c);
		}
		System.out.println("theta = "+theta+"\tpos = "+posInformative.size()+"\tneg = "+negInformative.size());
					
		Collections.sort(posInformative, new GammaComparator());
		Collections.sort(negInformative, new GammaComparator());
				
		TreeSet<Couple> posMostInformative = new TreeSet<Couple>();
		TreeSet<Couple> negMostInformative = new TreeSet<Couple>();
		
		for(int i=0; i<k; i++) {
			if(i < posInformative.size())
				posMostInformative.add(posInformative.get(i));
			if(i < negInformative.size())
				negMostInformative.add(negInformative.get(i));
		}
		
		long t1 = System.currentTimeMillis(); 
		
		// active learning phase
		// TODO add big loop
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

		// search for one more pos/neg example if there are none
		if(poslbl.isEmpty())
			for(Couple c : posInformative)
				if(!posMostInformative.contains(c)) {
					if(counter >= MAX_QUERIES)
						break;
					if(askOracle(c) == true) {
						poslbl.add(c);
						break;
					} else neglbl.add(c);
				}
		if(neglbl.isEmpty())
			for(Couple c : negInformative)
				if(!negMostInformative.contains(c)) {
					if(counter >= MAX_QUERIES)
						break;
					if(askOracle(c) == false) {
						neglbl.add(c);
						break;
					} else poslbl.add(c);
				}
		
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
		
		long t2 = System.currentTimeMillis();
        
        // perceptron learning phase
        TreeSet<Couple> fpC = new TreeSet<Couple>();
        TreeSet<Couple> fnC = new TreeSet<Couple>();
        for(int i_perc=0; true; i_perc++) {
        	System.out.println("PERCEPTRON: iteration #"+i_perc);
        	fnC.clear(); fpC.clear();
        	tp0 = 0; fp0 = 0; tn0 = 0; fn0 = 0;
			traceSvm(poslbl, neglbl);
		
			for(Couple c : poslbl)
				if(classify(c))
					tp0++;
				else {
					fn0++;
					fnC.add(c);
				}
			for(Couple c : neglbl)
				if(classify(c)) {
					fp0++;
					fpC.add(c);
				} else
					tn0++;
			
	        if(perceptronEndCondition(i_perc, getFScore(tp0, fp0, tn0, fn0)))
	        	break;
	        else {
	        	updateWeights(fpC, fnC);
	        	updateSimilarities(fpC, fnC);
	        }
	    }
        
        for(Property p : props)
        	if(p.getDatatype() == Property.TYPE_STRING)
        		System.out.println("WEIGHTS ("+p.getName()+"): "+p.getFilter().getWeights());
        
		long t3 = System.currentTimeMillis(); 
		System.out.println("== EXECUTION TIME (seconds) ==");
		System.out.println("Preparation     \t" + (t1-t0)/1000.0);
		System.out.println("Active learning \t" + (t2-t1)/1000.0);
		System.out.println("SVM + Perceptron\t" + (t3-t2)/1000.0);
		System.out.println("Total exec. time\t" + (t3-t0)/1000.0);

		// testing...
		
		System.out.println("");

		subsetEvaluation(sources, targets);
//		evaluation(sources, targets);
		
		try {
			Svm3D.draw(model, problem, theta, sv_d, theta0);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("3D: no plot (n < 3).");
		}
		
	}

	private static void updateSimilarities(TreeSet<Couple> fpC,
			TreeSet<Couple> fnC) {
		for(Property p : props) {
			if(p.getDatatype() == Property.TYPE_STRING) {
				StandardFilter filter = p.getFilter();
				String pname = p.getName();
				int index = p.getIndex();
				for(Couple c : fpC)
					c.setDistance(filter.getDistance(c.getSource().getPropertyValue(pname),
							c.getTarget().getPropertyValue(pname)), index);
				for(Couple c : fnC)
					c.setDistance(filter.getDistance(c.getSource().getPropertyValue(pname),
							c.getTarget().getPropertyValue(pname)), index);
			}
		}
	}

	private static void updateWeights(TreeSet<Couple> fpC, TreeSet<Couple> fnC) {
		for(Property p : props) {
			if(p.getDatatype() == Property.TYPE_STRING) {
				// TODO find alternative to casting: method .prepare() in StandardFilter
				WeightedNgramFilter rf = (WeightedNgramFilter)(p.getFilter());
				String pname = p.getName();
				for(Couple c : fpC)
					rf.prepareNgCache(c.getSource().getPropertyValue(pname), 
							c.getTarget().getPropertyValue(pname), false, 3);
				for(Couple c : fnC)
					rf.prepareNgCache(c.getSource().getPropertyValue(pname), 
							c.getTarget().getPropertyValue(pname), true, 3);
				rf.updateWeights();
			}
		}
	}

	private static boolean perceptronEndCondition(int i_perc, double f1) {
		return i_perc >= MAX_ITER_PERCEPTRON || f1 == 1.0;
	}

	private static void normalizeNumValues(TreeSet<Couple> intersection) {
		// normalize numeric values... [0,1]
		for(int i=0; i<props.size(); i++)
			if(props.get(i).getDatatype() == Property.TYPE_NUMERIC)
				for(Couple c : intersection)
					c.setDistance( normalize(c.getDistances().get(i), i), i );
	}

	private static double computeThreshold(int j) {
		double sum = 0.0;
		for(int i=0; i<n; i++)
			if(i != j)
				sum += w[i];
		return (theta - beta - sum) / w[j];
	}

	@SuppressWarnings("unused")
	private static void evaluation(TreeSet<Resource> sources, TreeSet<Resource> targets) {
		long cnt = 0;
		double tp = 0, tn = 0, fp = 0, fn = 0;
		double[] val = new double[n];
		
		for(Resource s : sources) {
			for(Resource t : targets) {
				for(int j=0; j<props.size(); j++) {
					Property prop = props.get(j);
					String p = prop.getName();
					val[j] = prop.getFilter().getDistance(s.getPropertyValue(p), t.getPropertyValue(p));
	                if(prop.getDatatype() == Property.TYPE_NUMERIC)
	                	val[j] = normalize(val[j], j);
				}
				if(askOracle(s.getID()+"#"+t.getID())) {
					if(classify(val))
						tp++;
					else
						fn++;
				} else {
					if(classify(val))
						fp++;
					else
						tn++;
				}
				cnt++;
				if(cnt % 100000 == 0)
					System.out.print(".");
			}
		}
		System.out.println();
        
		getFScore(tp, fp, tn, fn);
	}
	
	private static double getFScore(double tp, double fp, double tn, double fn) {
        double pre = tp+fp != 0 ? tp / (tp + fp) : 0;
        double rec = tp+fn != 0 ? tp / (tp + fn) : 0;
        double f1 = pre+rec != 0 ? 2 * pre * rec / (pre + rec) : 0;
        System.out.println("pre = "+pre+", rec = "+rec);
        System.out.println("f1 = "+f1+" (tp="+tp+", fp="+fp+", tn="+tn+", fn="+fn+")");
        return f1;
	}


	private static void subsetEvaluation(TreeSet<Resource> sources, TreeSet<Resource> targets) {
		double tp = tp0, tn = tn0, fp = fp0, fn = fn0;
		
		final int BREAK_AT = 20000;
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
        
        for(int i=0; i<BREAK_AT; i++) {
        	Resource s = src.get( (int)(src.size()*Math.random()) );
        	Resource t = tgt.get( (int)(tgt.size()*Math.random()) );
        	if(ids.contains(s.getID()+"#"+t.getID())) {
        		i--;
        	} else {
				double val;
				for(int j=0; j<props.size(); j++) {
					Property prop = props.get(j);
					String p = prop.getName();
					val = prop.getFilter().getDistance(s.getPropertyValue(p), t.getPropertyValue(p));
	                x2[problem.l+i][j] = new svm_node();
	                x2[problem.l+i][j].index = j;
	                if(prop.getDatatype() == Property.TYPE_NUMERIC)
	                	x2[problem.l+i][j].value = normalize(val, j);
	                else
	                	x2[problem.l+i][j].value = val;
				}
				ids.add(s.getID()+"#"+t.getID());
				if(i % 1000 == 0)
					System.out.print(".");
        	}
		}
		System.out.println();
        
		// recover old values
		for(int j=0; j<props.size(); j++)
			if(props.get(j).getDatatype() == Property.TYPE_NUMERIC)
		        for(int i=0; i<orig_x.length; i++)
		    		x2[i][j].value = orig_x[i][j].value;
		
		// predicts the test set
		for(int i=0; i<ids.size(); i++) {
			if(askOracle(ids.get(i))) {
				if(classify(x2[problem.l+i])) tp++;
				else { fn++; System.out.println("FN: "+ids.get(i)); }
				y2[problem.l+i] = 1;
			} else {
				if(classify(x2[problem.l+i])) { fp++; System.out.println("FP: "+ids.get(i)); }
				else tn++;
				y2[problem.l+i] = -1;
			}
		}
        
		problem.x = x2;
		problem.y = y2;
		problem.l += ids.size();
		
		getFScore(tp, fp, tn, fn);
	}

	private static double normalize(double value, int j) {
		// XXX Absent/incomplete information: What to do? 
		if(Double.isNaN(value))
			return 0.0;
		ArrayList<Double> ext = extrema.get(j);
		double maxS = ext.get(0), minS = ext.get(1), maxT = ext.get(2), minT = ext.get(3);
		double denom = Math.max(maxT - minS, maxS - minT);
		if(denom == 0.0)
			return 1.0;
		else
			return 1.0 - value / denom;
	}

	private static void computeExtrema(int index, TreeSet<Resource> sources, TreeSet<Resource> targets) {
		ArrayList<Double> ext = new ArrayList<Double>();
		String pname = props.get(index).getName();
		double maxS = Double.NEGATIVE_INFINITY, minS = Double.POSITIVE_INFINITY;
		for(Resource s : sources) {
			double d = ValueParser.parse( s.getPropertyValue(pname) );
			if(d > maxS) maxS = d;
			if(d < minS) minS = d;
		}
		ext.add(maxS);
		ext.add(minS);
		double maxT = Double.NEGATIVE_INFINITY, minT = Double.POSITIVE_INFINITY;
		for(Resource t : targets) {
			double d = ValueParser.parse( t.getPropertyValue(pname) );
			if(d > maxT) maxT = d;
			if(d < minT) minT = d;
		}
		ext.add(maxT);
		ext.add(minT);
		extrema.put(index, ext);
		((MahalaFilter) props.get(index).getFilter()).setExtrema(ext);
		System.out.println(ext.toString());
	}

	private static boolean blockingEndCondition(TreeSet<Couple> intersection, int size) {
		int backupSetSize = k * BACKUP_SIZE_RATE;
		return intersection.size() >= backupSetSize || size < backupSetSize;
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

	private static boolean classify(double[] val) {
        svm_node[] node = new svm_node[n];
        for(int i=0; i<n; i++) {
        	node[i] = new svm_node();
        	node[i].index = i;
        	node[i].value = val[i];
        }
        return classify(node);
	}
	
	private static boolean classify(svm_node[] node) {
        if(svm.svm_predict(model, node) == 1.0)
        	return true;
        else return false;
	}
	

	// gamma = distance from classifier
    private static double computeGamma(ArrayList<Double> dist) {
		if(model == null) {
			// Default classifier...
	        double numer = 0.0, denom = 0.0;
	        for(int i=0; i<dist.size(); i++) {
	            numer += dist.get(i) * w[i];
	            denom += Math.pow(w[i], 2);
	        }
	        numer -= theta;
	        denom = Math.sqrt(denom);
	        return Math.abs(numer/denom);
		}
		double num = 0.0, den = 0.0;
		for(int i=0; i<n; i++) {
			num += w[i] * phi(dist.get(i));
			den += Math.pow(w[i], 2);
		}
		num -= theta;
		double pInvSum = 0.0;
		for(int i=0; i<n; i++)
			pInvSum += Math.pow(dist.get(i) - phiInverse( phi(dist.get(i)) - w[i] * num / den ), 2);
		return Math.sqrt(pInvSum);
    }
    
    private static double phi(double x) {
    	return Math.pow(x, DEGREE);
    }

    private static double phiInverse(double x) {
    	return Math.pow(x, 1.0 / DEGREE);
    }

    private static boolean askOracle(Couple c) {
    	counter++;
    	String ids = c.getSource().getID()+"#"+c.getTarget().getID();
		return askOracle(ids);
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
        parameter.C = 1E+4;
        parameter.svm_type = svm_parameter.C_SVC;
        parameter.kernel_type = KERNEL;
        if(KERNEL == svm_parameter.POLY) {
			parameter.degree = DEGREE; // default: 3
			parameter.coef0  = COEF0; // default: 0
			parameter.gamma  = 1; // default: 1/n
        } 
        parameter.eps = 1E-4;
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
