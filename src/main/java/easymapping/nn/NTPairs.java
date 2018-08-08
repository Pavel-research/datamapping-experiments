package easymapping.nn;

import com.mulesoft.nn.annotions.Augmentation;
import com.mulesoft.nn.annotions.In;
import com.mulesoft.nn.annotions.OneHot;
import com.mulesoft.nn.annotions.Out;

import easymapping.MappingPath;

 
@Augmentation(NTPairsAugmentor.class)
public class NTPairs {

	@In	
	NTPair[] pairs;	
	protected MappingPath original;
	
	
	@Out
	@OneHot(vocabulary= LinearGen.class)
	
	int outs;	
	public NTPairs(MappingPath original,NTPair[] pairs, boolean[] outs) {
		super();
		this.original=original;
		this.pairs = pairs;
		for (int i=0;i<outs.length;i++) {
			if (outs[i]) {
				this.outs=i;
				break;
			}
		}
	}

	public NTPairs(MappingPath original2, NTPair[] array, int newId) {
		this.original=original2;
		this.pairs=array;
		this.outs=newId;
	}

	
}
