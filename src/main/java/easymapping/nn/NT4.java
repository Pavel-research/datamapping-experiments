package easymapping.nn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.mulesoft.nn.api.ITrainableModel;
import com.mulesoft.nn.api.PreseparatedDataSource;
import com.mulesoft.nn.api.TopNOptions;
import com.mulesoft.nn.api.TopNOptions.Option;
import com.mulesoft.nn.blocks.CompleteModel;
import com.mulesoft.nn.blocks.ModelElement;
import com.mulesoft.nn.blocks.Shape;
import com.mulesoft.nn.config.ActivationFunction;
import com.mulesoft.nn.config.LearningAlgoritm;
import com.mulesoft.nn.config.LearningScheme;
import com.mulesoft.nn.config.LossFunction;
import com.mulesoft.nn.workspace.impl.Experiment;
import com.mulesoft.nn.workspace.impl.PreparedDataSetArea;
import com.mulesoft.nn.workspace.impl.UnigramTable;
import com.mulesoft.nn.workspace.impl.Workspace;

import easymapping.BaseLine;
import easymapping.BasicSolver;
import easymapping.BasicSolver.MappingContext;
import easymapping.MappingPath;
import easymapping.SolveStat;

public class NT4 {

	private static ArrayList<TestElement> mappings;
	
	static HashSet<String>oftenWords=new HashSet<>();

	public static void main(String[] args) {
		NT2.initOften();
		PreparedDataSetArea<NTPairPair> prepare = Workspace.getDefault().prepare(createDataSource(), true);

		ModelElement input = prepare.input();
		Shape outputShape = input.getOutputShape();
		System.out.println(outputShape);
		
		CompleteModel model = input.conv2d(2, 1,ActivationFunction.RELU)
				.out("out",LossFunction.CATEGORICAL_CROSS_ENTROPY).toModel();
		prepare.setMaxEpochCount(20);
		Experiment<NTPairPair> experiment = prepare.experiment("a", model, new LearningScheme(LearningAlgoritm.ADAM, 3));
		
//		ITrainableModel<NTPair> model2 = prepare.best().model();
//		ClassificationModelEvaluationReport evaluateClassifier = model2.evaluateClassifier(prepare.test());
//		System.out.println(evaluateClassifier);
//		doTest(model2);
//		createDataSource();
//		doTest(model2);
	}

	public static String getKey() {
		StringBuilder bld=new StringBuilder();
		for (TestElement e : mappings) {
			if (e.test) {
				e.pairs.forEach(v->{
					bld.append(v.original.toString());
				});
			}
		}
		String all=bld.toString();
		return all;
	}

	static class Opt implements Comparable<Opt> {
		MappingPath original;

		public Opt(MappingPath original, MappingPath target, double score) {
			super();
			this.original = original;
			this.target = target;
			this.score = score;
		}

		MappingPath target;
		double score;

		@Override
		public int compareTo(Opt o) {
			if (o.score > this.score) {
				return 1;
			}
			if (o.score < this.score) {
				return -1;
			}
			return 0;
		}
	}

	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void doTest(ITrainableModel<NTPair> model2) {
		SolveStat sst = new SolveStat(0, 0, 0);
		SolveStat allTogether = new SolveStat(0, 0, 0);
		long l0=System.currentTimeMillis();
		int c=0;
		int count=0;
		int maxCount=0;
		int mt=0;
		for (TestElement e : mappings) {
			if (e.test) {
				long t0=System.currentTimeMillis();
				c++;
				int i = e.unmapped.mappings.size()+e.unmapped.umappings.size();
				count+=i;
				if (i>maxCount) {
					maxCount=i;
				}
				MappingContext unmapped = e.unmapped;
				if (!e.pairs.isEmpty()) {
					ArrayList<NTPair>ps=new ArrayList<>();
					e.pairs.forEach(p->{
						for (var p1:p.pairs) {
							ps.add(p1);
						}
					});
					Collection<TopNOptions> clasify = model2.clasify(ps, 2);
					ArrayList<Opt> opts = new ArrayList<>();
					for (var t : clasify) {
						NTPair target = (NTPair) t.getTarget();

						MappingPath original = target.p1;
						MappingPath mappingPath = target.p2;
						ArrayList<Option<Boolean>> values = t.getValues();

						for (Option<Boolean> object : values) {

							if (object.value.equals(true)) {
								
								opts.add(new Opt(original, mappingPath, object.probability));
							}
						}
					}
					Collections.sort(opts);
					for (Opt p : opts) {
						if (unmapped.umappings.contains(p.original)) {
							if (unmapped.mappings.contains(p.target)) {
								unmapped.umappings.remove(p.original);
								unmapped.mappings.remove(p.target);
								unmapped.solveStat.add(new SolveStat(0, -1, 0));
								if (p.target.equals(unmapped.correct(p.original))) {
									SolveStat trySolve = new SolveStat(1, 0, 0);
									sst.add(trySolve);
									unmapped.solveStat.add(trySolve);
								} else {
									SolveStat trySolve = new SolveStat(0, 0, 1);
									sst.add(trySolve);
									unmapped.solveStat.add(trySolve);
								}
							}
						}
					}
				}
				allTogether.add(unmapped.solveStat);
				long t1=System.currentTimeMillis();
				
				int td=(int) (t1-t0);
				mt=Math.max(mt, td);
			}
		}
		System.out.println(accuracy(sst));
		System.out.println(accuracy(allTogether));
		long l1=System.currentTimeMillis();
		System.out.println(l1-l0+" "+c+" "+count+" "+maxCount+" "+mt);
	}

	public static double accuracy(SolveStat sst) {
		return sst.getCorrectCount()
				/ (double) (sst.getCorrectCount() + sst.getIncorrectCount() + sst.getUnknownCount());
	}

	static class TestElement {

		public TestElement(BasicSolver v, ArrayList<NTPairs> pairs2, MappingContext unmapped) {
			this.solver = v;
			this.pairs = pairs2;
			this.unmapped = unmapped;
		}

		private MappingContext unmapped;
		BasicSolver solver;
		ArrayList<NTPairs> pairs;
		boolean test;
	}

	static PreseparatedDataSource<NTPairPair> createDataSource() {
		mappings = getMappings();
		Random random = new Random(23223);
		ArrayList<NTPairPair> testPairs = new ArrayList<>();
		ArrayList<NTPairPair> trainPairs = new ArrayList<>();
		ArrayList<NTPairPair> allPairs = new ArrayList<>();
		for (TestElement e : mappings) {

			if (random.nextInt(5) == 4) {
				if (!e.pairs.isEmpty()) {
				testPairs.add(new NTPairPair(e.pairs.toArray(new NTPairs[e.pairs.size()])));
				e.test = true;
				}
			} else {
				if (!e.pairs.isEmpty()) {
					trainPairs.add(new NTPairPair(e.pairs.toArray(new NTPairs[e.pairs.size()])));					
				}
			}
		}
		allPairs.addAll(testPairs);
		allPairs.addAll(trainPairs);
		return new PreseparatedDataSource<>("tsa2", allPairs, trainPairs, testPairs, NTPairPair.class);
	}

	public static ArrayList<TestElement> getMappings() {
		ArrayList<TestElement> elements = new ArrayList<>();

		BaseLine.solvers().forEach(v -> {
			MappingContext unmapped = v.unmapped2();
			HashMap<String, UnigramTable> ts = getAlreadyMappedParents(unmapped);
			
			ArrayList<NTPairs> pairs = new ArrayList<>();
			
			unmapped.umappings.forEach(o -> {
				ArrayList<NTPair> ps = new ArrayList<>();
				UnigramTable unigramTable = ts.get(o.getParentPath());
				unmapped.mappings.forEach(m -> {
					
					int orZero = unigramTable==null?0:unigramTable.getOrZero(m.getParentPath());
					
					
					NTPair e = new NTPair(o, m, unmapped.correct(o).equals(m), 0, 0,unigramTable==null?0:orZero/(unigramTable.sum+0.5));
					
					ps.add(e);
				});
				NTPair[] array = ps.toArray(new NTPair[ps.size()]);
				boolean[] outs = new boolean[array.length];
				int i = 0;
				for (NTPair p : array) {
					outs[i++] = p.output;
					//p.output = true;
				}
				pairs.add(new NTPairs(o, array, outs));
			});
			elements.add(new TestElement(v, pairs, unmapped));
		});
		return elements;
	}

	public static HashMap<String, UnigramTable> getAlreadyMappedParents(MappingContext unmapped) {
		HashMap<MappingPath, MappingPath> performedMappings = unmapped.getPerformedMappings();
		HashMap<String, UnigramTable> ts = new HashMap<>();
		Set<MappingPath> keySet = performedMappings.keySet();
		keySet.forEach(m -> {
			MappingPath mappingPath = performedMappings.get(m);
			String parentPath = m.getParentPath();
			UnigramTable unigramTable = ts.get(parentPath);
			if (unigramTable == null) {
				unigramTable = new UnigramTable();
				ts.put(parentPath, unigramTable);
			}
			unigramTable.add(mappingPath.getParentPath());
		});
		return ts;
	}

}
