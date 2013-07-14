package acids2.multisim;

import uk.ac.shef.wit.simmetrics.similaritymetrics.CosineSimilarity;

public class MultiSimCosineSimilarity extends MultiSimStringSimilarity {

	
	public MultiSimCosineSimilarity(MultiSimProperty property, int index) {
		super(property, index);
	}

	@Override
	public String getName() {
		return "Cosine Similarity";
	}

	@Override
	public int getDatatype() {
		return MultiSimDatatype.TYPE_STRING;
	}

	@Override
	public MultiSimProperty getProperty() {
		return property;
	}

	@Override
	public double getSimilarity(String a, String b) {
		CosineSimilarity cs = new CosineSimilarity();
		return cs.getSimilarity(a, b);
	}

	@Override
	public MultiSimFilter getFilter() {
		// TODO Auto-generated method stub
		return null;
	}

}
