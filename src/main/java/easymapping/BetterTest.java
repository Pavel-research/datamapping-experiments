package easymapping;

import java.io.IOException;

import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.mulesoft.nn.workspace.impl.Workspace;
import com.mulesoft.omodel.compact.ClassProtoMap;
import com.mulesoft.omodel.compact.WorkspaceCompact;

public class BetterTest {

	static int ma=0;
	static int sa=0;
	private static int sa2;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ClassProtoMap default1 = ClassProtoMap.getDefault();
		try {
			BaseLine.readAllMappings().forEach(v->{
				v.notTrivial().forEach(m->{
					IntIntOpenHashMap coocurences = default1.getCoocurences(WorkspaceCompact.getDefault().getNameRepository().getId(WordUtils.lastSegment(m.first)));
					int id = WorkspaceCompact.getDefault().getNameRepository().getId(WordUtils.lastSegment(m.second));
					IntIntOpenHashMap coocurences1 = default1.getCoocurences(id);
					ma++;
					if (coocurences!=null&&coocurences1!=null) {
						if (!coocurences.isEmpty()&&!coocurences1.isEmpty()) {
							if (coocurences.containsKey(id)) {
								sa2++;
							}
							sa++;
						}
					}
				});
			});
			System.out.println(ma+":"+sa+":"+sa2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
