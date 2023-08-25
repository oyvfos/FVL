package utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.simple.SimpleMatrix;
import org.ejml.sparse.csc.CommonOps_DSCC;

import com.google.common.collect.Lists;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
public class Taylor2 {

    private static final String MatrixAlgebra = null;
	// return n!
    public static int factorial(int n) {
        if (n == 0) return 1;
        return n * factorial(n-1);
    }

    public static RationalPolynomial UFDWeights(int m,int n,int s) {
    	int N = n+1;//Integer.parseInt(args[0]);
  
     // Taylor series for log(x)
        RationalPolynomial l = RationalPolynomial.ZERO;
        
        RationalPolynomial l1 = new RationalPolynomial(new BigRational(-1, 1),0);
        RationalPolynomial l2 = l1.plus(new RationalPolynomial(new BigRational(1, 1),1));
        List<RationalPolynomial> array = new ArrayList() ;
        //array.add(new RationalPolynomial(new BigRational(1, 1),0));
        array.add(l2);
        array.add(l2.times(l2));
        array.add(l2.times(l2).times(l2));
        array.add(l2.times(l2).times(l2).times(l2));
        
        for (int i = 1; i <= N; i++) {
        	 BigRational coef;
        	if      (i % 2 == 0) coef = new BigRational(-1, i);
            else                 coef = new BigRational(1, i);
            //System.out.println(term);
        	RationalPolynomial term = new RationalPolynomial(coef, i);
            l = l.plus(term);
        }
        //System.out.println("log(x) = " + l);
        
     // Taylor series for x^n
        RationalPolynomial p = new RationalPolynomial(BigRational.ZERO,N);
        
        Polynomial p1 = new Polynomial(1,s);
        for (int i = 0; i <= s; i++) {
        	//System.out.println(p1.evaluate(1));
        	
        	BigRational coef = BigRational.ZERO;
        	if (i<=s)  coef=  new BigRational(p1.evaluate(1), factorial(i));
            //RationalPolynomial term = new RationalPolynomial(coef, 0).times(array.get(i));
            RationalPolynomial term = new RationalPolynomial(coef, i);
            p = p.plus(term);
            if (p1.degree()!= -1) p1=p1.differentiate();
           // System.out.println(p1);
           // System.out.println(new RationalPolynomial(coef, 0));
           // System.out.println(array.get(i));
            
            
        }
        //System.out.println("x^n = " + p);
        //p=p.truncate(n);
        RationalPolynomial p2 = RationalPolynomial.ZERO;
        if(m==2) l=l.mult(l);
        if (s==0) p2=l; else p2=l.times(p);//.mult(p);
        RationalPolynomial p3 = RationalPolynomial.ZERO;
        for (int i = 1; i < N; i++) {
        	//System.out.println(p1.evaluate(1));
            BigRational[] coef = p2.getCoef();
            RationalPolynomial term = new RationalPolynomial(coef[i], 0).times(array.get(i-1));
            
            p3 = p3.plus(term);
           // System.out.println(p1);
           // System.out.println(new RationalPolynomial(coef, 0));
           // System.out.println(array.get(i));
            //p1=p1.differentiate();
            
        }
        //System.out.println("X^n log(x) = " + p2);
        //System.out.println("X^n log(x) = " + p3);
        return p3;
    	
    }
    public static double[] toArray(int n, List<List<Object>> s) {
    	double[] s1 = s.stream()
    	    	.map(i->i.get(n))
    	    	.mapToDouble(num -> Double.parseDouble(num.toString())).toArray();
    	    	//.collect(toList());
		return s1;
    }
    public static DoubleMatrix UDFtoMatrix(int size, int m,int n, double h) {
    	double[] r= UFDWeights(m,n,0).getCoefd();
    	double[] mid= UFDWeights(m,n,1).getCoefd();
    	int s;
    	if (m==2) s=3; else s=2;
    	double[] b= UFDWeights(m,n,s).getCoefd();
    	
    	double[][] ma = new double[size][size];
        
    	for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
             if (i==0 && j<r.length) ma[i][j]=r[j]; //First row 
             else if (i==(size-1) && j>=i-(m+1)) ma[i][j]=b[j-i+(m+1)]; //Last row
             else if (i>0 && i<(size-1) && j>=i-1 && j<=i+1)  ma[i][j]=mid[j-i+1]; //In between
            }
          }
		return DoubleMatrix.copyOf(ma).multipliedBy(1/Math.pow(h,m));
    }
    public static SimpleMatrix UDFtoMatrixS(int size, int m,int n, double h) {
    	double[] r= UFDWeights(m,n,0).getCoefd();
    	double[] mid= UFDWeights(m,n,1).getCoefd();
    	int s;
    	DMatrixRMaj d = new DMatrixRMaj() ;
    	
    	if (m==2) s=3; else s=2;
    	double[] b= UFDWeights(m,n,s).getCoefd();
    	
    	double[][] ma = new double[size][size];
        
    	for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
             if (i==0 && j<r.length) ma[i][j]=r[j]; //First row 
             else if (i==(size-1) && j>=i-(m+1)) ma[i][j]=b[j-i+(m+1)]; //Last row
             else if (i>0 && i<(size-1) && j>=i-1 && j<=i+1)  ma[i][j]=mid[j-i+1]; //In between
            }
          }
    	d.set(ma);
		return SimpleMatrix.wrap(d).scale(1/Math.pow(h,m));
    }
    public static void main(String[] args) {
     test();
     
    }
    public static void testM() {
        test();
        DMatrixSparseCSC a = new DMatrixSparseCSC(1000,1000,0);
  	  DMatrixSparseCSC b = new DMatrixSparseCSC(1000,1000,0);
  	DMatrixSparseCSC c = new DMatrixSparseCSC(1000,1000,0);
  	  a.set(1,2,3);
  	  CommonOps_DSCC.mult(a,b,c);
       }
    
    public static void test() { 
    	long start = System.currentTimeMillis();
    	double qx=0.0029265 ;
    	double Lapse=0.02;
    	double cost =100;
    	double a0=0.001258720889208218;
    	double b0=0.00013;
    	double s0=0.00349;
    	double GK=4000;
    	
    	//MatrixAlgebra ALGEBRA = new CommonsMatrixAlgebra();
        // range grid
    	
    	Object[] g1 = IntStream.range(-3, 4).mapToDouble(i -> i*0.01).boxed().toArray();
    	Object[] g2 = IntStream.range(30/5, 65/5).mapToDouble(i -> i*5).boxed().toArray();
    	
    	List<List<Object>> s = Lists.cartesianProduct(Arrays.asList(g1),Arrays.asList(g2));
    	//List<List<Object>> ss = Lists.cartesianProduct(Arrays.asList(g1),Arrays.asList(g2),Arrays.asList(g2));
    	//s vectors
    	
    	DoubleArray s2 = DoubleArray.copyOf(toArray(0,s));
    	DoubleArray s3 = DoubleArray.copyOf(toArray(1,s)).multipliedBy(GK);
    	
    	
    	
    	SimpleMatrix s2S = new SimpleMatrix(g1.length*g2.length, 1,true, s2.toArray()); 
    	SimpleMatrix s3S = new SimpleMatrix(g1.length*g2.length, 1,true, s3.toArray());
    	// Differentiation matrices
    	//UFDWeights(2,3,3).getCoefd();
    	//DoubleMatrix ds2 = DoubleMatrix.identity(g1.length);
    	//DoubleMatrix ds3 = DoubleMatrix.identity(g2.length);
    	
    	//DoubleMatrix s2DerivFirst = UDFtoMatrix(size,2,3);
    	
    	
    	//DoubleMatrix s2DerivFirst = (DoubleMatrix) ALGEBRA.kroneckerProduct(UDFtoMatrix(g1.length,1,2,0.01),ds3);
    	//DoubleMatrix s2DerivSec = (DoubleMatrix) ALGEBRA.kroneckerProduct(UDFtoMatrix(g1.length,2,3,0.01),ds3);
    	//DoubleMatrix s3DerivFirst = (DoubleMatrix) ALGEBRA.kroneckerProduct(ds2,UDFtoMatrix(g2.length,1,2,500));
    	
    	
    	
    	//SimpleMatrix s3DerivFirstS = ds2S.kron(UDFtoMatrixS(g2.length,1,2,500));
    	
    	SimpleMatrix ds2S = SimpleMatrix.identity(g1.length);
    	SimpleMatrix ds3S= SimpleMatrix.identity(g2.length);
    	SimpleMatrix s3DerivFirstS = ds2S.kron(UDFtoMatrixS(g2.length,1,2,500));
    	SimpleMatrix s2DerivSecS = UDFtoMatrixS(g1.length,2,3,0.01).kron(ds3S);
    	SimpleMatrix s2DerivFirstS = UDFtoMatrixS(g1.length,1,2,0.01).kron(ds3S);

    	//System.out.println(s3);
    	//A
    	//DoubleMatrix d = DoubleMatrix.identity(g1.length*g2.length);
    	SimpleMatrix dS= SimpleMatrix.identity(g1.length*g2.length);
    	DoubleArray t = s2.plus(.9*qx+Lapse);
    	SimpleMatrix p1S = SimpleMatrix.diag(t.toArray());
    	p1S.convertToSparse();
    	//DoubleMatrix t1 = DoubleMatrix.diagonal(s3.multipliedBy(s2.plus(-.015-.8*qx)));
    	SimpleMatrix t1S = SimpleMatrix.diag(s3.multipliedBy(s2.plus(-.015-.8*qx)).toArray());
//    	//DoubleMatrix p2 = (DoubleMatrix) ALGEBRA.multiply(t1,s3DerivFirst);
    	s3DerivFirstS.convertToSparse();
    	t1S.convertToSparse();
    	SimpleMatrix p2S=t1S.mult(s3DerivFirstS);
    	
    	SimpleMatrix p3S = s2DerivSecS.scale(.5*s0*s0);
    	
    	SimpleMatrix t2S = SimpleMatrix.diag(s2.multipliedBy(b0).plus(a0).toArray());
    	//DoubleMatrix t2 = DoubleMatrix.diagonal(s2.multipliedBy(b0).plus(a0));
    	s2DerivFirstS.convertToSparse();
    	t2S.convertToSparse();
    	SimpleMatrix p4S = s2DerivFirstS.mult(t2S);
    	//DoubleMatrix p4 = (DoubleMatrix) ALGEBRA.multiply(s2DerivFirst,t2);
//    	//b
//    	//DoubleArray bt1 = s3.multipliedBy(-Lapse).minus(cost);
//    	
//    	//Solve
    	
    	int steps = 20;
//    	DoubleArray y = DoubleArray.copyOf(s3.stream().map(i->Math.max(0,i-GK)).toArray()).plus(GK);
    	SimpleMatrix yS = new SimpleMatrix(s3.size(),1,true,s3.stream().map(i->Math.max(0,i-GK)).toArray()).plus(GK);
    	//double[][] temp =new double[yS.numRows()][steps];
    	p2S.convertToSparse();
    	p3S.convertToSparse();
    	p4S.convertToSparse();
    	dS.convertToSparse();
    	long start1 = System.currentTimeMillis();
    	for (int i = 1; i < steps+1; i++) {
    	
    		SimpleMatrix y1 = p1S.minus(p2S).minus(p3S).minus(p4S).minus(dS).mult(yS).plus(s3S.scale(-Lapse).minus(cost*Math.pow(1.01, steps-i)));
    		yS=y1.scale(-1);
    	
    	}
//    	for (int i = 1; i < steps+1; i++) {
//        	
//    		SimpleMatrix y1 = p1S.minus(p2S).minus(p3S).minus(p4S).minus(dS).transpose().mult(yS).plus(s3S.scale(-Lapse).minus(cost*Math.pow(1.01, steps-i)));
//    		yS=y1.scale(-1);
//    	
//    	}
    	long end = System.currentTimeMillis();
    	System.out.println(yS.get(26));
    	System.out.println(end-start1);    	
//    	For[te=1,te<(steps+1),te++,
//    			{y1=(First[A[[te]]]-First@d).y +b[[te]]  ;
//    			y=-y1;AppendTo[temp,y1];}];
    	//
    	//
    	//DoubleMatrix s1 = DoubleMatrix.of(s2.length,1,s2);
    	//s1*d;
    	//ygrid= Range[-.03,.03,0.01];sgrid= Range[3000,6000,500]

    	//Lapse1 First@d+.9*qx[[K+1-t]]First@d+s2 p  First@d-First[sd]((-0.015-0.8*qx[[K+1-t]])s3+s3* p s2)- First[rd2].5s0^2-First[rd](a0-b0 p s2 )/.{a0->0.001258720889208218,b0->0.00013,s0->0.00349}
    	//b=Table[-Lapse1 s3-Co1 1.01^(K+1-t) , {t,0,K}];
    	//ALGEBRA.multiply(d,s1);
//    	for (int i = 1; i < 1; i++) {
//    		//System.out.println("hey");
//    	DoubleMatrix m2 = (DoubleMatrix) ALGEBRA.kroneckerProduct(DoubleMatrix.copyOf(m), DoubleMatrix.copyOf(m));
//    	}
    	
    	
    
    	        

   }

}