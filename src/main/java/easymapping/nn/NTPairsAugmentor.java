package easymapping.nn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

public class NTPairsAugmentor implements Function<NTPairs,NTPairs>{

	

	@Override
	public NTPairs apply(NTPairs input) {
		NTPair ntPair = input.pairs[input.outs];
		ArrayList<NTPair> arrayList = new ArrayList<>(Arrays.asList(input.pairs));
		Collections.shuffle(arrayList);
		int newId=arrayList.indexOf(ntPair);
		NTPair[] array = arrayList.toArray(new NTPair[arrayList.size()]);
		//input.pairs=array;
		//input.outs=newId;
		return new NTPairs(input.original, array, newId);
	}
	
}