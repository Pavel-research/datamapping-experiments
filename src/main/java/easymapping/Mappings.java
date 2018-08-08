package easymapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Mappings {
	protected String fileName;

	public Mappings(String fileName) {
		super();
		this.fileName = fileName;
	}

	protected ArrayList<Mapping> allMappings = new ArrayList<>();
	private ArrayList<Mapping> notTrivialMappings;

	protected void cleanNot1to1() {
		HashSet<String> ss = new HashSet<>();
		HashSet<String> ss1 = new HashSet<>();
		ArrayList<Mapping> fMapping = new ArrayList<>();
		for (Mapping m : allMappings) {
			if (ss.contains(m.first)) {
				continue;
			}
			if (ss1.contains(m.second)) {
				continue;
			}
			ss.add(m.first);
			ss1.add(m.second);
			fMapping.add(m);
		}
		this.allMappings = fMapping;
	}

	public int notTrivialCount() {
		List<Mapping> notTrivial = notTrivial();
		if (notTrivial.size() == 1) {
			return 0;
		}
		return notTrivial.size();
	}

	public List<Mapping> notTrivial() {
		if (notTrivialMappings != null) {
			return notTrivialMappings;
		}
		notTrivialMappings = new ArrayList<>();
		for (Mapping m : allMappings) {
			if (!m.isTrivial()) {
				notTrivialMappings.add(m);
			}
		}
		return notTrivialMappings;
	}

	public BasicSolver notTrivialMappings() {
		ArrayList<MappingPath> lpaths = new ArrayList<>();
		ArrayList<MappingPath> rpaths = new ArrayList<>();
		BasicSolver notTrivialMappings2 = new BasicSolver(lpaths, rpaths);
		HashSet<String> one = new HashSet<>();
		HashSet<String> sone = new HashSet<>();
		this.allMappings.forEach(v -> {
			if (!one.contains(v.first)) {
				//one.add(v.first);
				if (!sone.contains(v.second)) {
					//sone.add(v.second);
					var lp = new MappingPath(v.first, (List<String>) WordUtils.wordsOfLastSegment(v.first));
					lpaths.add(lp);
					var rp = new MappingPath(v.second, (List<String>) WordUtils.wordsOfLastSegment(v.second));
					notTrivialMappings2.correct.put(lp, rp);
					rpaths.add(rp);
				}
			}
		});
		notTrivialMappings2.commit();
		return notTrivialMappings2;
	}
}