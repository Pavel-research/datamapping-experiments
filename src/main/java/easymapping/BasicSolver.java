package easymapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.mulesoft.nn.data.WordSplitter;
import com.mulesoft.nn.ga.GARandom;
import com.mulesoft.nn.workspace.impl.UnigramTable;
import com.mulesoft.nn.workspace.impl.UnigramTable.Pair;
import com.mulesoft.nn.workspace.impl.Word2VecManager;

public class BasicSolver {

	protected ArrayList<MappingPath> right;
	protected ArrayList<MappingPath> left;

	protected HashMap<MappingPath, MappingPath> correct = new HashMap<>();

	protected HashMap<String, ArrayList<MappingPath>> passToMapping = new HashMap<>();

	protected HashMap<String, MappingPath> rightByPath = new HashMap<>();
	static int unmatched;

	public BasicSolver(ArrayList<MappingPath> leftPaths, ArrayList<MappingPath> rightPaths) {
		this.left = leftPaths;
		this.right = rightPaths;
	}

	protected void commit() {
		cleanCommonParts(left);
		cleanCommonParts(right);
		for (var p : this.right) {
			rightByPath.put(p.path, p);
			for (String s : p.words) {
				String lowerCase = s.toLowerCase();
				ArrayList<MappingPath> arrayList = passToMapping.get(lowerCase);
				if (arrayList == null) {
					arrayList = new ArrayList<>();
					passToMapping.put(lowerCase, arrayList);
				}
				arrayList.add(p);
			}
		}
	}

	public SolveStat trySolve() {
		MappingContext mappingContext = unmapped();
		proceedUnmapped(mappingContext);
		return mappingContext.solveStat;
	}

	public MappingContext unmapped() {
		MappingContext mappingContext = new MappingContext();
		for (var left : this.left) {
			if (!mapByLastSegment(mappingContext, left)) {
				boolean mapByLastSegments = mapByLastSegments(mappingContext, left);
				if (!mapByLastSegments) {
					UnigramTable ut = buildWordMatches(left);
					if (ut.isEmpty()) {
						tryToSelectUnmatched(mappingContext, left);
					} else {
						proceedWordMatches(mappingContext, left, ut.pairs());
					}
				}
			}

		}

		return mappingContext;
	}

	public MappingContext unmapped2() {
		MappingContext mappingContext = new MappingContext();
		for (var left : this.left) {
			if (!mapByLastSegment(mappingContext, left)) {
				boolean mapByLastSegments = mapByLastSegments(mappingContext, left);
				if (!mapByLastSegments) {
					//UnigramTable ut = buildWordMatches(left);
					tryToSelectUnmatched(mappingContext, left);
					//proceedWordMatches(mappingContext, left, ut.pairs());

				}
			}

		}

		return mappingContext;
	}

	private boolean mapByLastSegment(MappingContext context, MappingPath left) {
		String lastSegment = WordUtils.basicCleanName(left.lastSegment());
		ArrayList<MappingPath> ps = new ArrayList<>();
		String[] segments = left.segments();
		String prevSegment = null;
		if (segments.length > 1) {
			prevSegment = WordUtils.basicCleanName(segments[segments.length - 2]);
		}
		for (var r : this.right) {
			String lastSegment2 = WordUtils.basicCleanName(r.lastSegment());

			if (lastSegment.equalsIgnoreCase(lastSegment2)) {
				ps.add(r);
			} else if (prevSegment != null && lastSegment2.equalsIgnoreCase(prevSegment) && prevSegment.length() > 2) {
				if (!prevSegment.toLowerCase().equals(prevSegment)) {
					ps.add(r);
				}
			}
		}
		return selectBest(context, left, ps);
	}

	private boolean mapByLastSegments(MappingContext context, MappingPath left) {

		ArrayList<MappingPath> ps = new ArrayList<>();
		for (var r : this.right) {
			if (context.mappings.contains(r)) {
				if (testPathMatch(left, r) || testPathMatch(r, left)) {
					ps.add(r);
				}
			}
		}
		return selectBest(context, left, ps);
	}

	public boolean testPathMatch(MappingPath left, MappingPath r) {
		List<String> words = new ArrayList<>();
		left.words.forEach(ra -> words.add(ra.toLowerCase()));
		String[] lastSegment2 = r.segments();
		boolean matched = false;
		for (int i = lastSegment2.length - 1; i >= 0; i--) {
			if (words.remove(lastSegment2[i].toLowerCase())) {
				if (words.isEmpty()) {
					matched = true;
					break;
				}
			} else {
				break;
			}
		}
		return matched;
	}

	static int lsC = 0;
	static int lsIC = 0;

	private boolean selectBest(MappingContext context, MappingPath left, ArrayList<MappingPath> ps) {
		boolean mappedByRelevantLastSegment = false;
		if (!ps.isEmpty()) {
			MappingPath r = null;
			if (ps.size() == 1) {
				r = ps.get(0);
			} else {
				r = selectMostRelevantByOuterPath(left, ps, r);
			}
			if (r != null) {
				context.mappings.remove(r);
				context.performedMappings.put(left, r);
				if (correct.get(left).equals(r)) {
					context.solveStat.correctCount++;
					lsC++;
				} else {
					context.solveStat.incorrectCount++;
					lsIC++;
				}
				mappedByRelevantLastSegment = true;
			}
		}
		return mappedByRelevantLastSegment;
	}

	private MappingPath selectMostRelevantByOuterPath(MappingPath l, ArrayList<MappingPath> ps, MappingPath r) {
		for (MappingPath q : ps) {
			if (q.toString().equals(l.toString())) {
				r = q;
				break;
			}
		}
		if (r == null) {
			String[] segments = l.segments();
			int sm = 0;
			MappingPath cand = null;
			for (MappingPath q : ps) {
				String[] segments1 = q.segments();
				int mc = 0;
				for (int i = segments.length - 2; i >= 0; i--) {
					String ee = segments[i].toLowerCase();
					int dlt = segments.length - i;
					if (segments1.length >= dlt) {
						String eq = segments1[segments1.length - dlt].toLowerCase();
						if (WordUtils.equalsIgnoringPlurality(ee, eq)) {
							mc++;
						} else {
							break;
						}
					}
				}
				if (mc > sm) {
					cand = q;
					sm = mc;
				}
			}
			if (cand != null && sm > 0) {
				r = cand;
			}
		}
		if (r == null) {
			r = ps.get(0);
		}
		return r;
	}

	private UnigramTable buildWordMatches(MappingPath l) {
		UnigramTable ut = new UnigramTable();
		for (String m : l.words) {
			String lowerCase = m.toLowerCase();
			ArrayList<MappingPath> arrayList = passToMapping.get(lowerCase);
			if (arrayList != null) {
				for (MappingPath q : arrayList) {
					ut.add(q.path);
				}
			}
		}
		return ut;
	}

	static int floatingSegmentSucc;
	static int floatingSegmentWrong;

	private void tryToSelectUnmatched(MappingContext context, MappingPath l) {
		SolveStat solveStat = context.solveStat;
		MappingPath correctPath = this.correct.get(l);
		MappingPath mm = null;

		String lastSegment = l.lastSegment();
		l2: for (MappingPath r : this.right) {
			String[] split = r.segments();
			for (String q : split) {
				if (q.toLowerCase().equals(lastSegment)) {
					mm = r;
					break l2;
				}
			}
		}
		if (mm == null) {
			lastSegment = l.nm();
			for (MappingPath r : this.right) {
				if (l.nm().equals(r.nm())) {
					mm = r;
					break;
				}
			}
		}
		if (mm != null && context.mappings.contains(mm)) {
			context.performedMappings.put(l, mm);
			context.mappings.remove(mm);
			if (mm.equals(correctPath)) {
				floatingSegmentSucc++;
				solveStat.correctCount++;
			} else {
				floatingSegmentWrong++;
				solveStat.incorrectCount++;

			}
		} else {
			context.umappings.add(l);
			solveStat.unknownCount++;
		}
	}

	public class MappingContext {
		public final SolveStat solveStat;
		public final LinkedHashSet<MappingPath> mappings = new LinkedHashSet<>(right);
		public final LinkedHashSet<MappingPath> umappings = new LinkedHashSet<>();

		protected HashMap<MappingPath, MappingPath> performedMappings = new LinkedHashMap<>();

		public HashMap<MappingPath, MappingPath> getPerformedMappings() {
			return performedMappings;
		}

		public MappingContext() {
			this.solveStat = new SolveStat(0, 0, 0);
		}

		public MappingPath correct(MappingPath p) {
			return correct.get(p);
		}
	}

	static int alreadyMappedPartialMatch = 0;
	static int wrongPartialMatch = 0;
	static int goodPartialMatch = 0;

	static int goodCandidatesWordMatches = 0;
	static int allCandidatesWordMatches = 0;

	private void proceedWordMatches(MappingContext parameterObject, MappingPath l, ArrayList<Pair> pairs) {
		MappingPath correctPath = this.correct.get(l);
		ArrayList<MappingPath> candidates = new ArrayList<>();
		int c = -1;
		for (var s : pairs) {
			MappingPath mappingPath = rightByPath.get(s.word);
			if (!parameterObject.mappings.contains(mappingPath)) {
				continue;
			}
			if (c == -1) {
				c = s.count;
			} else {
				if (s.count < c) {
					break;
				}
			}
			candidates.add(mappingPath);
		}

		MappingPath mappingPath = null;
		int maxScore = -1;
		if (candidates.contains(correctPath)) {
			goodCandidatesWordMatches++;
		}
		if (parameterObject.mappings.contains(correctPath)) {
			allCandidatesWordMatches++;
		}
		if (candidates.size() > 1) {
			Collections.shuffle(candidates);
		}
		for (MappingPath d : candidates) {
			int s = score(d, l);
			if (s > maxScore) {
				mappingPath = d;
				maxScore = s;
			}
		}
		if (mappingPath == null) {
			parameterObject.umappings.add(l);
			parameterObject.solveStat.unknownCount++;
			alreadyMappedPartialMatch++;
		} else {
			parameterObject.mappings.remove(mappingPath);
			if (correctPath.equals(mappingPath)) {
				parameterObject.solveStat.correctCount++;
				goodPartialMatch++;
			} else {
				parameterObject.solveStat.incorrectCount++;
				wrongPartialMatch++;
			}
		}
	}

	static int goodUnmappedMatched = 0;
	static double wrongUnmmapped;

	static int rSelectMatched = 0;
	static double rSelectwrongUnmmapped;

	static int potentialUnmpped = 0;

	private void proceedUnmapped(MappingContext context) {
		for (MappingPath p : new ArrayList<>(context.umappings)) {

			MappingPath next = select(context.mappings, p);
			if (next != null) {
				MappingPath correctMappingPath = correct.get(p);
				if (context.mappings.contains(correctMappingPath)) {
					potentialUnmpped++;
				}
				if (correctMappingPath.equals(next)) {
					context.solveStat.correctCount++;
					context.solveStat.unknownCount--;
					goodUnmappedMatched++;
				} else {
					if (context.mappings.contains(correctMappingPath)) {
						System.out.println(p + "=>" + next + " but should :" + correctMappingPath.toString());
					}
					wrongUnmmapped++;
				}
				context.mappings.remove(next);
				context.umappings.remove(p);
			}
		}
		if (!context.umappings.isEmpty()) {
			for (MappingPath p : new ArrayList<>(context.umappings)) {
				MappingPath next = selectRandom(context.mappings, p);
				if (next != null) {
					if (correct.get(p).equals(next)) {
						context.solveStat.correctCount++;
						context.solveStat.unknownCount--;
						rSelectMatched++;
					} else {
						rSelectwrongUnmmapped++;
					}
					context.mappings.remove(next);
					context.umappings.remove(p);
				}
			}
		}
		if (!context.umappings.isEmpty()) {
			unmatched++;
		}
	}

	public static int score(MappingPath d, MappingPath l) {
		int c = 0;
		if (d.words.size() > 0 && l.words.size() > 0) {
			if (d.words.get(d.words.size() - 1).equalsIgnoreCase(l.words.get(l.words.size() - 1))) {
				c += 2;
			}
		}
		if (d.path.contains(l.lastSegment())) {
			c += 4;
		}
		if (l.path.contains(d.lastSegment())) {
			c += 4;
		}

		c += partialAdjust(d, l);
		c += partialAdjust(l, d);
		HashSet<String> hashSet = new HashSet<>(
				d.words.stream().map(x -> x.toLowerCase()).collect(Collectors.toList()));
		if (d.lastSegment().length() > 0) {
			if (Character.isDigit(d.lastSegment().charAt(d.lastSegment().length() - 1))) {
				hashSet.add(d.lastSegment().substring(d.lastSegment().length() - 1));
			}
		}

		Collection<String> words = new ArrayList<>(l.words);
		if (l.lastSegment().length() > 0) {
			if (Character.isDigit(l.lastSegment().charAt(l.lastSegment().length() - 1))) {
				words.add(l.lastSegment().substring(l.lastSegment().length() - 1));
			}
		}

		hashSet.retainAll(words.stream().map(x -> x.toLowerCase()).collect(Collectors.toList()));
		for (var s : hashSet) {
			if (s.toLowerCase().equals("code") || s.toLowerCase().equals("id")) {
				c += s.length() / 2;
			} else
				c += s.length() * 2;
		}
		for (String s : d.words) {
			if (s.length() == 1) {
				continue;
			}
			if (!hashSet.contains(s)) {
				c -= Math.min(1, s.length() / 3);
			}
		}

		for (String s : words) {
			if (s.length() == 1) {
				continue;
			}
			if (!hashSet.contains(s)) {
				c -= Math.min(1, s.length() / 3);
			}
		}
		if (c < 0) {
			c = 0;
		}
		String[] lseg = l.segments();
		String[] rseg = d.segments();

		int mc = 0;
		int dc = 0;
		for (int i = lseg.length - 2; i >= 0; i--) {
			String ee = lseg[i].toLowerCase();
			int dlt = lseg.length - i;
			if (rseg.length >= dlt) {
				String eq = rseg[rseg.length - dlt].toLowerCase();
				if (WordUtils.equalsIgnoringPlurality(ee, eq)) {
					mc++;
				} else {
					if (WordUtils.hasCommonParts(ee, eq)) {
						dc++;
					}

					break;
				}
			}
		}
		c += mc * 10 + dc * 5;
		return c;
	}

	public static int partialAdjust(MappingPath d, MappingPath l) {
		int m = 0;
		String[] segments2 = d.segments();
		for (String w : l.words) {
			if (w.length() > 2) {
				String[] segments = segments2;
				for (int i = 0; i < segments.length - 1; i++) {
					if (segments[i].toLowerCase().startsWith(w.toLowerCase())) {
						m += 2;
					}
				}
			}
		}
		return m;
	}

	private MappingPath selectRandom(LinkedHashSet<MappingPath> mappings, MappingPath p) {
		return GARandom.choose(new ArrayList<>(mappings));
	}

	private MappingPath select(LinkedHashSet<MappingPath> mappings, MappingPath p) {

		ArrayList<MappingPath> abbrs = new ArrayList<>();
		for (MappingPath path : mappings) {
			boolean b = isPotentialAbbr(p, path);
			if (b) {
				abbrs.add(path);
			}
		}
		if (abbrs.isEmpty()) {
			List<String> apply = new WordSplitter().apply(p.toString()).stream().map(x -> x.toLowerCase())
					.collect(Collectors.toList());
			int mcount = 0;
			if (!apply.isEmpty()) {
				apply.remove(0);
				MappingPath bc = null;
				for (MappingPath path : mappings) {
					List<String> apply1 = new WordSplitter().apply(path.toString());
					apply1.remove(0);
					int c = 0;
					for (String q : apply1) {
						if (apply.contains(q.toLowerCase())) {
							c++;
						}
					}
					if (c > mcount) {
						bc = path;
						mcount = c;
					}
				}
				if (bc != null) {
					return bc;
				}
			}
		}
		if (abbrs.size() > 0) {
			if (abbrs.size() > 1) {
				String lastSegment = p.lastSegment().toLowerCase();
				int mc = 0;
				MappingPath bem = null;
				for (MappingPath pa : abbrs) {
					String ls = pa.lastSegment().toLowerCase();
					int c = 0;
					for (int i = 0; i < lastSegment.length(); i++) {
						if (ls.indexOf(lastSegment.charAt(i)) != -1) {
							c++;
						}
					}
					if (c > mc) {
						bem = pa;
						mc = c;
					}
				}
				return bem;
			}
			return abbrs.get(0);
		}
		String lastSegment = p.lastSegment();
		MappingPath bc = null;
		int md = 0;
		for (MappingPath path : mappings) {
			String string = path.lastSegment();
			int lcsLength = getLongestCommonSubstring(string.toLowerCase(), lastSegment.toLowerCase());
			if (lcsLength > 4 && lcsLength > md) {
				md = lcsLength;
				bc = path;
			}
		}

		if (bc != null) {
			return bc;
		}
		ArrayList<MappingPath> ls = new ArrayList<>();
		if (lastSegment.length() > 3) {
			for (MappingPath path : mappings) {
				if (path.toString().contains(lastSegment)) {
					ls.add(path);
				}
			}
		}
		if (ls.size() == 1 && mappings.size() > 1) {
			return ls.iterator().next();
		}
		ls.clear();
		if (lastSegment.length() > 3) {
			String mm = lastSegment.substring(0, 3).toLowerCase();
			for (MappingPath path : mappings) {
				if (path.lastSegment().toLowerCase().startsWith(mm)) {
					ls.add(path);
				}
			}
		}
		double maxScoreTerm = 0;
		MappingPath mp = null;
		for (MappingPath mm : mappings) {
			double scoreTerm = WordUtils.scoreTerm(p.words, mm.words);
			if (scoreTerm > 0.001) {
				if (maxScoreTerm < scoreTerm) {
					mp = mm;
					maxScoreTerm = scoreTerm;
				}
			}
		}
		if (mp != null) {
			return mp;
		}

		if (ls.size() == 1) {
			return ls.iterator().next();
		}
		if (lastSegment.length() > 3 && Character.isDigit(lastSegment.charAt(lastSegment.length() - 1))) {
			ls.clear();
			String mm = lastSegment.substring(lastSegment.length() - 1);
			for (MappingPath path : mappings) {
				if (path.lastSegment().toLowerCase().endsWith(mm)) {

					var m = path.lastSegment();
					if (m.length() > 2
							&& m.substring(0, 3).toLowerCase().equals(lastSegment.substring(0, 3).toLowerCase())) {
						ls.add(path);
						break;
					}

				}
			}
		}
		if (ls.size() == 1) {
			return ls.iterator().next();
		}

		if (lastSegment.length() > 3 && Character.isDigit(lastSegment.charAt(lastSegment.length() - 1))) {
			ls.clear();
			String mm = lastSegment.substring(lastSegment.length() - 1);
			for (MappingPath path : mappings) {
				if (path.lastSegment().toLowerCase().endsWith(mm)) {
					List<String> apply = new WordSplitter().apply(path.lastSegment());
					for (String m : apply) {
						if (m.length() > 2
								&& m.substring(0, 3).toLowerCase().equals(lastSegment.substring(0, 3).toLowerCase())) {
							ls.add(path);
							break;
						}
					}

				}
			}
		}
		if (ls.size() == 1) {
			return ls.iterator().next();
		}
		double minD = 1.0;
		MappingPath sc = null;
		if (p.words.size() == 1) {
			for (MappingPath q : mappings) {
				for (String a1 : p.words) {
					try {
						int parseInt = Integer.parseInt(a1);
						continue;
					} catch (Exception e) {
						// TODO: handle exception
					}
					if (q.words.size() == 1) {

						for (String ma : q.words) {
							try {
								int parseInt = Integer.parseInt(ma);
								continue;
							} catch (Exception e) {
								// TODO: handle exception
							}
							if (a1.length() > 3 && ma.length() > 3
									&& UnigramTable.getInstance().getOrZero(ma.toLowerCase()) > 5
									&& UnigramTable.getInstance().getOrZero(a1.toLowerCase()) > 5) {
								double distance = Word2VecManager.distance(ma.toLowerCase(), a1.toLowerCase());
								if (Math.abs(distance) < minD) {
									minD = Math.abs(distance);
									sc = q;
								}
							}
						}
					}
				}
			}
		}
		if (sc != null) {
			return sc;
		}
		return GARandom.choose(new ArrayList<>(mappings));
	}

	public static boolean isPotentialAbbr(MappingPath p, MappingPath path) {
		String nm = p.nm();
		String nm2 = path.nm();
		boolean b = WordUtils.isAbbr(nm, nm2)
				|| WordUtils.isAbbr(p.lastSegment().toLowerCase(), path.lastSegment().toLowerCase());
		return b;
	}

	public static int getLongestCommonSubstring(String a, String b) {
		int m = a.length();
		int n = b.length();

		int max = 0;

		int[][] dp = new int[m][n];

		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				if (a.charAt(i) == b.charAt(j)) {
					if (i == 0 || j == 0) {
						dp[i][j] = 1;
					} else {
						dp[i][j] = dp[i - 1][j - 1] + 1;
					}

					if (max < dp[i][j])
						max = dp[i][j];
				}

			}
		}

		return max;
	}

	private void cleanCommonParts(ArrayList<MappingPath> left2) {
		HashSet<String> allWords = null;
		for (MappingPath p : left2) {
			Collection<String> words = p.words;
			if (allWords == null) {
				allWords = new LinkedHashSet<>(words);
			} else {
				allWords.retainAll(words);
			}
		}
		if (!allWords.isEmpty()) {
			for (MappingPath p : left2) {
				p.words.removeAll(allWords);
			}
		}
	}

}