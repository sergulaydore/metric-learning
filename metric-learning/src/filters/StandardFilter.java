package filters;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

import similarities.WeightedEditDistanceExtended;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class StandardFilter {
	
	protected static final double INIT_FULL_WEIGHT = 1.0;
	protected static final double INIT_CASE_WEIGHT = 0.5;

	protected static HashMap<String, Double> weights = new HashMap<String, Double>();
	
	public static WeightedEditDistanceExtended wed = new WeightedEditDistanceExtended() {
		@Override
		public double transposeWeight(char cFirst, char cSecond) {
			return Double.POSITIVE_INFINITY;
		}
		@Override
		public double substituteWeight(char cDeleted, char cInserted) {
			Double d = weights.get(cDeleted+","+cInserted);
			if(d == null)
				return INIT_FULL_WEIGHT;
			else
				return d;
		}
		@Override
		public double matchWeight(char cMatched) {
			return 0.0;
		}
		@Override
		public double insertWeight(char cInserted) {
			Double d = weights.get("ε,"+cInserted);
			if(d == null)
				return INIT_FULL_WEIGHT;
			else
				return d;
		}
		@Override
		public double deleteWeight(char cDeleted) {
			Double d = weights.get(cDeleted+",ε");
			if(d == null)
				return INIT_FULL_WEIGHT;
			else
				return d;
		}
	};
	
	protected static void loadCaseWeights() {
		for(char c='A'; c<='Z'; c++) {
			weights.put(c+","+(char)(c+32), INIT_CASE_WEIGHT);
			weights.put((char)(c+32)+","+c, INIT_CASE_WEIGHT);
		}
	}

	protected static void loadConfusionMatrix() {
		Scanner in = null;
		try {
			in = new Scanner(new File("data/ConfusionMatrix.txt"));
		} catch (FileNotFoundException e) {
			System.err.println("Missing file `./data/ConfusionMatrix.txt`!");
			return;
		}
		for(char c1='a'; c1<='{'; c1++) {
			for(char c2='a'; c2<='{'; c2++) {
				double d = in.nextDouble();
				if(d != 1) {
					if(c1 == '{') {
						weights.put("ε,"+c2, d);
						weights.put("ε,"+(char)(c2-32), d);
					} else 	if(c2 == '{') {
						weights.put(c1+",ε", d);
						weights.put((char)(c1-32)+",ε", d);
					} else {
						weights.put(c1+","+c2, d);
						weights.put((char)(c1-32)+","+(char)(c2-32), d);
						// crossing weights (e.g., <A,b>, <a,B>)
						double dcross = (1.0 + d) / 2;
						weights.put((char)(c1-32)+","+c2, dcross);
						weights.put(c1+","+(char)(c2-32), dcross);
					}
				}
			}
		}
		in.close();
	}
	
	protected static double getMinWeight() {
		double min = Double.MAX_VALUE;
		for(Double d : weights.values())
			if(d < min)
				min = d;
		return min;
	}
	

}
