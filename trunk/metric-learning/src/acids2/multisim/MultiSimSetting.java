package acids2.multisim;

import java.util.ArrayList;

import acids2.Couple;
import acids2.Resource;
import acids2.TestUnit;
import acids2.classifiers.svm.SvmHandler;

public class MultiSimSetting extends TestUnit {
	
	private ArrayList<Resource> sources, targets;
	private int K;
	private String datasetPath;
	
	private SvmHandler m;
	private MultiSimMeasures ms;
	
	public MultiSimSetting(ArrayList<Resource> sources,
			ArrayList<Resource> targets, int K, String datasetPath) {
		
		this.sources = sources;
		this.targets = targets;
		this.K = K;
		this.datasetPath = datasetPath;
		
		m = new SvmHandler();
		ms = new MultiSimMeasures(this, m);
		
		ArrayList<MultiSimSimilarity> sims = ms.getAllSimilarities(); 
//		int n = sims.size();
		
//		Resource s = sources.get((int) (sources.size() * Math.random()));
//		Resource t = targets.get((int) (targets.size() * Math.random()));
//		Couple c = new Couple(s, t);
//		
//		for(MultiSimSimilarity sim : sims) {
//			double d = sim.getSimilarity(c.getSource().getPropertyValue(sim.getProperty().getName()),
//					c.getTarget().getPropertyValue(sim.getProperty().getName()));
//			c.setDistance(d, sim.getIndex());
//			System.out.println(c.getSource().getPropertyValue(sim.getProperty().getName()) + "\t" + 
//					c.getTarget().getPropertyValue(sim.getProperty().getName()) + "\t" + d);
//		}
		
		ArrayList<Couple> couples = new ArrayList<Couple>();
		int mapSize = Math.min(sources.size(), targets.size());
		
		for(int i=0; i<sims.size(); i++) {
			MultiSimSimilarity sim = sims.get(i);
			MultiSimProperty p = sim.getProperty();
			MultiSimFilter filter = sim.getFilter();
			System.out.println("Processing similarity "+sim.getName()+" of "+p.getName());
			
			if(filter != null)
				for(double threshold=0.9; ; threshold-=0.1) {
					System.out.println("threshold = "+threshold);
					// FIXME find a clever filtering management
					if(couples.isEmpty())
						couples = filter.filter(sources, targets, p.getName(), threshold);
					else
						couples = filter.filter(couples, p.getName(), threshold);
					System.out.println("size = "+couples.size());
					if(couples.size() >= mapSize) {
						p.setFiltered(true);
						break;
					}
				}
			if(couples.size() <= mapSize * 10)
				break;
		}
		
		for(MultiSimSimilarity sim : sims) {
			MultiSimProperty p = sim.getProperty();
			if(!p.isFiltered()) { // FIXME should be sim.isComputed()
				for(Couple c : couples) {
					Resource s = c.getSource();
					Resource t = c.getTarget();
					double d = sim.getSimilarity(s.getPropertyValue(p.getName()), t.getPropertyValue(p.getName()));
					c.setDistance(d, sim.getIndex());
				}
			}
		}

		for(Couple c : couples)
			for(MultiSimSimilarity sim : sims) {
				double d = sim.getSimilarity(c.getSource().getPropertyValue(sim.getProperty().getName()),
						c.getTarget().getPropertyValue(sim.getProperty().getName()));
				c.setDistance(d, sim.getIndex());
				System.out.println(c.getSource().getPropertyValue(sim.getProperty().getName()) + "\t" + 
						c.getTarget().getPropertyValue(sim.getProperty().getName()) + "\t" + d);
			}

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

	public SvmHandler getM() {
		return m;
	}
	
	
}
