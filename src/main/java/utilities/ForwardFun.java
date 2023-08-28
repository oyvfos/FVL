package utilities;

import com.opengamma.strata.collect.ArgChecker;



public class ForwardFun {
	private static double di;
	private static double b;
	private static double s;
	public ForwardFun(
		      double di) {
		    this.di= 0.0054;
		   
		    ;
		  }
	  
	final static Function<Double, Double> vas = new Function<Double, Double>() {
        @Override
        public Double evaluate(final Double... ts) {
          final double x = ts[0];
          final double fwd1 = ts[1];
          final double vol = ts[2];
          double di = 0.0054;
          //return (r0 - thetaHW/kappa)/Math.exp(kappa*t) + thetaHW/kappa;
          return Math.exp(Math.log(fwd1+di)+x*vol+0.5*vol*vol)-di;
          
        }
      };
      
        
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//System.out.println(Z.evaluate(new Double[] {0d,110d}));
	}
	public static double rate(Double x, Double fw, Double vol) {
		// TODO Auto-generated method stub
		
		return vas.evaluate(new Double[] {x,fw,vol});
	}
	
}
