package easymapping;

import java.io.Serializable;
import java.util.List;

public class MappingPath implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public MappingPath(String first, List<String> collection) {
		this.path=first;
		this.words=collection;
	}

	protected String path;
	
	
	public String getParentPath() {
		String path = this.toString();
		int lastIndexOf = path.lastIndexOf('/');
		if (lastIndexOf!=-1) {
			path=path.substring(0, lastIndexOf);
		}
		return path;
	}
	
	public List<String>words;
	private String[] splits;
	
	@Override
	public String toString() {
		return path;
	}
	
	public String nm() {
		StringBuilder b=new StringBuilder();
		for (String s:words) {
			String lowerCase = s.toLowerCase();
			
			b.append(lowerCase);
		}
		return b.toString();
	}

	public String lastSegment() {
		int lastIndexOf = this.path.lastIndexOf('/');
		if (lastIndexOf!=-1) {
			String substring = this.path.substring(lastIndexOf+1);
			return substring;
		}
		return this.path;
	}

	public String[] segments() {
		if (splits!=null) {
			return splits;
		}
		splits = this.path.split("/");
		return splits;
	}
}
