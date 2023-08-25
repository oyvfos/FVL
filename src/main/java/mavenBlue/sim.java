package mavenBlue;

import java.util.stream.IntStream;

import umontreal.ssj.randvar.NormalGen;
import umontreal.ssj.randvar.RandomVariateGen;
import umontreal.ssj.rng.MRG31k3p;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.stochprocess.OrnsteinUhlenbeckProcess;

public class sim {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		RandomStream stream1 = new MRG31k3p();
	      

	      // Create 3 parallel streams of normal random variates
	      RandomVariateGen gen1 = new NormalGen (stream1);
	      OrnsteinUhlenbeckProcess ou = new OrnsteinUhlenbeckProcess(0, 0.01, .01, .01,
	  	        stream1); 
	      ou.setObservationTimes(.25, 120);
	      //double[] times = IntStream.range(0, 120).mapToDouble(i -> i*.25).toArray();
	      double[] out = ou.generatePath();
	      System.out.println(ou.getPath());
	}

}
