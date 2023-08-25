package utilities;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import com.google.common.collect.Lists;

public class corrMat {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Object[] g1 = IntStream.range(0, 3).boxed().toArray();//rente
		Object[] g2 = IntStream.range(0, 3).boxed().toArray();
	 	Object[] g3 = IntStream.range(0, 3).boxed().toArray();//eq
	 	List<List<Object>> s = Lists.cartesianProduct(Arrays.asList(g1),Arrays.asList(g2),Arrays.asList(g3),Arrays.asList(g3));
	 	
	}

}
