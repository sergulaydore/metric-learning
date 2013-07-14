package acids2.multisim;

import java.util.ArrayList;

import acids2.Resource;
import acids2.classifiers.svm.SvmHandler;

public class MultiSimMeasures {
	
	private ArrayList<MultiSimProperty> props = new ArrayList<MultiSimProperty>();
	
	private SvmHandler svmHandler;
	private MultiSimSetting setting;
	
	public MultiSimMeasures(MultiSimSetting setting, SvmHandler svmHandler) {
		
		super();
		this.setting = setting;
		this.svmHandler = svmHandler;
		
		initialize();
		
	}
	
	public ArrayList<MultiSimSimilarity> getAllSimilarities() {
		ArrayList<MultiSimSimilarity> sims = new ArrayList<MultiSimSimilarity>();
		for(MultiSimProperty p : props)
			sims.addAll(p.getSimilarities());
		return sims;
	}
	
	public int computeN() {
		int dim = 0;
		for(MultiSimProperty p : props)
			dim += p.getSize();
		return dim;
	}
	
	
	/**
	 * Initializes the properties checking their data types. Eventually calls weights and extrema computation.
	 */
	private void initialize() {
		ArrayList<Resource> sources = setting.getSources();
		ArrayList<Resource> targets = setting.getTargets();
		
		ArrayList<String> propertyNames;		
		try {
			propertyNames = sources.get(0).getPropertyNames();
		} catch (Exception e) {
			System.err.println("Source set is empty!");
			return;
		}
		
		int index = 0;
		for(String pn : propertyNames) {
			int type = MultiSimDatatype.TYPE_NUMERIC;
			for(Resource s : sources) {
				if(s.checkDatatype(pn) == MultiSimDatatype.TYPE_STRING) {
					type = MultiSimDatatype.TYPE_STRING;
					break;
				}
			}
			if(type == MultiSimDatatype.TYPE_NUMERIC) {
				for(Resource t : targets) {
					if(t.checkDatatype(pn) == MultiSimDatatype.TYPE_STRING) {
						type = MultiSimDatatype.TYPE_STRING;
						break;
					}
				}
			}
			MultiSimProperty p = new MultiSimProperty(pn, type, index, this);
			props.add(p);
			index += p.getSize();
		}
		
		for(MultiSimProperty p : props) {
			for(MultiSimSimilarity sim : p.getSimilarities()) {
				System.out.println(p.getName() + "\t" + sim.getName() + "\t" + MultiSimDatatype.asString(sim.getDatatype()));
			}
		}
		
		svmHandler.setN(propertyNames.size());
		svmHandler.initW();
	}

	public ArrayList<MultiSimProperty> getProps() {
		return props;
	}

	public SvmHandler getSvmHandler() {
		return svmHandler;
	}

	public MultiSimSetting getSetting() {
		return setting;
	}

	
}
