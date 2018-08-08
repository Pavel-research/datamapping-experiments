package easymapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BaseLine {

	public static void main(String[] args) throws IOException {
		ArrayList<Mappings> mappings = readAllMappings();
		int xx = 0;
		double nxx = 0;
		int sz = 0;		
		SolveStat solveStat = new SolveStat(0, 0, 0);
		long l0=System.currentTimeMillis();
		for (Mappings m : mappings) {
			xx += m.allMappings.size();
			int notTrivialCount = m.notTrivialCount();
			nxx += notTrivialCount;
			BasicSolver notTrivialMappings = m.notTrivialMappings();
			if (notTrivialMappings.left.size() != notTrivialMappings.correct.size()) {
				throw new IllegalStateException();
			}
			sz = Math.max(notTrivialMappings.left.size(), sz);
			sz = Math.max(notTrivialMappings.right.size(), sz);
			SolveStat trySolve = notTrivialMappings.trySolve();
			if (trySolve.correctCount + trySolve.unknownCount + trySolve.incorrectCount != notTrivialMappings.left
					.size()) {
				throw new IllegalStateException();
			}
			solveStat.add(trySolve);
		}
		long l1=System.currentTimeMillis();
		System.out.println(xx + ":" + nxx + "(" + (xx - nxx) / xx + ")");
		double accuracty = solveStat.correctCount/(double)(solveStat.correctCount+solveStat.unknownCount+solveStat.incorrectCount);
		System.out.println("Accuracy: "+accuracty+" Time:"+(l1-l0));
		System.out.println("Last segments full accuracy:"+BasicSolver.lsC/(double)(BasicSolver.lsC+BasicSolver.lsIC)+": "+(BasicSolver.lsC+BasicSolver.lsIC));
		System.out.println("Floating Sergment:"+BasicSolver.floatingSegmentSucc/((double)BasicSolver.floatingSegmentSucc+BasicSolver.floatingSegmentWrong)+": "+(BasicSolver.floatingSegmentSucc+BasicSolver.floatingSegmentWrong));
		System.out.print("Partial Matches:"+BasicSolver.goodPartialMatch/((double)BasicSolver.goodPartialMatch+BasicSolver.wrongPartialMatch)+": "+(BasicSolver.goodPartialMatch+BasicSolver.wrongPartialMatch+" "));
		System.out.println("Potential Partial Matches:"+BasicSolver.goodCandidatesWordMatches/((double)BasicSolver.goodPartialMatch+BasicSolver.wrongPartialMatch));
		//System.out.println("Potential All Matches:"+BasicSolver.allCandidatesWordMatches/((double)BasicSolver.goodPartialMatch+BasicSolver.wrongPartialMatch));
		System.out.print("Unmapped matches:"+BasicSolver.goodUnmappedMatched/((double)BasicSolver.goodUnmappedMatched+BasicSolver.wrongUnmmapped)+": "+(BasicSolver.goodUnmappedMatched+BasicSolver.wrongUnmmapped+" "));
		System.out.println("Potential inmapped matches:"+BasicSolver.potentialUnmpped/((double)BasicSolver.goodUnmappedMatched+BasicSolver.wrongUnmmapped));
		System.out.println("Last matches:"+BasicSolver.rSelectMatched/((double)BasicSolver.rSelectMatched+BasicSolver.rSelectwrongUnmmapped)+": "+(BasicSolver.rSelectMatched+BasicSolver.rSelectwrongUnmmapped));
		System.out.println("Left right imbalance:"+BasicSolver.unmatched);
		System.out.println("Total interesting:"+(solveStat.correctCount+solveStat.unknownCount+solveStat.incorrectCount));
	}
	
	public static List<BasicSolver>solvers(){
		try {
			return readAllMappings().stream().map(x->x.notTrivialMappings()).collect(Collectors.toList());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static ArrayList<Mappings> readAllMappings() throws IOException {
		var lines = Files.lines(Paths.get("D:/mappings/final_mapping.csv"));
		ArrayList<Mappings> mappings = new ArrayList<>();
		lines.forEach(l -> {
			String[] split = l.split(",");
			String f = split[0];
			String f1 = split[1];
			String fName = split[7];
			Mapping x = new Mapping(f, f1, fName);
			if (mappings.isEmpty()) {
				Mappings ms = new Mappings(fName);
				ms.allMappings.add(x);
				mappings.add(ms);
			} else {
				Mappings ma = mappings.get(mappings.size() - 1);
				if (ma.fileName.equals(x.fName)) {
					ma.allMappings.add(x);
				} else {
					Mappings ms = new Mappings(fName);
					ms.allMappings.add(x);
					mappings.add(ms);
				}
			}			
		});
		lines.close();
		for (Mappings m : mappings) {
			m.cleanNot1to1();
		}
		return mappings;
	}
}