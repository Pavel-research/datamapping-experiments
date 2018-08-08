package easymapping.nn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mulesoft.nn.data.WordExtractor;

public class WordSplitter implements Function<String, List<String>> {

	WordExtractor ex;

	@Override
	public List<String> apply(String name) {
		
		if (ex == null) {
			ex = new WordExtractor();
		}
		ArrayList<String> extractVectorWords = ex.extractVectorWords(name);
		ArrayList<String>rs=new ArrayList<>();
		extractVectorWords.forEach(v->{rs.add(v.toLowerCase());});
		return rs;
	}
}