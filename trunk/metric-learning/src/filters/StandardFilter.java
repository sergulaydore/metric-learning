package filters;

import java.util.HashMap;

import similarities.WeightedEditDistanceExtended;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class StandardFilter {
	
	protected static final double INIT_FULL_WEIGHT = 1.0;
	protected static final double INIT_CASE_WEIGHT = 0.5;

	protected static HashMap<String, Double> weights = new HashMap<String, Double>();
	
	protected static WeightedEditDistanceExtended wed = new WeightedEditDistanceExtended() {
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
		weights.clear();
		for(char c='A'; c<='Z'; c++) {
			weights.put(c+","+(char)(c+32), INIT_CASE_WEIGHT);
			weights.put((char)(c+32)+","+c, INIT_CASE_WEIGHT);
		}
	}
			
	protected static double getMinWeight() {
		double min = Double.MAX_VALUE;
		for(Double d : weights.values())
			if(d < min)
				min = d;
		return min;
	}
	

}
