package acids2.multisim;

public abstract class MultiSimSimilarity {

	protected MultiSimProperty property;
	
	protected int index;
	
	public MultiSimSimilarity(MultiSimProperty property, int index) {
		this.property = property;
		this.index = index;
	}

	public MultiSimProperty getProperty() {
		return property;
	}
	
	public int getIndex() {
		return index;
	}
	
	public abstract MultiSimFilter getFilter();

	public abstract String getName();
	
	public abstract int getDatatype();
	
	public abstract double getSimilarity(String a, String b);

}
