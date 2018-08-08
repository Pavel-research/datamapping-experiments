package easymapping.nn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.mulesoft.nn.config.ActivationFunction;
import com.mulesoft.nn.config.LearningAlgoritm;
import com.mulesoft.nn.config.LearningScheme;
import com.mulesoft.nn.config.LossFunction;
import com.mulesoft.nn.data.Utils;
import com.mulesoft.nn.workspace.impl.PreparedDataSetArea;
import com.mulesoft.nn.workspace.impl.UnigramTable;
import com.mulesoft.nn.workspace.impl.Workspace;

import easymapping.BaseLine;
import easymapping.BasicSolver;
import easymapping.BasicSolver.MappingContext;
import easymapping.MappingPath;
import easymapping.SolveStat;

public class NT2 {

	private static ArrayList<TestElement> mappings;
	
	static HashSet<String>oftenWords=new HashSet<>();

	public static void main(String[] args) {
		initOften();
		PreparedDataSetArea<NTPairs> prepare = Workspace.getDefault().prepare(createDataSource(), true);
//		String all = getKey();
//		prepare = Workspace.getDefault().prepare(createDataSource(), true);
//		if (!all.equals(getKey())) {
//			throw new IllegalStateException();
//		}
		
		
		
		CompleteModel model = prepare.input().conv1ds(1, ActivationFunction.RELU, 250, 40).conv1d(1, 1, ActivationFunction.ELU).flatten()
				.dense(ActivationFunction.ELU, 1000)
				.outBlock(ActivationFunction.SOFTMAX, LossFunction.CATEGORICAL_CROSS_ENTROPY).toModel();
		prepare.setMaxEpochCount(20);
		prepare.experiment("a", model, new LearningScheme(LearningAlgoritm.ADAM, 80));
		
//		String json1 = prepare.createSetup(model, new LearningScheme(LearningAlgoritm.ADAM,80)).toJSON();
//		
		ITrainableModel<NTPairs> model2 = prepare.best().model();
		doTest(model2);
//		prepare = Workspace.getDefault().prepare(createDataSource(), true);
//		model2 = prepare.best().model();
//		doTest(model2);		
	}

	public static void initOften() {
		UnigramTable ts=new UnigramTable();
		try {
			BaseLine.readAllMappings().forEach(m->{
				m.notTrivial().forEach(item->{
					item.firstWords().forEach(v->{
						if (v.length()>1) {
						ts.add(v.toLowerCase());
						}
					});
					item.secondWords().forEach(r->{
						if (r.length()>1) {
						ts.add(r.toLowerCase());
						}
					});
				});
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] topN = ts.topN(20);
		oftenWords.addAll(Arrays.asList(topN));
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
	public static void doTest(ITrainableModel<NTPairs> model2) {
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
					ArrayList<MappingPath> arrayList = new ArrayList<>(unmapped.mappings);
					Collection<TopNOptions> clasify = model2.clasify(e.pairs, 6);
					ArrayList<Opt> opts = new ArrayList<>();
					for (var t : clasify) {
						NTPairs target = (NTPairs) t.getTarget();

						MappingPath original = target.original;

						ArrayList<Option<Integer>> values = t.getValues();
						Utils.toJson(target);
						for (Option<Integer> object : values) {

							if (object.value < arrayList.size()) {
								MappingPath mappingPath = arrayList.get(object.value);
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

	static PreseparatedDataSource<NTPairs> createDataSource() {
		mappings = getMappings();
		Random random = new Random(23223);
		ArrayList<NTPairs> testPairs = new ArrayList<>();
		ArrayList<NTPairs> trainPairs = new ArrayList<>();
		ArrayList<NTPairs> allPairs = new ArrayList<>();
		for (TestElement e : mappings) {

			if (random.nextInt(5) == 4) {
				testPairs.addAll(e.pairs);
				e.test = true;
			} else {
				if (!e.pairs.isEmpty()) {
					e.pairs.forEach(v -> {
						if (e.unmapped.mappings.contains(e.unmapped.correct(v.original))) {
							trainPairs.add(v);
						}
					});
				}
			}
			allPairs.addAll(e.pairs);
		}
		return new PreseparatedDataSource<>("tsa", allPairs, trainPairs, testPairs, NTPairs.class);
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
					p.output = true;
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
