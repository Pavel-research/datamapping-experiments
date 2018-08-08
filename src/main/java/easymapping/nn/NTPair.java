package easymapping.nn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.mulesoft.nn.annotions.Ignore;
import com.mulesoft.nn.annotions.In;
import com.mulesoft.nn.annotions.OneHot;
import com.mulesoft.nn.annotions.Out;
import com.mulesoft.nn.workspace.impl.NDocVector;
import com.mulesoft.nn.workspace.impl.Word2VecManager;

import easymapping.BasicSolver;
import easymapping.MappingPath;
import easymapping.WordUtils;

public class NTPair implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	@Ignore
	MappingPath p1;
	
	@Ignore
	MappingPath p2;

	
	



	

	


	public NTPair(MappingPath p1, MappingPath p2, boolean out, int i2, int j, double parentPathRate) {
		this.parentPathRate = parentPathRate;
		this.p1=p1;
		this.p2=p2;
		this.isAbbr = BasicSolver.isPotentialAbbr(p1, p2);
		String p1LastSegment = p1.lastSegment().toLowerCase();
		String p2LastSegment = p2.lastSegment().toLowerCase();
		this.score2=BasicSolver.score(p1, p2);
		int li=0;
		int cc=0;
		
		//this.propertyName=p1.lastSegment();
		//this.propertyName1=p2.lastSegment();
		String lastSegment = p1.lastSegment();
		String lastSegment2 = p2.lastSegment();
		if (lastSegment.toUpperCase().equals(lastSegment)) {
			this.allCaps1=true;
		}
		if (lastSegment2.toUpperCase().equals(lastSegment2)) {
			this.allCaps2=true;
		}
		for (int i=0;i<lastSegment.length();i++) {
			char c=p1LastSegment.charAt(i);
			if (Character.isUpperCase(c)) {
				
				int indexOf = lastSegment2.indexOf(c,li);
				if (indexOf!=-1) {
					li=indexOf;
					cc++;
				}
			}
		}
		if (cc>0) {
			this.capIntersection=cc/(double)(p1LastSegment.length()+p2LastSegment.length());
		}
		var ls=new LinkedHashSet<String>();
		for (String s:p1.words) {
			ls.add(s.toLowerCase());
		}
		int ci=0;
		for (String s:p2.words) {
			if (ls.contains(s.toLowerCase())) {
				ci++;
			}
		}
		this.commonWordsCount=ci/10.0;
//		LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>(lc(p1.words_);
//		linkedHashSet.removeAll(p1.words);
//		for (String w:new ArrayList<>(p1).words) {
//			for (String w1:p2.words) {
//				Word2VecManager.distance(w, w1);
//			}
//		}
		
//		for (String m:p1.words) {
//			m=m.toLowerCase();
//			if (NT2.oftenWords.contains(m)) {
//				w1.add(m);
//			}
//		}
//		for (String m:p2.words) {
//			m=m.toLowerCase();
//			if (NT2.oftenWords.contains(m)) {
//				w2.add(m);
//			}
//		}
		if (p1LastSegment.length() > 0 && p2LastSegment.length() > 0) {
			char charAt = p1LastSegment.charAt(p1.lastSegment().length() - 1);
			if (Character.isDigit(charAt)) {
				if (charAt==p2LastSegment.charAt(p2.lastSegment().length()-1)) {
					this.sameNumber=true;
				}
			}
		}
		if (p1LastSegment.length() > 3) {
			String mm = p1LastSegment.substring(0, 3);
			if (p2LastSegment.startsWith(mm)) {
				this.stWith=true;
			}
		}
		if (p2LastSegment.length() > 3) {
			String mm = p2LastSegment.substring(0, 3);
			if (p1LastSegment.startsWith(mm)) {
				this.stWith1=true;
			}
		}
		if (p2.getParentPath().toString().toLowerCase().contains(p1LastSegment)) {
			pathIntersection1=true;
		}
		if (p1.getParentPath().toString().toLowerCase().contains(p2LastSegment)) {
			pathIntersection2=true;
		}
		int c = 0;
		for (int i = 0; i < p1LastSegment.length(); i++) {
			if (p2LastSegment.indexOf(p1LastSegment.charAt(i)) != -1) {
				c++;
			}
		}
		if (c > 0) {
			distance = c * c / ((double) p1LastSegment.length() * p2LastSegment.length());
		} else {
			distance = 0.0;
		}
		int lcsLength = BasicSolver.getLongestCommonSubstring(p1LastSegment, p2LastSegment);
		if (lcsLength > 0) {
			longestString = lcsLength / ((double) p1LastSegment.length() * p2LastSegment.length());
			this.lcsLength = Math.min(1.0, lcsLength / 10.0);
		} else {
			distance = 0.0;
		}
		this.isInPath = p2.toString().toLowerCase().contains(p1LastSegment);
		this.isInPath1 = p1.toString().toLowerCase().contains(p1LastSegment);
		this.score = WordUtils.scoreTerm(p1.words, p2.words);
		this.sc = Math.min(1.0, p1.segments().length / 10);
		this.sc1 = Math.min(1.0, p2.segments().length / 10);

		this.output = out;
		this.isSt = p1.lastSegment().startsWith(p2.lastSegment()) || p2.lastSegment().startsWith(p1.lastSegment());
		int lcsLength2 = BasicSolver.getLongestCommonSubstring(p1.getParentPath(), p2.getParentPath());
		if (lcsLength2 > 0) {
			longestString2 = lcsLength2 / ((double) p1.getParentPath().length() * p2.getParentPath().length());
			this.lcsLength2 = Math.min(1.0, lcsLength / 10.0);
		} else {
			distance = 0.0;
		}

	}
//	@In
//	@WordVector
//	@Transformer(WordSplitter.class)
//	@DocVector(DV3.class)
//	public String propertyName;
//	
//	@In
//	@WordVector
//	@Transformer(WordSplitter.class)
//	@DocVector(DV3.class)
//	public String propertyName1;
	
	public String clean(String string) {
		while (string.length() > 50) {
			int indexOf = string.indexOf('/');
			if (indexOf == -1) {
				indexOf = 50;
			}
			string = string.substring(indexOf + 1);
		}
		return string;
	}

	public static class DV3 extends NDocVector {

		public DV3() {
			super(3);
		}
	}
	
	@In
	private double score2;
	
	@In
	private double capIntersection;
	
//	@In
//	@OneHotInCollection
//	@Assocation(identity=false)
//	HashSet<String>w1=new HashSet<>();
//	
//	@In
//	@OneHotInCollection
//	@Assocation(identity=false)
//	HashSet<String>w2=new HashSet<>();

	@In
	private double commonWordsCount;

	@In
	private boolean allCaps1;

	@In
	private boolean allCaps2;

	
	@In
	private boolean stWith;
	
	@In
	private boolean stWith1;

	@In
	private boolean pathIntersection1;
	
	@In
	private boolean pathIntersection2;
	
	@In
	private double parentPathRate;

	@In
	private double score;

	@In
	private double sc;
	@In
	private double sc1;

	@In
	private boolean isSt;

	@In
	private double position;

	@In
	protected boolean isAbbr;

	@In
	protected boolean isInPath;

	@In
	private boolean isInPath1;

	@In
	private double longestString2;
	
	@In
	private boolean sameNumber;

	@In
	private double lcsLength2;

	@In
	protected double lcsLength;

	@In
	protected double distance;

	@In
	protected double longestString;

	@Out
	@OneHot
	protected boolean output;

}