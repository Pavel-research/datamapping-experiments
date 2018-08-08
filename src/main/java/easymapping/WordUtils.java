package easymapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.mulesoft.nn.data.WordSplitter;
import com.mulesoft.omodel.compact.TermsMap;
import com.mulesoft.omodel.compact.TermsMap.Term;

public class WordUtils {

	public static boolean isSimpleAbbr(String testedName, String toTest) {
		int lastIndex = 0;
		toTest = toTest.toLowerCase();
		for (int i = 0; i < testedName.length(); i++) {
			char c = testedName.charAt(i);
			int indexOf = toTest.indexOf(c, lastIndex);
			if (indexOf < 0) {
				return false;
			} else {
				lastIndex = indexOf + 1;
			}

		}
		return true;
	}
	public static String lastSegment(String first2) {
		int lastIndexOf = first2.lastIndexOf('/');
		if (lastIndexOf!=-1) {
			return first2.substring(lastIndexOf+1);
		}
		return first2;
	}
	public static Collection<String> wordsOfLastSegment(String first2) {
		int lastIndexOf = first2.lastIndexOf('/');
		if (lastIndexOf != -1) {
			String lowerCase = first2.substring(lastIndexOf + 1);
			return new WordSplitter().apply(lowerCase);
		}
		return new ArrayList<>(Collections.singletonList(first2));
	}

	public static boolean isAbbr(String nm, String nm2) {
		if (nm2.length() > 0 && nm.length() > 0) {
			return isSimpleAbbr(nm, nm2) || isSimpleAbbr(nm2, nm);
		}
		return false;
	}
	
	public static boolean startWithIgnoringPlurality(String i1, String i2) {
		return i1.startsWith(i2.toLowerCase()) || i2.startsWith(InflectorBase.singularize(i2.toLowerCase()))
				|| i1.startsWith(InflectorBase.singularize(i2.toLowerCase()));
	}
	public static boolean equalsIgnoringPlurality(String ee, String eq) {
		return ee.equals(eq) || ee.equals(InflectorBase.singularize(eq))
				|| InflectorBase.pluralize(ee).equals(eq);
	}
	
	public static boolean hasCommonParts(String i1, String i2) {
		return startWithIgnoringPlurality(i2, i1)||startWithIgnoringPlurality(i1, i2);
	}
	
	public static String basicCleanName(String lastSegment) {
		if (lastSegment.endsWith("__c")||lastSegment.endsWith("__r")) {
			lastSegment=lastSegment.substring(0,lastSegment.lastIndexOf('_'));
		}
		String removeUnderscoreAndDash = removeUnderscoreAndDash(lastSegment);
		
		if (removeUnderscoreAndDash.toLowerCase().endsWith("number")&&removeUnderscoreAndDash.length()>6) {
			removeUnderscoreAndDash=removeUnderscoreAndDash.substring(0,removeUnderscoreAndDash.length()-3);
		}
		if (removeUnderscoreAndDash.endsWith("No")&&removeUnderscoreAndDash.length()>3) {
			removeUnderscoreAndDash=removeUnderscoreAndDash.substring(0,removeUnderscoreAndDash.length()-2)+"Num";
		}
		
		return removeUnderscoreAndDash;
	}
	public static String removeUnderscoreAndDash(String lastSegment) {
		StringBuilder bld = new StringBuilder();
		if (lastSegment.startsWith("u_")||lastSegment.startsWith("m_")) {
			lastSegment=lastSegment.substring(2);
		}
		for (int i = 0; i < lastSegment.length(); i++) {
			char charAt = lastSegment.charAt(i);
			if (charAt == '_' || charAt == '-'|| charAt == ' '|| charAt == '.') {
				continue;
			}
			bld.append(charAt);
		}
		return bld.toString();
	}
	
	static HashSet<String>allowed=new HashSet<>();
	
	static{
		allowed.add("zip");
		allowed.add("adr");
		allowed.add("num");
		allowed.add("bio");
	}
	
	public static double scoreTerm(List<String> words, List<String> words2) {
		double max=0;
		for (String q : words) {
			q=q.toLowerCase();
			for (String q1 : words) {
				q1=q1.toLowerCase();
				if (isAllowed(q)&&isAllowed(q1)) {
					double rel = rel(q, q1);
					double rel1 = rel(q1, q);
					double mx=Math.max(rel, rel1);
					if (mx>max) {
						max=mx;
					}
				}
			}
		}
		return max;
	}

	public static boolean isAllowed(String q) {
		return q.length() > 3||allowed.contains(q);
	}

	public static double rel(String q, String q1) {
		double rel=0;
		Term term = TermsMap.getInstance().get(q);
		if (term!=null) {
			Integer integer = term.get(q1);
			if (integer!=null) {
				if (integer*200>term.count) {
					rel=integer/(double)term.count;
				}
			}
		}
		return rel;
	}
}
