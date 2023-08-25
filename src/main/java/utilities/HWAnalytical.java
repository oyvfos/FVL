package utilities;

import com.opengamma.strata.collect.ArgChecker;

import pricer.DiscountingPolicyTradePricer;

public class HWAnalytical {
	private static double a;
	private static double b;
	private static double s;
	public HWAnalytical(
		      double a,double b,double s ) {
		    this.a= a;
		    this.b= b;
		    this.s= s;
		    ;
		  }
	  
	final static Function<Double, Double> vas = new Function<Double, Double>() {
        @Override
        public Double evaluate(final Double... ts) {
          final double t = ts[0];
          final double r0 = ts[1];
          //return (r0 - thetaHW/kappa)/Math.exp(kappa*t) + thetaHW/kappa;
          return 0d;
          
        }
      };
      final public static Function<Double, Double> B = new Function<Double, Double>() {
          @Override
          public Double evaluate(final Double... ts) { 
            final double t = ts[0];
            final double T = ts[1];
            return (1 - Math.exp((-b)*(T - t)))/b; 
          }
        };
        final static Function<Double, Double> A = new Function<Double, Double>() {
            @Override
            public Double evaluate(final Double... ts) {
              final double t = ts[0];
              final double T = ts[1];
              return (a/b - Math.pow(s,2)/(2*Math.pow(b, 2)))*(B.evaluate(t,T) - (T - t)) - (Math.pow(s, 2)*Math.pow(B.evaluate(t, T),2)/(4*b));               
            }
        };
        final static Function<Double, Double> Z = new Function<Double, Double>() {
            @Override
            public Double evaluate(final Double... ts) {
              final double t = ts[0];
              final double T = ts[1];
              final double r = ts[2];
              //return Math.exp(A.evaluate(t, T) - vas.evaluate(t, r)* B.evaluate(t, T));
              return Math.exp(A.evaluate(t, T) - r* B.evaluate(t, T));
            }
          };
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		System.out.println(Z.evaluate(new Double[] {0d,110d}));
	}
	public static double price(Double t, Double T,Double r0) {
		// TODO Auto-generated method stub
		
		return Z.evaluate(new Double[] {t,T,r0});
	}
	public static double B(double t, double T) {
		// TODO Auto-generated method stub
		return B.evaluate(t,T);
	}
}
