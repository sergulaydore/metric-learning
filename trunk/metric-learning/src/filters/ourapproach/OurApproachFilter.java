package filters.ourapproach;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Vector;

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
		
		weights.put("i,ε", 0.5);
		weights.put("r,ε", 0.5);
		weights.put("s,ε", 0.5);
		weights.put("t,ε", 0.5);
		
		updatef();
		
		TreeSet<Couple> results = new TreeSet<Couple>();

		int tau = (int) (θ / getMinWeight());
//		System.out.println("tau = "+tau);
		
		int count = 0;
		for(Resource s : sources) {
			String sp = s.getPropertyValue(propertyName);
			for(Resource t : targets) {
				String tp = t.getPropertyValue(propertyName);
//				System.out.println("<"+sp+", "+tp+"> "+(Math.abs(sp.length() - tp.length()) <= tau));
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
//					System.out.println("  -> C is "+print(c));
					double minSub = 1.0, minInsdel = 1.0;
					for(char ch : c) {
						double w1 = f_sub.get(ch);
						if(w1 < minSub)
							minSub = w1;
						double w2 = f_insdel.get(ch);
						if(w2 < minInsdel)
							minInsdel = w2;
					}
					int size = c.size();
					double minGap = (int)(size / 2) * minSub + (size % 2) * minInsdel;
//					System.out.println("  -> minGap = "+minGap+" ("+(minGap <= θ)+")");
					if(minGap <= θ) {
						/*
						 *  Verification.
						 */
						String src = s.getPropertyValue(propertyName);
						String tgt = t.getPropertyValue(propertyName);
						double d = wed.proximity(src, tgt);
						count++;
//						System.out.println("     -> d = "+d+" ("+(d <= θ)+")");
						if(d <= θ) {
							Couple cpl = new Couple(s, t);
							cpl.addDistance(d);
							results.add(cpl);
						}
					}
				}
			}
		}
		System.out.println("count = "+count);
		return results;
	}
	
	
	private static String print(ArrayList<Character> c) {
		String s = "(";
		for(char ch : c)
			s += ch + ",";
		return s+")";
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
