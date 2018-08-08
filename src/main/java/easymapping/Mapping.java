package easymapping;

import java.util.Collection;

public class Mapping {
	protected String first;
	protected String second;
	String fName;

	public Mapping(String first, String second, String fName) {
		super();
		this.first = first;
		this.second = second;
		this.fName = fName;
	}

	@Override
	public String toString() {
		return first + "=>" + second;
	}

	public boolean isTrivial() {
		return lset(first).equals(lset(second));
	}

	private String lset(String first2) {
		int lastIndexOf = first2.lastIndexOf('/');
		if (lastIndexOf != -1) {
			String lowerCase = first2.substring(lastIndexOf + 1).toLowerCase();
			StringBuilder bld = new StringBuilder();
			for (int i = 0; i < lowerCase.length(); i++) {
				if (lowerCase.charAt(i) == '-' || lowerCase.charAt(i) == '_' || lowerCase.charAt(i) == ' '
						|| lowerCase.charAt(i) == '@') {
					continue;
				}
				bld.append(lowerCase.charAt(i));
			}
			return bld.toString();
		}
		return first2;
	}

	public Collection<String> firstWords() {
		return WordUtils.wordsOfLastSegment(first);
	}

	public Collection<String> secondWords() {
		return WordUtils.wordsOfLastSegment(second);
	}

}