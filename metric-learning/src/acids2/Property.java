package acids2;

import filters.RandomFilter;
import filters.StandardFilter;
import filters.mahalanobis.MahalaFilter;
import filters.reeded.ReededFilter;

public class Property {
	
	public static final int TYPE_STRING = 0;
	public static final int TYPE_NUMERIC = 1;
	public static final int TYPE_DATETIME = 2;

	private String name;
	private int datatype;
	private StandardFilter filter;

	public Property(String name, int datatype) {
		super();
		this.name = name;
		this.datatype = datatype;
		switch(datatype) {
		case TYPE_STRING:
			this.filter = new ReededFilter();
			break;
		case TYPE_NUMERIC:
			this.filter = new MahalaFilter();
			break;
		case TYPE_DATETIME: // TODO datetime similarity and filtering?
			this.filter = new MahalaFilter();
			break;
		default: // string comparison always works.
			this.filter = new ReededFilter();
			break;
		}
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getDatatype() {
		return datatype;
	}
	public void setDatatype(int datatype) {
		this.datatype = datatype;
	}
	public StandardFilter getFilter() {
		return filter;
	}
	public void setFilter(StandardFilter filter) {
		this.filter = filter;
	}
	
}
