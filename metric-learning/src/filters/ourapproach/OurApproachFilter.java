package filters.ourapproach;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Vector;

import test.Test;

import metriclearning.Couple;
import metriclearning.Resource;
import filters.StandardFilter;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class OurApproachFilter extends StandardFilter {
	
	private static HashMap<Character, Double> f_insdel = new HashMap<Character, Double>();

	private static HashMap<Character, Double> f_sub = new HashMap<Character, Double>();

	public static TreeSet<Couple> ourApproachFilter(ArrayList<Resource> sources,
			ArrayList<Resource> targets, String propertyName, double θ) {
		
		loadCaseWeights();
		loadConfusionMatrix();
		
		updatef();
		
		TreeSet<Couple> results = new TreeSet<Couple>();

		double mw = getMinWeight();
		double tau = θ / mw;
		
		long start = System.currentTimeMillis();

		for(Resource s : sources) {
			String sp = s.getPropertyValue(propertyName);
			for(Resource t : targets) {
				String tp = t.getPropertyValue(propertyName);
				if(Math.abs(sp.length() - tp.length()) <= tau) {
					Vector<Character> cs = new Vector<Character>();
					for(int i=0; i<sp.length(); i++)
						cs.add(sp.charAt(i));
					Vector<Character> ct = new Vector<Character>();
					for(int i=0; i<tp.length(); i++)
						ct.add(tp.charAt(i));
					Vector<Character> c = subtract(cs, ct);
					c.addAll(subtract(ct, cs));
//					Collections.sort(c);
//					double minSub = 1.0, minInsdel = 1.0;
//					for(char ch : c) {
//						double w1 = f_sub.get(ch);
//						if(w1 < minSub)
//							minSub = w1;
//						double w2 = f_insdel.get(ch);
//						if(w2 < minInsdel)
//							minInsdel = w2;
//					}
					
//					String exclDisj = xor(sp, tp);
					
//					double minSub = 1.0;//, minInsdel = 1.0;
//					for(int i=0; i<exclDisj.length(); i++) {
//						char ch = exclDisj.charAt(i);
//						double w1 = f_sub.get(ch);
//						if(w1 < minSub)
//							minSub = w1;
////						double w2 = f_insdel.get(ch);
////						if(w2 < minInsdel)
////							minInsdel = w2;
//					}
					double minGap = (int)(c.size() / 2);// + (size % 2) * minInsdel;
					if(minGap <= tau) {
						/*
						 *  Verification.
						 */
						double d = wed.proximity(sp, tp);
						if(d <= θ) {
							Couple cpl = new Couple(s, t);
							cpl.addDistance(d);
							results.add(cpl);
						}
					}
				}
			}
		}
		double compTime = (double)(System.currentTimeMillis()-start)/1000.0;
		System.out.print(compTime+"\t");
		Test.append(compTime+"\t");
		
		return results;
	}
	
	
	private static String print(ArrayList<Character> c) {
		String s = "(";
		for(char ch : c)
			s += ch + ",";
		return s+")";
	}

	private static String xor(String a, String b) {

		String r = "";
		for(int i=0; i<a.length(); i++) {
			char c = a.charAt(i);
			boolean found = false;
			for(int j=0; j<b.length(); j++) {
				if(c == b.charAt(j)) {
					b = b.substring(0, j) + b.substring(j+1, b.length());
					found = true;
					break;
				}
			}
			if(!found)
				r = r + c;
		}
		
		return r+b;
	}

	private static Vector<Character> subtract(Vector<Character> cs,
			Vector<Character> ct) {
		Vector<Character> res = new Vector<Character>(cs);
		for(Character c1 : ct)
			res.remove(c1);
		return res;
	}


	private static void updatef() {
		for(char c='0'; c<='9'; c++) {
			f_sub.put(c, getMinimalSubCostOf(c));
			f_insdel.put(c, getMinimalInsdelCostOf(c));
		}
		for(char c='A'; c<='Z'; c++) {
			f_sub.put(c, getMinimalSubCostOf(c));
			f_insdel.put(c, getMinimalInsdelCostOf(c));
		}
		for(char c='a'; c<='z'; c++) {
			f_sub.put(c, getMinimalSubCostOf(c));
			f_insdel.put(c, getMinimalInsdelCostOf(c));
		}
		f_sub.put(' ', getMinimalSubCostOf(' '));
		f_insdel.put(' ', getMinimalInsdelCostOf(' '));
	}

	private static double getMinimalSubCostOf(char c) {
		double min = 1.0;
		for(String key : weights.keySet()) {
			if(key.contains(c+"") && !key.contains("ε")) {
				double w = weights.get(key);
				if(w < min) 
					min = w;
			}
		}
		return min;
	}

	private static double getMinimalInsdelCostOf(char c) {
		Double ins = weights.get("ε,"+c);
		Double del = weights.get(c+",ε");
		if(ins != null) {
			if(del != null)
				return Math.min(ins, del);
			else
				return ins;
		} else {
			if(del != null)
				return del;
			else
				return 1.0;
		}
	}
	
}
