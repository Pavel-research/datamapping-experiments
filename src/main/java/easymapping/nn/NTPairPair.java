package easymapping.nn;

import com.mulesoft.nn.annotions.In;
import com.mulesoft.nn.annotions.OneHotInCollection;
import com.mulesoft.nn.annotions.Out;

 
public class NTPairPair {

	@In	
	NTPair[][] pairs;	
	
	@Out
	@OneHotInCollection
	boolean[][] out;
	
	
	
	public NTPairPair(NTPairs[]pairs) {
		super();
		this.out=new boolean[pairs.length][];
		this.pairs=new NTPair[pairs.length][];
		for (int i=0;i<pairs.length;i++) {
			this.pairs[i]=pairs[i].pairs;
			this.out[i]=new boolean[pairs[i].pairs.length];			
			this.out[i][pairs[i].outs]=true;
		}
	}
	
}
