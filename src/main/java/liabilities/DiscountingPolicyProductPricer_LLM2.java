package liabilities;

import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

//import org.ejml.dense.row.Nullable;
import org.ejml.simple.SimpleMatrix;

import com.google.common.collect.Lists;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.RateComputation;

import product.ResolvedPolicy;
import utilities.ForwardFun;
import utilities.Taylor;

/**
 * Pricer for for forward rate agreement (FRA) products.
 * <p>
 * This provides the ability to price {@link ResolvedPolicy}.
 * The product is priced using a forward curve for the index.
 */
public class DiscountingPolicyProductPricer_LLM2 {

  /**
   * Default implementation.
   */
  public static final DiscountingPolicyProductPricer_LLM2 DEFAULT = new DiscountingPolicyProductPricer_LLM2(
      RateComputationFn.standard());

  /**
   * Rate computation.
   */
  private final RateComputationFn<RateComputation> rateComputationFn;
  
  private SimpleMatrix y;
  private SimpleMatrix yAdj;
  private double dt;


  /**
   * Creates an instance.
   * 
   * @param rateComputationFn  the rate computation function
   */
  public DiscountingPolicyProductPricer_LLM2(
      RateComputationFn<RateComputation> rateComputationFn) {
    this.rateComputationFn = ArgChecker.notNull(rateComputationFn, "rateComputationFn");
    this.y = null;
    this.yAdj = null;
    this.dt=1;
    
   
	
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the FRA product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * This is the discounted forecast value.
   * 
   * @param resolvedPolicy  the product
   * @param provider  the rates provider
   * @return the present value of the product
   */
  public Pair<SimpleMatrix, SimpleMatrix> diffMatDet0(ResolvedPolicy resolvedPolicy, RatesProvider provider, ReferenceData refData ) {
		
	  	Optional<Surface> qxS = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getMale() ? "AAG-M" : "AAG-V")).findData(SurfaceName.of(resolvedPolicy.getMale() ? "AAG-man" : "AAG-vrouw")) ;
	  	Optional<Surface> LapseS = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getMale() ? "VERVAL_UL_2019M" : "VERVAL_UL_2019V")).findData(SurfaceName.of(resolvedPolicy.getMale() ? "verv-man" : "verv-vrouw")) ;
	  	Optional<Surface> porfS = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getMale() ? "PORT-M" : "PORT-V")).findData(SurfaceName.of(resolvedPolicy.getMale() ? "portf-man" : "portf-vrouw")) ;
	  	Optional<Surface> tarif = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getTarifId())).findData(SurfaceName.of(resolvedPolicy.getTarifId())) ;
	  
	  	//this.dt=provider.findData(TestId.of("EUR-CPI")).get();
	  	Curve curve = provider.findData(CurveName.of("EUR-CPI")).get();
	  	Curve fwdCurve = provider.findData(CurveName.of("ESG")).get();
	  	int exp = Period.between(provider.getValuationDate(),resolvedPolicy.getExpiryDate()).getYears();
	  	int expM = Period.between(provider.getValuationDate(),resolvedPolicy.getExpiryDate()).getMonths();
	  	int age = Period.between(resolvedPolicy.getBirthDate(),provider.getValuationDate()).getYears();
	  	int valYear = provider.getValuationDate().getYear();
	  
	  	double steps = exp + dt*Math.floor(expM/(12*dt));
	 	
	  	double costF= 16.11;
	  	double dt = provider.data(NonObservableId.of("TimeStep"));
//	  	
	  	double y0= 0;
	  	double mr=resolvedPolicy.getMortalityRestitution();
	  	double gr = resolvedPolicy.getRateInvestementAccountGuaranteed();
	  	// Two types of policies
	  	if (gr!=0){
	  		y0=resolvedPolicy.getInvestementAccountProxy();
	  	for (double i = 0; i < steps; i=i+dt) {
		  		double tar = tarif.get().zValue(age+i, valYear+i);
		  		y0=(tar*(1-mr/100)+gr*(1+tar))*y0*dt +y0;
		  
		  	}
	  	};
	  	double GK=y0;//172
	  	//GK=10000d/2;
	  	//double factor = resolvedPolicy.getInvestementAccountGuaranteed()>0 ? resolvedPolicy.getInvestementAccount()/resolvedPolicy.getInvestementAccountGuaranteed():1;
	  	double terbeh = resolvedPolicy.getExpenseRateinvestementAccount();
	  	if (steps==0) return Pair.of(SimpleMatrix.diag(0),SimpleMatrix.diag(0));
	  	y0=resolvedPolicy.getInvestementAccount();
	   	double[] fv = new double[(int) (steps/dt)]; 
	  	for (double i = 0; i < steps; i=i+dt) {
	  		double qx = qxS.get().zValue(age+i, valYear+i);
	  		double fwd1= fwdCurve.yValue(i);
	  		double tar = tarif.get().zValue(age+i, valYear+i);
	  		fv[(int) (i/dt)]=y0;
	  		y0=(-terbeh -tar*qx + fwd1)*y0*dt +y0;
	  
	  	}
	  	
	  	y0=GK + Math.max(0,fv[(int) (steps/dt)-1]-GK);
		for (double i = 0; i < steps; i=i+dt) {
	  		double  t=steps-i; 
	  		double qx = qxS.get().zValue(age+t, valYear+t);
	  		double lapse = LapseS.get().zValue(age+t, valYear+t)/100;
	  		double portfCorr = porfS.get().zValue(age+t, valYear+t)/100;
	  		double fwd1= fwdCurve.yValue(t);
	  		double inflation = curve.yValue(t)/100;
	  		y0=(((fwd1 + portfCorr*qx + lapse)*y0 -lapse*fv[(int) (t/dt)-1]-costF*inflation)*-dt+y0)*1;
	  
	  	}
		
	    //return CurrencyAmount.of(Currency.EUR, this.y.cols(steps-1, steps).get(171));//indexOf
		 	return Pair.of(SimpleMatrix.diag(-y0),SimpleMatrix.diag(0));//;,CurrencyAmount.of(Currency.EUR, yS.get(ind)));
		 	//return CurrencyAmount.of(Currency.EUR, yS.get(ind));//indexOf
		 	//return CurrencyAmount.of(Currency.EUR, end-start1);//indexOf
	  }
  public Pair<SimpleMatrix, SimpleMatrix> diffMatDet(ResolvedPolicy resolvedPolicy, RatesProvider provider, ReferenceData refData ) {
		
	  	Optional<Surface> qxS = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getMale() ? "AAG-M" : "AAG-V")).findData(SurfaceName.of(resolvedPolicy.getMale() ? "AAG-man" : "AAG-vrouw")) ;
	  	Optional<Surface> LapseS = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getMale() ? "VERVAL_UL_2019M" : "VERVAL_UL_2019V")).findData(SurfaceName.of(resolvedPolicy.getMale() ? "verv-man" : "verv-vrouw")) ;
	  	Optional<Surface> porfS = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getMale() ? "PORT-M" : "PORT-V")).findData(SurfaceName.of(resolvedPolicy.getMale() ? "portf-man" : "portf-vrouw")) ;
	  	Optional<Surface> tarif = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getTarifId())).findData(SurfaceName.of(resolvedPolicy.getTarifId())) ;
	  
	  	//this.dt=provider.findData(TestId.of("EUR-CPI")).get();
	  	Curve curve = provider.findData(CurveName.of("EUR-CPI")).get();
	  	Curve fwdCurve = provider.findData(CurveName.of("ESG")).get();
	  	int exp = Period.between(provider.getValuationDate(),resolvedPolicy.getExpiryDate()).getYears();
	  	int age = Period.between(resolvedPolicy.getBirthDate(),provider.getValuationDate()).getYears();
	  	int expM = Period.between(provider.getValuationDate(),resolvedPolicy.getExpiryDate()).getMonths();
	  	int valYear = provider.getValuationDate().getYear();
	  
	  	
	  	double steps = exp + dt*Math.floor(expM/(12*dt));
	  	//if (steps==0) return  null;//CurrencyAmount.of(Currency.EUR, 0);
	  	if (steps==0) return Pair.of(SimpleMatrix.diag(0),SimpleMatrix.diag(0));
	  	double costF= 16.11;
	  	double dt = provider.data(NonObservableId.of("TimeStep"));
//	  	
	  	double y0= 0;
	  	double mr=resolvedPolicy.getMortalityRestitution();
	  	double gr = resolvedPolicy.getRateInvestementAccountGuaranteed();
	  	// Two types of policies
	  	if (gr!=0){
	  		y0=resolvedPolicy.getInvestementAccountProxy();
	  		for (double i = 0; i < steps; i=i+dt) {
		  		double tar = tarif.get().zValue(age+i, valYear+i);
		  		y0=(tar*(1-mr/100)+gr*(1+tar))*y0*dt +y0;
	  		}
	  	};
	  	double GK=y0;//172
	  	//GK=10000d/2;
	  	//double factor = resolvedPolicy.getInvestementAccountGuaranteed()>0 ? resolvedPolicy.getInvestementAccount()/resolvedPolicy.getInvestementAccountGuaranteed():1;
	  	double terbeh = resolvedPolicy.getExpenseRateinvestementAccount();
	  	
	   	   	//int ind=4;//171
	   	 //double fwdCorr= 1.0e-4*0;
	  	double accVal= resolvedPolicy.getInvestementAccount();
	   	 double scaleVal=1d/accVal;
//	  		
	  		double[] g2 = IntStream.range(-2, 3).mapToDouble(i -> i*.1+1).toArray();
	  		//List<List<Object>> s = Lists.cartesianProduct(Arrays.asList(g2));
	  		DoubleArray s3 = DoubleArray.copyOf(g2).multipliedBy(accVal);
	  		SimpleMatrix s3S = new SimpleMatrix(g2.length, 1,true, s3.toArray());
	  		//SimpleMatrix s3ST = new SimpleMatrix(g2.length, 1,true, s3.toArray());
	  		SimpleMatrix s3DerivFirstS = Taylor.UDFtoMatrixS(g2.length,1,2,.1).scale(scaleVal);
	  		//SimpleMatrix s3DerivFirstST = Taylor.UDFtoMatrixS(g2.length,1,2,.1);
	  		SimpleMatrix dS= SimpleMatrix.identity(g2.length);
		 	SimpleMatrix yS = new SimpleMatrix(s3.size(),1,true,DoubleArray.copyOf(s3.stream().map(i-> GK + Math.max(0,i-GK)).toArray()).toArray());
		 	//yS=s3S;
		 	SimpleMatrix ySAdj = new SimpleMatrix(s3.size(),1);
		 	ySAdj.set((dS.numCols()-1)/2, 0, 1);
		 	SimpleMatrix fullY=yS;
		 	SimpleMatrix fullYadj=ySAdj;
		 	
		 	long start1 = System.currentTimeMillis();
		 	for (double i = (steps-dt); i > -1; i=i-dt) {
		 		double t=i;
		 		double t1=steps-t; 
		 		double qx = qxS.get().zValue(age+t, valYear+t);
		  		double lapse = LapseS.get().zValue(age+t, valYear+t)/100;
		  		double portfCorr = porfS.get().zValue(age+t, valYear+t)/100;
		  		
		  		double qxF = qxS.get().zValue(age+t1, valYear+t1);
		  		double lapseF = LapseS.get().zValue(age+t1, valYear+t1)/100;
		  		double portfCorrF = porfS.get().zValue(age+t1, valYear+t1)/100;
		  		double tar = tarif.get().zValue(age+i, valYear+i);
//		  		lapseF =0.007;
//		  		portfCorrF= 0.9;
//		  		qxF=0.007;
//		  		lapse =0.007;
//		  		portfCorr= 0.9;
//		  		qx=0.007;
		  		double inflation = curve.yValue(t)/100;
		  		double inflationF = curve.yValue(t1)/100;
		  		//inflation = Math.pow(1.01, i);
		  		//inflationF = Math.pow(1.01, t1);
		  		//double restitutie= (1-mr)/100;
		  		//int pos = (int) Math.floor(i);
		  		//double fwdCorr= fwd[(int) (i*dt)] + 1.0e-4*0 - Math.log(HullWhiteAnalytical.price(0d, Double.valueOf(i), fwd[0])/HullWhiteAnalytical.price(0d, Double.valueOf(i+1), fwd[0]));
		  		double fwd1= fwdCurve.yValue(t);
		  		double fwd1F= fwdCurve.yValue(t1);
//		  		fwd1F=0.01;
//		  		fwd1=0.01;
		  		//double test= modelForwardRate(fwd[0],10,a0,b0,s0);
		  		double ta = fwd1 + portfCorr*qx + lapse;
		  		double taF = fwd1F + portfCorrF*qxF + lapseF;
		  		SimpleMatrix p1S = dS.scale(ta);
		  		SimpleMatrix p1SF = dS.scale(taF);
			 	
			 	List<double[]> cop = Collections.nCopies(dS.numCols(), s3.multipliedBy(-terbeh -tar*qx + fwd1).toArray());
			 	double[] vals = cop.stream().flatMapToDouble(x -> Arrays.stream(x)).toArray();
			 	
			 	List<double[]> copF = Collections.nCopies(dS.numCols(), s3.multipliedBy(-terbeh -tar*qxF + fwd1F).toArray());
			 	double[] valsF = copF.stream().flatMapToDouble(x -> Arrays.stream(x)).toArray();
			 	
			 	SimpleMatrix sm1 = new SimpleMatrix(dS.numCols(), dS.numCols(), false, vals);
			 	SimpleMatrix sm1F = new SimpleMatrix(dS.numCols(), dS.numCols(), false, valsF);
			 	SimpleMatrix p2S1=s3DerivFirstS.elementMult(sm1);
			 	SimpleMatrix p2S1F=s3DerivFirstS.elementMult(sm1F);
//			 	SimpleMatrix tst = p1S.minus(p2S1).scale(dt);
//			 	SimpleMatrix tstb = s3S.scale(-lapse).minus(costF*inflation);
			 	SimpleMatrix tstc = p1SF.minus(p2S1F);
				SimpleMatrix y1 = p1S.minus(p2S1).scale(dt).minus(dS).mult(yS).plus(s3S.scale(-lapse).minus(costF*inflation).scale(dt));
		 		SimpleMatrix y1Adj = (p1SF.minus(p2S1F).scale(dt)).transpose().minus(dS).mult(ySAdj);
		 		yS=y1.scale(-1);
		 		ySAdj=y1Adj.scale(-1);
		 		fullY=fullY.concatColumns(y1)	;
		 		fullYadj=fullYadj.concatColumns(y1Adj);
		 		
		 		//yS.concatColumns(yS,y1);
		 		//DoubleArray.
		 	}
		 	//this.y=yS;
		 	
	  	//}
		 	//resolvedPolicy.toBuilder().set("tarifId", "300").build();
		 	long end = System.currentTimeMillis();
		 	 
	    //return CurrencyAmount.of(Currency.EUR, this.y.cols(steps-1, steps).get(171));//indexOf
		 	return Pair.of(fullY,fullYadj);//;,CurrencyAmount.of(Currency.EUR, yS.get(ind)));
		 	//return CurrencyAmount.of(Currency.EUR, yS.get(ind));//indexOf
		 	//return CurrencyAmount.of(Currency.EUR, end-start1);//indexOf
	  }
	  
//  public static SimpleMatrix elemWise( SimpleMatrix A, SimpleMatrix B) {
//      int numColsC = A.numCols()*B.numCols;
//      int numRowsC = A.numRows()*B.numRows;
//      SimpleMatrix C = new SimpleMatrix(A.numCols(), B.numRows());
//      for (int i = 0; i < A.numCols(); i++) {
//          
//              for (int rowB = 0; rowB < B.numRows(); rowB++) {
//                  for (int colB = 0; colB < B.numCols; colB++) {
//                      double val = a*B.get(rowB, colB);
//                      C.unsafe_set(i*B.numRows + rowB, j*B.numCols + colB, val);
//                  }
//              }
//          
//      }
//
//      return C;
//  }

  public Pair<SimpleMatrix, SimpleMatrix> diffMat(ResolvedPolicy resolvedPolicy, RatesProvider provider, ReferenceData refData ) {
	
	  Optional<Surface> qxS = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getMale() ? "AAG-M" : "AAG-V")).findData(SurfaceName.of(resolvedPolicy.getMale() ? "AAG-man" : "AAG-vrouw")) ;
	  	Optional<Surface> LapseS = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getMale() ? "VERVAL_UL_2019M" : "VERVAL_UL_2019V")).findData(SurfaceName.of(resolvedPolicy.getMale() ? "verv-man" : "verv-vrouw")) ;
	  	Optional<Surface> porfS = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getMale() ? "PORT-M" : "PORT-V")).findData(SurfaceName.of(resolvedPolicy.getMale() ? "portf-man" : "portf-vrouw")) ;
	  	Optional<Surface> tarif = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getTarifId())).findData(SurfaceName.of(resolvedPolicy.getTarifId())) ;
	  
	  	//this.dt=provider.findData(TestId.of("EUR-CPI")).get();
	  	Curve curve = provider.findData(CurveName.of("EUR-CPI")).get();
	  	Curve fwdCurve = provider.findData(CurveName.of("ESG")).get();
	  	int exp = Period.between(provider.getValuationDate(),resolvedPolicy.getExpiryDate()).getYears();
	  	int age = Period.between(resolvedPolicy.getBirthDate(),provider.getValuationDate()).getYears();
	  	int expM = Period.between(provider.getValuationDate(),resolvedPolicy.getExpiryDate()).getMonths();
	  	int valYear = provider.getValuationDate().getYear();
	  
	  	
	  	double steps = exp + dt*Math.floor(expM/(12*dt));
	  	//if (steps==0) return  null;//CurrencyAmount.of(Currency.EUR, 0);
	  	if (steps==0) return Pair.of(SimpleMatrix.diag(0),SimpleMatrix.diag(0));
	  	double costF= 16.11;
	  	double dt = provider.data(NonObservableId.of("TimeStep"));
//	  	
	  	double y0= 0;
	  	double mr=resolvedPolicy.getMortalityRestitution();
	  	double gr = resolvedPolicy.getRateInvestementAccountGuaranteed();
	  	// Two types of policies
	  	if (gr!=0){
	  		y0=resolvedPolicy.getInvestementAccountProxy();
	  		for (double i = 0; i < steps; i=i+dt) {
		  		double tar = tarif.get().zValue(age+i, valYear+i);
		  		y0=(tar*(1-mr/100)+gr*(1+tar))*y0*dt +y0;
	  		}
	  	};
	  	double GK=y0;//172
	  	//GK=10000d/2;
	  	//double factor = resolvedPolicy.getInvestementAccountGuaranteed()>0 ? resolvedPolicy.getInvestementAccount()/resolvedPolicy.getInvestementAccountGuaranteed():1;
	  	double terbeh = resolvedPolicy.getExpenseRateinvestementAccount();
	  	
	   	   	//int ind=4;//171
	   	 //double fwdCorr= 1.0e-4*0;
	  	double accVal= resolvedPolicy.getInvestementAccount();
	   	 double scaleVal=1d/accVal;
  	
  		
  		//HUll WHite pars
  		double a0=0.001258720889208218;
   	   	double b0=0.00013;
   	   	double s0=0.00349;
   	   	double di=0.0054;
   	   	int ind=12;//171
   	 //double fwdCorr= 1.0e-4*0;
   	 
  		SimpleMatrix p3S = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "p3S")).getDifferenationMatrix() ;
  		//SimpleMatrix p4S = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "p4S")).getDifferenationMatrix() ;
  		SimpleMatrix s2S = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "s2S")).getDifferenationMatrix() ;
  		SimpleMatrix s3S = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "s3S")).getDifferenationMatrix().scale(scaleVal);
  		//SimpleMatrix s4S = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "s4S")).getDifferenationMatrix().scale(resolvedPolicy.getInvestementAccount());
  		//DoubleArray s4 = Taylor.toArray(s4S) ;
  		DoubleArray s3 = Taylor.toArray(s3S) ;
		DoubleArray s2 = Taylor.toArray(s2S) ;
		
		SimpleMatrix s3DerivFirstS = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "s3DerivFirstS")).getDifferenationMatrix().scale(scaleVal) ;
  		//SimpleMatrix s4DerivFirstS = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "s4DerivFirstS")).getDifferenationMatrix() ;
  		SimpleMatrix dS = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "dS")).getDifferenationMatrix();
//  		
  		
	 	SimpleMatrix yS = new SimpleMatrix(s3.size(),1,true,DoubleArray.copyOf(s3.stream().map(i-> GK + Math.max(0,i-GK)).toArray()).toArray());
	 	//yS=s3S;
	 	SimpleMatrix ySAdj = new SimpleMatrix(s3.size(),1);
	 	ySAdj.set(ind, 0, 1);
	 	SimpleMatrix fullY=yS;
	 	SimpleMatrix fullYadj=ySAdj;
	 	
	 	//double[][] temp =new double[yS.numRows()][steps];
	 	//ForwardFun ff = new ForwardFun(0.0054);
	 	
	 	long start1 = System.currentTimeMillis();
	 	for (double i = steps; i > -0; i=i-dt) {
	 		double t=i;
	 		double t1=steps-t; 
	 		double qx = qxS.get().zValue(age+t, valYear+t);
	  		double lapse = LapseS.get().zValue(age+t, valYear+t)/100;
	  		double portfCorr = porfS.get().zValue(age+t, valYear+t)/100;
	  		
	  		double qxF = qxS.get().zValue(age+t1, valYear+t1);
	  		double lapseF = LapseS.get().zValue(age+t1, valYear+t1)/100;
	  		double portfCorrF = porfS.get().zValue(age+t1, valYear+t1)/100;
	  		
//	  		lapseF =0.007;
//	  		portfCorrF= 0.9;
//	  		qxF=0.007;
//	  		lapse =0.007;
//	  		portfCorr= 0.9;
//	  		qx=0.007;
	  		double inflation = curve.yValue(t)/100;
	  		double inflationF = curve.yValue(t1)/100;
	  		//inflation = Math.pow(1.01, i);
	  		inflationF = Math.pow(1.01, t1);
	  		//double restitutie= (1-mr)/100;
	  		//int pos = (int) Math.floor(i);
	  		//double fwdCorr= fwd[(int) (i*dt)] + 1.0e-4*0 - Math.log(HullWhiteAnalytical.price(0d, Double.valueOf(i), fwd[0])/HullWhiteAnalytical.price(0d, Double.valueOf(i+1), fwd[0]));
	  		double fwd1= fwdCurve.yValue(t);
	  		double fwd1F= fwdCurve.yValue(t1);
	  		//double fwd2 = ForwardFun.rate(0d, fwd1, volsCurve.yValue(t));
	  		//Market model
	  		//double[] sdA = s2.stream().map(d->ForwardFun.rate(d, fwd1, volsCurve.yValue(t))).toArray();
	  		//DoubleArray s2M= DoubleArray.copyOf(sdA);
//	  		fwd1F=0.01;
	  		//fwd1=0.01;
	  		//double test= modelForwardRate(fwd[0],10,a0,b0,s0);
	  		//double ta = fwd1 + portfCorr*qx + lapse;
	  		DoubleArray taF = s2M.plus(0*fwd1F + portfCorrF*qxF + lapse);
	  		DoubleArray ta = s2M.plus(0*fwd1 + portfCorr*qx+lapse);	
	  		SimpleMatrix p1S = SimpleMatrix.diag(ta.toArray());
	  		//SimpleMatrix p1S = dS.scale(ta);
	  		SimpleMatrix p1SF = SimpleMatrix.diag(taF.toArray());;
		 	
		 	List<double[]> cop = Collections.nCopies(dS.numCols(), s3.multipliedBy(s2M.plus(-terbeh -0.8*qx + 0*fwd1)).toArray());
		 	double[] vals = cop.stream().flatMapToDouble(x -> Arrays.stream(x)).toArray();
		 	
		 	List<double[]> copF = Collections.nCopies(dS.numCols(), s3.multipliedBy(s2M.plus(-terbeh -0.8*qx + fwd1F)).toArray());
		 	double[] valsF = copF.stream().flatMapToDouble(x -> Arrays.stream(x)).toArray();
		 	
		 	SimpleMatrix sm1 = new SimpleMatrix(dS.numCols(), dS.numCols(), false, vals);
		 	SimpleMatrix sm1F = new SimpleMatrix(dS.numCols(), dS.numCols(), false, valsF);
		 	SimpleMatrix p2S=s3DerivFirstS.elementMult(sm1);
		 	SimpleMatrix p2SF=s3DerivFirstS.elementMult(sm1F);
		 	
	 		SimpleMatrix y1 = p1S.minus(p2S).minus(p3S).scale(dt).minus(dS).mult(yS).plus(s3S.scale(-lapse).minus(costF*inflation).scale(dt));;
	 		SimpleMatrix y1Adj = p1SF.minus(p2SF).minus(p3S).transpose().scale(dt).minus(dS).mult(ySAdj);
	 		yS=y1.scale(-1);
	 		ySAdj=y1Adj.scale(-1);
	 		fullY=fullY.concatColumns(y1)	;
	 		fullYadj=fullYadj.concatColumns(y1Adj);
	 		
	 		//yS.concatColumns(yS,y1);
	 		//DoubleArray.
	 	}
	 	//this.y=yS;
	 	
  	//}
	 	//resolvedPolicy.toBuilder().set("tarifId", "300").build();
	 	long end = System.currentTimeMillis();
	 	 
    //return CurrencyAmount.of(Currency.EUR, this.y.cols(steps-1, steps).get(171));//indexOf
	 	return Pair.of(fullY,fullYadj);//;,CurrencyAmount.of(Currency.EUR, yS.get(ind)));
	 	//return CurrencyAmount.of(Currency.EUR, yS.get(ind));//indexOf
	 	//return CurrencyAmount.of(Currency.EUR, end-start1);//indexOf
  }
  
  public double modelForwardRate(double initialRate,double t, double a,double b,double s  ) {
	  return -(Math.exp(-b*t)-1)*(a*b -s*s/2)/(b*b)+s*s/(2*b*b)*Math.exp(-b*t)*(1-Math.exp(-b*t))+initialRate*Math.exp(-b*t);
  }
  
  /**
   * Calculates the present value sensitivity of the FRA product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param fra  the product
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivityBuilder presentValueSensitivity(ResolvedPolicy resolvedPolicy, RatesProvider provider,ReferenceData refData, Pair<SimpleMatrix, SimpleMatrix> diffMat) {
	  double dt = provider.data(NonObservableId.of("TimeStep"));
	  if (diffMat.getSecond().numRows()==1) return PointSensitivityBuilder.none();
	  //int steps = Period.between(provider.getValuationDate(),resolvedPolicy.getExpiryDate()).getYears();
	  //DiscountFactors discountFactors = provider.discountFactors(Currency.EUR); 
	  double GK = diffMat.getFirst().get(0,0);
	  double accVal= resolvedPolicy.getInvestementAccount();
	  double scaleVal=1d/accVal;//resolvedPolicy.getInvestementAccount()

	  double[] g2 = IntStream.range(-2, 3).mapToDouble(i -> i*.1+1).toArray();
	  SimpleMatrix dS= SimpleMatrix.identity(g2.length);
	  DoubleArray s3 = DoubleArray.copyOf(g2).multipliedBy(accVal);
	  //SimpleMatrix s3S = new SimpleMatrix(g2.length, 1,true, s3.toArray());
	  SimpleMatrix s3DerivFirstS = Taylor.UDFtoMatrixS(g2.length,1,2,.1).scale(scaleVal);
	  //stoch interest rate	
	  if (diffMat.getSecond().numRows()>5) {
		   s3DerivFirstS = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "s3DerivFirstS")).getDifferenationMatrix().scale(scaleVal) ;
		   dS = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "dS")).getDifferenationMatrix();
		   SimpleMatrix s3S = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "s3S")).getDifferenationMatrix().scale(accVal);
		   s3 = Taylor.toArray(s3S) ;	
	  }
  
	List<double[]> cop = Collections.nCopies(dS.numCols(), s3.toArray());
	 double[] vals = cop.stream().flatMapToDouble(x -> Arrays.stream(x)).toArray();
	 	
	 SimpleMatrix sm1 = new SimpleMatrix(dS.numCols(), dS.numCols(), false, vals);
	 	
	 SimpleMatrix p2S=s3DerivFirstS.elementMult(sm1);
	  //SimpleMatrix p2S=t2S.mult(s3DerivFirstS);
	  //p2S.convertToSparse();
	  SimpleMatrix fwdDer = dS.minus(p2S);
	  //fwdDer.convertToSparse();
	  PointSensitivityBuilder sens = PointSensitivityBuilder.none();
	  List<Double> l= new ArrayList<Double>();
	  List<IborRateSensitivity> ps=Lists.newArrayList();
	  //DiscountFactors discountFactors = provider.discountFactors(Currency.EUR);
	  double prevTmp=0;
	  double tmp=0;
	  int totalSteps= diffMat.getSecond().numCols() ;
	  for (int i = 0; i < (totalSteps); i++) {
		  prevTmp=tmp;
		  tmp = diffMat.getSecond().cols(totalSteps-i-1, totalSteps-i).transpose().mult(fwdDer).mult(diffMat.getFirst().cols(i, i+1)).get(0,0);//*(-i/(Math.log(discountFactors.discountFactor(i+1))));
		  //sensTemp=sensTemp.concatColumns(tmp);
		  l.add(tmp);
		  //ZeroRateSensitivity zrsStart1 = ZeroRateSensitivity.of(Currency.EUR, i, tmp);
        IborIndexObservation obs = IborIndexObservation.of(IborIndices.EUR_LIBOR_12M, provider.getValuationDate().plusMonths((long) (i*12*dt)).minusDays(4), refData);
        ps.add(IborRateSensitivity.of(obs, Currency.EUR,  -tmp));
//	
	  }
	  
//    if (fra.getPaymentDate().isBefore(provider.getValuationDate())) {
//      return PointSensitivities.empty();
//    }
   //DiscountFactors discountFactors = provider.discountFactors(Currency.EUR);
//    double df = discountFactors.discountFactor(fra.getPaymentDate());
//    double notional = fra.getNotional();
//    double unitAmount = unitAmount(fra, provider);
//    double derivative = derivative(fra, provider);
      //PointSensitivityBuilder discSens = discountFactors.zeroRatePointSensitivity(provider.getValuationDate());
//        .multipliedBy(unitAmount * notional);
	double sum = l.stream().mapToDouble(f -> f.doubleValue()/10000).sum()*dt;
	//System.out.println(sum);
    return PointSensitivityBuilder.of(ps);
  }

//  //-------------------------------------------------------------------------
//  /**
//   * Calculates the forecast value of the FRA product.
//   * <p>
//   * The forecast value of the product is the value on the valuation date without present value discounting.
//   * 
//   * @param fra  the product
//   * @param provider  the rates provider
//   * @return the forecast value of the product
//   */
  public CurrencyAmount presentValue(ResolvedPolicy fra, RatesProvider provider, ReferenceData refData, Pair<SimpleMatrix, SimpleMatrix> diffMat) {
    //double fv = forecastValue0(fra, provider);
	  int col = diffMat.getFirst().numCols();
	  int ind = (diffMat.getFirst().numRows()-1)/2;
	  return CurrencyAmount.of(Currency.EUR, diffMat.getFirst().cols(col-1,col).get(ind)*-1);
  }
//
//  /**
//   * Calculates the forecast value sensitivity of the FRA product.
//   * <p>
//   * The forecast value sensitivity of the product is the sensitivity of the forecast value to
//   * the underlying curves.
//   * 
//   * @param fra  the product
//   * @param provider  the rates provider
//   * @return the point sensitivity of the forecast value
//   */
//  public PointSensitivities forecastValueSensitivity(ResolvedPolicy fra, RatesProvider provider) {
//  
//	  //IborIndexRates rates = provider.iborIndexRates(computation.getIndex());
//	    return rates.ratePointSensitivity(computation.getObservation());
//  }
//
//  //-------------------------------------------------------------------------
//  /**
//   * Calculates the par rate of the FRA product.
//   * <p>
//   * The par rate is the rate for which the FRA present value is 0.
//   * 
//   * @param fra  the product
//   * @param provider  the rates provider
//   * @return the par rate
//   */
//  public double parRate(ResolvedPolicy fra, RatesProvider provider) {
//    return forwardRate(fra, provider);
//  }
//
//  /**
//   * Calculates the par rate curve sensitivity of the FRA product.
//   * <p>
//   * The par rate curve sensitivity of the product is the sensitivity of the par rate to
//   * the underlying curves.
//   * 
//   * @param fra  the product
//   * @param provider  the rates provider
//   * @return the par rate sensitivity
//   */
//  public PointSensitivities parRateSensitivity(ResolvedPolicy fra, RatesProvider provider) {
//    return forwardRateSensitivity(fra, provider).build();
//  }
//
//  /**
//   * Calculates the par spread of the FRA product.
//   * <p>
//   * This is spread to be added to the fixed rate to have a present value of 0.
//   * 
//   * @param fra  the product
//   * @param provider  the rates provider
//   * @return the par spread
//   */
//  public double parSpread(ResolvedPolicy fra, RatesProvider provider) {
//    double forward = forwardRate(fra, provider);
//    return forward - fra.getFixedRate();
//  }
//
//  /**
//   * Calculates the par spread curve sensitivity of the FRA product.
//   * <p>
//   * The par spread curve sensitivity of the product is the sensitivity of the par spread to
//   * the underlying curves.
//   * 
//   * @param fra  the product
//   * @param provider  the rates provider
//   * @return the par spread sensitivity
//   */
//  public PointSensitivities parSpreadSensitivity(ResolvedPolicy fra, RatesProvider provider) {
//    return forwardRateSensitivity(fra, provider).build();
//  }
//
//  //-------------------------------------------------------------------------
//  /**
//   * Calculates the future cash flow of the FRA product.
//   * <p>
//   * There is only one cash flow on the payment date for the FRA product.
//   * The expected currency amount of the cash flow is the same as {@link #forecastValue(ResolvedPolicy, RatesProvider)}.
//   * 
//   * @param fra  the product
//   * @param provider  the rates provider
//   * @return the cash flows
//   */
//  public CashFlows cashFlows(ResolvedPolicy fra, RatesProvider provider) {
//    LocalDate paymentDate = fra.getPaymentDate();
//    double forecastValue = forecastValue0(fra, provider);
//    double df = provider.discountFactor(fra.getCurrency(), paymentDate);
//    CashFlow cashFlow = CashFlow.ofForecastValue(paymentDate, fra.getCurrency(), forecastValue, df);
//    return CashFlows.of(cashFlow);
//  }
//
//  //-------------------------------------------------------------------------
//  /**
//   * Explains the present value of the FRA product.
//   * <p>
//   * This returns explanatory information about the calculation.
//   * 
//   * @param fra  the FRA product for which present value should be computed
//   * @param provider  the rates provider
//   * @return the explanatory information
//   */
//  public ExplainMap explainPresentValue(ResolvedPolicy fra, RatesProvider provider, ReferenceData refData) {
//    ExplainMapBuilder builder = ExplainMap.builder();
//    Currency currency = fra.getCurrency();
//    builder.put(ExplainKey.ENTRY_TYPE, "FRA");
//    builder.put(ExplainKey.PAYMENT_DATE, fra.getPaymentDate());
//    builder.put(ExplainKey.START_DATE, fra.getStartDate());
//    builder.put(ExplainKey.END_DATE, fra.getEndDate());
//    builder.put(ExplainKey.ACCRUAL_YEAR_FRACTION, fra.getYearFraction());
//    builder.put(ExplainKey.DAYS, (int) DAYS.between(fra.getStartDate(), fra.getEndDate()));
//    builder.put(ExplainKey.PAYMENT_CURRENCY, currency);
//    builder.put(ExplainKey.NOTIONAL, CurrencyAmount.of(currency, fra.getNotional()));
//    builder.put(ExplainKey.TRADE_NOTIONAL, CurrencyAmount.of(currency, fra.getNotional()));
//    if (fra.getPaymentDate().isBefore(provider.getValuationDate())) {
//      builder.put(ExplainKey.COMPLETED, Boolean.TRUE);
//      builder.put(ExplainKey.FORECAST_VALUE, CurrencyAmount.zero(currency));
//      builder.put(ExplainKey.PRESENT_VALUE, CurrencyAmount.zero(currency));
//    } else {
//      double rate = rateComputationFn.explainRate(
//          fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provider, builder);
//      builder.put(ExplainKey.FIXED_RATE, fra.getFixedRate());
//      builder.put(ExplainKey.DISCOUNT_FACTOR, provider.discountFactor(currency, fra.getPaymentDate()));
//      builder.put(ExplainKey.PAY_OFF_RATE, rate);
//      builder.put(ExplainKey.UNIT_AMOUNT, unitAmount(fra, provider));
//      builder.put(ExplainKey.FORECAST_VALUE, forecastValue(fra, provider));
//      builder.put(ExplainKey.PRESENT_VALUE, presentValue(fra, provider, refData));
//    }
//    return builder.build();
//  }
//
//  //-------------------------------------------------------------------------
//  // calculates the forecast value
//  private double forecastValue0(ResolvedPolicy resolvedPolicy, RatesProvider provider) {
//    if (resolvedPolicy.getPaymentDate().isBefore(provider.getValuationDate())) {
//      return 0d;
//    }
//    // notional * unitAmount
//    return resolvedPolicy.getNotional() * unitAmount(resolvedPolicy, provider);
//  }
//
//  // unit amount in various discounting methods
//  private double unitAmount(ResolvedPolicy resolvedPolicy, RatesProvider provider) {
//    switch (resolvedPolicy.getDiscounting()) {
//      case NONE:
//        return unitAmountNone(resolvedPolicy, provider);
//      case ISDA:
//        return unitAmountIsda(resolvedPolicy, provider);
//      case AFMA:
//        return unitAmountAfma(resolvedPolicy, provider);
//      default:
//        throw new IllegalArgumentException("Unknown FraDiscounting value: " + resolvedPolicy.getDiscounting());
//    }
//  }
//
//  // NONE discounting method
//  private double unitAmountNone(ResolvedPolicy resolvedPolicy, RatesProvider provider) {
//    double fixedRate = resolvedPolicy.getFixedRate();
//    double forwardRate = forwardRate(resolvedPolicy, provider);
//    double yearFraction = resolvedPolicy.getYearFraction();
//    return (forwardRate - fixedRate) * yearFraction;
//  }
//
//  // ISDA discounting method
//  private double unitAmountIsda(ResolvedPolicy resolvedPolicy, RatesProvider provider) {
//    double fixedRate = resolvedPolicy.getFixedRate();
//    double forwardRate = forwardRate(resolvedPolicy, provider);
//    double yearFraction = resolvedPolicy.getYearFraction();
//    return ((forwardRate - fixedRate) / (1.0 + forwardRate * yearFraction)) * yearFraction;
//  }
//
//  // AFMA discounting method
//  private double unitAmountAfma(ResolvedPolicy fra, RatesProvider provider) {
//    double fixedRate = fra.getFixedRate();
//    double forwardRate = forwardRate(fra, provider);
//    double yearFraction = fra.getYearFraction();
//    return (1.0 / (1.0 + fixedRate * yearFraction)) - (1.0 / (1.0 + forwardRate * yearFraction));
//  }
//
//  //-------------------------------------------------------------------------
//  // determine the derivative
//  private double derivative(ResolvedPolicy fra, RatesProvider provider) {
//    switch (fra.getDiscounting()) {
//      case NONE:
//        return derivativeNone(fra, provider);
//      case ISDA:
//        return derivativeIsda(fra, provider);
//      case AFMA:
//        return derivativeAfma(fra, provider);
//      default:
//        throw new IllegalArgumentException("Unknown FraDiscounting value: " + fra.getDiscounting());
//    }
//  }
//
//  // NONE discounting method
//  private double derivativeNone(ResolvedPolicy fra, RatesProvider provider) {
//    return fra.getYearFraction();
//  }
//
//  // ISDA discounting method
//  private double derivativeIsda(ResolvedPolicy fra, RatesProvider provider) {
//    double fixedRate = fra.getFixedRate();
//    double forwardRate = forwardRate(fra, provider);
//    double yearFraction = fra.getYearFraction();
//    double dsc = 1.0 / (1.0 + forwardRate * yearFraction);
//    return (1.0 + fixedRate * yearFraction) * yearFraction * dsc * dsc;
//  }
//
//  // AFMA discounting method
//  private double derivativeAfma(ResolvedPolicy fra, RatesProvider provider) {
//    double forwardRate = forwardRate(fra, provider);
//    double yearFraction = fra.getYearFraction();
//    double dsc = 1.0 / (1.0 + forwardRate * yearFraction);
//    return yearFraction * dsc * dsc;
//  }
//
//  //-------------------------------------------------------------------------
//  // query the forward rate
//  private double forwardRate(ResolvedPolicy resolvedPolicy, RatesProvider provider) {
//    return rateComputationFn.rate(resolvedPolicy.getFloatingRate(), resolvedPolicy.getStartDate(), resolvedPolicy.getEndDate(), provider);
//  }
//
//  // query the sensitivity
//  private PointSensitivityBuilder forwardRateSensitivity(ResolvedPolicy fra, RatesProvider provider) {
//    return rateComputationFn.rateSensitivity(fra.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provider);
//  }

}
