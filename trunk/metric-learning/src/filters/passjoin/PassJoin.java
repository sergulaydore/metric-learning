package filters.passjoin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

import metriclearning.Couple;
import metriclearning.Resource;
import utility.OrderByLengthAndAlpha;
import utility.WeightedEditDistanceExtended;


/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class PassJoin {

	static WeightedEditDistanceExtended wed = new WeightedEditDistanceExtended() {
		@Override
		public double transposeWeight(char cFirst, char cSecond) {
			return Double.POSITIVE_INFINITY;
		}
		@Override
		public double substituteWeight(char cDeleted, char cInserted) {
			return 1.0;
		}
		@Override
		public double matchWeight(char cMatched) {
			return 0.0;
		}
		@Override
		public double insertWeight(char cInserted) {
			return 1.0;
		}
		@Override
		public double deleteWeight(char cDeleted) {
			return 1.0;
		}
	};
		
	
	public static TreeSet<Couple> passJoin(ArrayList<Resource> sources, ArrayList<Resource> targets, String propertyName, int tau_min, int tau) {
		
		TreeSet<Couple> results = new TreeSet<Couple>();
		
		Collections.sort(sources, new OrderByLengthAndAlpha(propertyName));
		Collections.sort(targets, new OrderByLengthAndAlpha(propertyName));
		
		InvertedIndex sourceIndex = buildInvertedIndex(sources, propertyName, tau);
		
		for(Resource res : targets) {
			String r = res.getPropertyValue(propertyName);
			int rl = r.length();
			for(int l = rl-tau; l <= rl+tau; l++) {
				for(int i=1; i<=tau+1; i++) {
					HashMap<String, TreeSet<Resource>> Lli = sourceIndex.getLli(l, i);
					int li = sourceIndex.getSegmentLength(l, i);
					if(li > 0) {
						TreeSet<String> wsl = substringSelection(r, li);
						for(String w : wsl) {
							if(Lli.keySet().contains(w)) {
								/*
								 *  Verification.
								 */
								TreeSet<Resource> list = Lli.get(w);
								for(Resource cand : list) {
									String s = cand.getPropertyValue(propertyName);
									String t = res.getPropertyValue(propertyName);
									double d = wed.proximity(s, t);
									if(d <= tau && d >= tau_min) {
										Couple c = new Couple(cand, res);
										c.addDistance(d);
										results.add(c);
									}
								}
							}
						}
					}
				}
			}
		}
		
		return results;
	}
	
	/**
	 * Builds the inverted index for the strings in the array list.
	 * @param resources
	 * @param tau
	 * @return
	 */
	private static InvertedIndex buildInvertedIndex(ArrayList<Resource> resources, String propertyName, int tau) {
		InvertedIndex invIndex = new InvertedIndex();
		for(Resource r : resources) {
			String s = r.getPropertyValue(propertyName);
			PartitionStrategy ps = getEvenPartitionStrategy(s, tau + 1);
			LinkedList<String> parts = ps.getPartitions();
			for(int i=1; i<=parts.size(); i++) {
				String part = parts.get(i-1);
				invIndex.addToIndex(part, r, s.length(), i);
			}
		}
		return invIndex;
	}

	/**
	 * Substring selection, length-base method.
	 * @param r
	 * @param li 
	 * @return
	 */
	private static TreeSet<String> substringSelection(String r, int li) {
		TreeSet<String> subset = new TreeSet<String>();
		for(int i=0; i<=r.length()-li; i++) {
			subset.add(r.substring(i, i+li));
		}
		return subset;
	}
	
	/**
	 * This function could be improved by merging the homonym nodes.
	 * ArrayList was chosen because of the need to sort the strings.
	 * 
	 * This method gets all the possible partition strategies for a string.
	 * @param s
	 * @param thr
	 * @return
	 */
	@SuppressWarnings("unused")
	private static ArrayList<PartitionStrategy> getAllStrategies(String s, int thr) {
		int len = s.length() / thr;
		int n_max = s.length() - len * thr;
		int n_min = thr - n_max;
		
		ArrayList<PartitionStrategy> partStr = new ArrayList<PartitionStrategy>();
		// d = depth of the permutation tree
		PartitionStrategy ps0 = new PartitionStrategy(s, n_min, n_max);
		partStr.add(ps0);
		for(int d=0; d<thr; d++) {
			LinkedList<PartitionStrategy> toAdd = new LinkedList<PartitionStrategy>();
			for(PartitionStrategy ps : partStr) {
				if(ps.getN_min() > 0 && ps.getN_max() > 0) { // split
					PartitionStrategy psB = ps.clone();
					int marker = psB.getMarker();
					psB.addPartition(s.substring(marker, marker + len + 1));
					psB.decreaseN_max();
					toAdd.add(psB);
					marker = ps.getMarker();
					ps.addPartition(s.substring(marker, marker + len));
					ps.decreaseN_min();
				} else {
					if(ps.getN_min() > 0) { // take the smaller partition
						int marker = ps.getMarker();
						ps.addPartition(s.substring(marker, marker + len));
						ps.decreaseN_min();
					} else { // take the bigger partition
						int marker = ps.getMarker();
						ps.addPartition(s.substring(marker, marker + len + 1));
						ps.decreaseN_max();
					}
				}
			}
			for(PartitionStrategy ps : toAdd)
				partStr.add(ps);
		}
		
		return partStr;
	}

	/*
	 * This method follows the so-called "even partition" policy, where the length of the
	 * first segments are always equal or shorter than the length of the last segments.
	 */
	private static PartitionStrategy getEvenPartitionStrategy(String s, int thr) {
		int len = s.length() / thr;
		int n_max = s.length() - len * thr;
		int n_min = thr - n_max;
		
		int marker = 0;
		PartitionStrategy ps = new PartitionStrategy(s, n_min, n_max);
		for(int i=0; i<thr; i++) {
			if(i < n_min) {
				ps.addPartition(s.substring(marker, marker + len));
				marker = marker + len;
			} else {
				ps.addPartition(s.substring(marker, marker + len + 1));
				marker = marker + len + 1;
			}
		}
		return ps;
	}


}
