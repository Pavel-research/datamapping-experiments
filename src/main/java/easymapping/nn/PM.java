package easymapping.nn;

import java.util.ArrayList;

import com.mulesoft.nn.annotions.In;
import com.mulesoft.nn.annotions.Out;
import com.mulesoft.nn.api.ITrainableModel;
import com.mulesoft.nn.blocks.CompleteModel;
import com.mulesoft.nn.config.ActivationFunction;
import com.mulesoft.nn.config.LearningAlgoritm;
import com.mulesoft.nn.config.LearningScheme;
import com.mulesoft.nn.config.LossFunction;
import com.mulesoft.nn.ga.GARandom;
import com.mulesoft.nn.workspace.impl.PreparedDataSetArea;
import com.mulesoft.nn.workspace.impl.Workspace;

public class PM {

	static final int FEATURE_COUNT = 220;

	static final int BC = 200;

	static final int EXC = 1000;

	static class Example {

		@In
		boolean[] features = new boolean[FEATURE_COUNT];

		@Out
		boolean output;

		public Example() {
			int nextInt = GARandom.nextInt(0, FEATURE_COUNT);
			features[nextInt] = true;
			output = nextInt < FEATURE_COUNT / 2;
		}
	}

	static class Examples2 {

		@In
		boolean[][] features;

		@Out
		boolean[] outputs;

		public Examples2(int batch) {
			this.features = new boolean[batch][FEATURE_COUNT];
			this.outputs = new boolean[batch];
		}
	}

	public static void main(String[] args) {
		PreparedDataSetArea<Example> prepare = Workspace.getDefault().prepare(Example.class, createExamples());
		CompleteModel model = prepare.input().dense(ActivationFunction.RELU, 200, 100, 5).classifier().toModel();
		prepare.setMaxEpochCount(1);
		prepare.experiment(model, new LearningScheme(LearningAlgoritm.ADAM, 100));

		ArrayList<Example> createExamples = createExamples();

		ITrainableModel<Example> model2 = prepare.best().model();
		
		long l0 = System.currentTimeMillis();
		model2.setPredictBatchSize(EXC*32);
		model2.predict(createExamples);
		long l1 = System.currentTimeMillis();
		System.out.println(l1 - l0);
		
		PreparedDataSetArea<Examples2> prepare2 = Workspace.getDefault().prepare(Examples2.class, createExamples2());
		CompleteModel model3 = prepare2.input().conv1ds(1, ActivationFunction.RELU, 200, 100, 1).flatten().out("outputs", LossFunction.BINARY_CROSS_ENTROPY).toModel();
		prepare2.setMaxEpochCount(1);
		prepare2.experiment(model3, new LearningScheme(LearningAlgoritm.ADAM, 100));
		ITrainableModel<Examples2> model4 = prepare2.best().model();
		ArrayList<Examples2> createExamples2 = createExamples2();
		long l01 = System.currentTimeMillis();
		model4.predict(createExamples2);
		long l11 = System.currentTimeMillis();
		System.out.println(l11 - l01);
	}

	private static ArrayList<Example> createExamples() {
		ArrayList<Example> es = new ArrayList<>();
		for (int i = 0; i < EXC * BC; i++) {
			es.add(new Example());
		}
		return es;
	}

	private static ArrayList<Examples2> createExamples2() {
		ArrayList<Examples2> es = new ArrayList<>();
		for (int i = 0; i < BC; i++) {
			es.add(new Examples2(EXC));
		}
		return es;
	}
}
