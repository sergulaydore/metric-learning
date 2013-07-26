package acids2.multisim;

import java.util.ArrayList;
import java.util.Collections;

import libsvm.svm_parameter;
import utility.GammaComparator;
import acids2.Couple;
import acids2.Resource;
import acids2.TestUnit;
import acids2.classifiers.svm.SvmHandler;

public class MultiSimSetting extends TestUnit {
	
	private ArrayList<Resource> sources, targets;
	private int K;
	private String datasetPath;
	
	private SvmHandler m;
	private MultiSimMeasures measures;
	private MultiSimOracle oracle;
	private MultiSimEvaluation eval;
	private MultiSimTrainer trainer;
	
	private int dk = 5;
	private int mapSize;
	
	public MultiSimSetting(ArrayList<Resource> sources,
			ArrayList<Resource> targets, MultiSimOracle oracle, int K, String datasetPath) {
		
		this.sources = sources;
		this.targets = targets;
		this.oracle = oracle;
		this.K = K;
		this.datasetPath = datasetPath;
		this.mapSize = Math.min(sources.size(), targets.size());

		m = new SvmHandler(svm_parameter.LINEAR);
		measures = new MultiSimMeasures(this);
		eval = new MultiSimEvaluation(this);
		trainer = new MultiSimTrainer(this);
	}
	
	public void run() {

		ArrayList<MultiSimSimilarity> sims = measures.getAllSimilarities();
		ArrayList<Couple> couples = new ArrayList<Couple>();
		
		ArrayList<Couple> labelled = new ArrayList<Couple>();

		for(int i=0; labelled.size() < K; i++) {
			
			System.out.println("\n### Iteration = "+i+" ###");
			
			// TODO The following works only with linear classifiers. Implement one for polynomial classifiers.
			couples = filtering(sims);
			if(couples == null)
				return;
			computeRemaining(sims, couples);
			for(Couple c : couples) 
				measures.estimateMissingValues(c);
			
//			if(i > 0) {
//				ArrayList<Couple> temp = new ArrayList<Couple>();
//				for(MultiSimSimilarity sim : sims) {
//					ArrayList<Couple> input = new ArrayList<Couple>(temp);
//					double thr = m.computeMonteCarlo(sim);
//					if(sim.isComputed() && sim.getFilter() != null) {
//						if(input.isEmpty())
//							temp = sim.getFilter().filter(sources, targets, sim.getProperty().getName(), thr);
//						else
//							temp = sim.getFilter().filter(input, sim.getProperty().getName(), thr);
//					}
//				}
//				while(temp.size() >= mapSize * 10)
//					temp.remove((int)(temp.size() * Math.random()));
//				if(!temp.isEmpty()) {
//					computeRemaining(sims, temp);
//					couples = temp;
//				}
//			}
			
			ArrayList<Couple> posInformative = new ArrayList<Couple>();
			ArrayList<Couple> negInformative = new ArrayList<Couple>();
			
			for(Couple c : couples) {
		        c.setGamma( m.computeGamma(c) ); // TODO Change to m.computeGamma(c);
				if(m.classify(c))
					posInformative.add(c);
				else
					negInformative.add(c);
			}
			
			System.out.println("theta = "+m.getTheta()+"\tpos = "+posInformative.size()+"\tneg = "+negInformative.size());
			
			Collections.sort(posInformative, new GammaComparator());
			Collections.sort(negInformative, new GammaComparator());
			
			ArrayList<Couple> poslbl = new ArrayList<Couple>();
			ArrayList<Couple> neglbl = new ArrayList<Couple>();
			
			for(Couple c : labelled)
				if(c.isPositive())
					poslbl.add(c);
				else
					neglbl.add(c);
			
			if(i == 0) {
				// required for right classifier orientation
//				orientate(poslbl, neglbl, labelled);
				// train with dk most likely positive examples
				trainer.train(posInformative, negInformative, labelled, poslbl, neglbl, this.dk, false);
			}
			
			// train with dk most informative positive examples
			trainer.train(posInformative, negInformative, labelled, poslbl, neglbl, this.dk, true);
			// train with dk most informative negative examples
			trainer.train(negInformative, posInformative, labelled, poslbl, neglbl, this.dk, true);
			
//			// train with dk random examples
//			for(int j=0; j<this.dk; j++) {
//				Resource s = sources.get((int) (sources.size() * Math.random()));
//				Resource t = targets.get((int) (targets.size() * Math.random()));
//				Couple c = new Couple(s, t);
//				for(Property p : props) {
//					double d = p.getFilter().getDistance(s.getPropertyValue(p.getName()), t.getPropertyValue(p.getName()));
//					c.setDistance(d, p.getIndex());
//				}
//				labelled.add(c);
//				if(askOracle(c)) {
//					c.setPositive(true);
//					poslbl.add(c);
//				} else {
//					c.setPositive(false);
//					neglbl.add(c);
//				}
//			}
			
			System.out.println("Labeled pos: "+poslbl.size());
			System.out.println("Labeled neg: "+neglbl.size());
			
			boolean svmResponse = m.trace(poslbl, neglbl);
			if(!svmResponse) {
				// ask for more
				continue;
			}
			eval.evaluateOn(labelled);

			// last iteration
//			if(labelled.size() >= K) {
//				System.out.println("Converging...");
//				for(int j=0; m.getTheta() == -1.0 && j < 10; j++) {
//					trainer.train(posInformative, negInformative, labelled, poslbl, neglbl, 1, true);
//					m.trace(poslbl, neglbl);
//				}
//			}
		}
		
		System.out.println("Evaluating on filtered subset:");
		eval.labelAll(couples);
		eval.evaluateOn(couples);
		
		
		eval.fastEvaluation(sources, targets);

	}
	
	private ArrayList<Couple> filtering(ArrayList<MultiSimSimilarity> sims) {
		
		ArrayList<Couple> couples = new ArrayList<Couple>();
		ArrayList<MultiSimRanking> ranking = new ArrayList<MultiSimRanking>();
		
		double[] w = m.getWLinear();
		double[] means = measures.getMeans();
		for(int i=0; i<sims.size(); i++)
			if(sims.get(i).getFilter() != null)
				ranking.add(new MultiSimRanking(sims.get(i), w[i] * means[i]));
		if(ranking.isEmpty()) {
			System.err.println("No filter available.");
			return null;
		}
		Collections.sort(ranking);
		System.out.println(ranking);
		
		MultiSimSimilarity simPivot = ranking.get(0).getSim();
		MultiSimProperty p = simPivot.getProperty();
		System.out.println("Processing similarity "+simPivot.getName()+" of "+p.getName());
		simPivot.setComputed(true);
		double def = measures.computeThreshold(simPivot);
		if(def == 0.0)
			def = simPivot.getEstimatedThreshold();
		couples = simPivot.getFilter().filter(sources, targets, p.getName(), def);
		System.out.println("thr = "+def+"\tsize = "+couples.size());
		return couples;
	}

	private void computeRemaining(ArrayList<MultiSimSimilarity> sims, ArrayList<Couple> couples) {
		System.out.print("Computing similarities");
		for(MultiSimSimilarity sim : sims) {
			MultiSimProperty p = sim.getProperty();
			if(!sim.isComputed()) {
				for(Couple c : couples) {
					Resource s = c.getSource();
					Resource t = c.getTarget();
					double d = sim.getSimilarity(s.getPropertyValue(p.getName()), t.getPropertyValue(p.getName()));
					c.setDistance(d, sim.getIndex());
				}
			}
			System.out.print(".");
		}
		System.out.println();
	}

	public MultiSimMeasures getMeasures() {
		return measures;
	}

	public MultiSimOracle getOracle() {
		return oracle;
	}

	public ArrayList<Resource> getSources() {
		return sources;
	}

	public ArrayList<Resource> getTargets() {
		return targets;
	}

	public int getK() {
		return K;
	}

	public String getDatasetPath() {
		return datasetPath;
	}

	public SvmHandler getSvmHandler() {
		return m;
	}

	public int getMapSize() {
		return mapSize;
	}
	
	
}
