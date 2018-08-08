package easymapping.nn;

import java.util.ArrayList;

import com.mulesoft.nn.api.SimpleDataSource;
import com.mulesoft.nn.blocks.CompleteModel;
import com.mulesoft.nn.config.ActivationFunction;
import com.mulesoft.nn.config.LearningAlgoritm;
import com.mulesoft.nn.config.LearningScheme;
import com.mulesoft.nn.config.LossFunction;
import com.mulesoft.nn.workspace.impl.PreparedDataSetArea;
import com.mulesoft.nn.workspace.impl.Workspace;

import easymapping.BaseLine;
import easymapping.BasicSolver.MappingContext;

public class NT1 {

	public static void main(String[] args) {
		PreparedDataSetArea<NTPair> prepare = Workspace.getDefault()
				.prepare(SimpleDataSource.create("nt1", NT1::getMappings, NTPair.class), true);

		CompleteModel model = prepare.input().dense(ActivationFunction.RELU, 400,200).outBlock(ActivationFunction.SOFTMAX,LossFunction.CATEGORICAL_CROSS_ENTROPY).toModel();
		prepare.experiment("a", model, new LearningScheme(LearningAlgoritm.ADAM, 100));
	}

	public static ArrayList<NTPair> getMappings() {
		ArrayList<NTPair> pairs = new ArrayList<>();
		BaseLine.solvers().forEach(v -> {
			MappingContext unmapped = v.unmapped();
			if (unmapped.umappings.size() > 1) {
				int i=0;
				for (var o:unmapped.umappings){
					var ip=i;
					if (unmapped.mappings.contains(unmapped.correct(o))) {
						
						unmapped.mappings.forEach(m -> {
							pairs.add(new NTPair(o, m, unmapped.correct(o).equals(m),ip,unmapped.mappings.size(), 0));
						});

					}
					i++;
				};
			}
		});
		return pairs;
	}

}
