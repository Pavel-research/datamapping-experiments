package easymapping.nn;

import java.util.ArrayList;
import java.util.List;

import com.mulesoft.nn.api.IVocabGenerator;

public class LinearGen implements IVocabGenerator{

	@Override
	public List<Object> vocabulary() {
		ArrayList<Object>r=new ArrayList<>();
		for (int i=0;i<2000;i++) {
			r.add(i);
		}
		return r;
	}

}
