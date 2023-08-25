package liabilities;

import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.ejml.simple.SimpleMatrix;
import static com.opengamma.strata.basics.index.PriceIndices.EU_EXT_CPI;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.rate.InflationRateSensitivity;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.RateComputation;

import product.ResolvedPolicy;
import utilities.HullWhiteAnalytical;
import utilities.Taylor;

/**
 * Pricer for for forward rate agreement (FRA) products.
 * <p>
 * This provides the ability to price {@link ResolvedPolicy}.
 * The product is priced using a forward curve for the index.
 */
public class DiscountingPolicyProductPricer_p_LMM {

  /**
   * Default implementation.
   */
  public static final DiscountingPolicyProductPricer_p_LMM DEFAULT = new DiscountingPolicyProductPricer_p_LMM(
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
  public DiscountingPolicyProductPricer_p_LMM(
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
  public Pair<SimpleMatrix, SimpleMatrix> diffMat(ResolvedPolicy resolvedPolicy, RatesProvider provider, ReferenceData refData ) {
	
  	Optional<Surface> qxS = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getMale() ? "AAG-M" : "AAG-V")).findData(SurfaceName.of(resolvedPolicy.getMale() ? "AAG-man" : "AAG-vrouw")) ;
  	Optional<Surface> LapseS = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getMale() ? "VERVAL_UL_2019M" : "VERVAL_UL_2019V")).findData(SurfaceName.of(resolvedPolicy.getMale() ? "verv-man" : "verv-vrouw")) ;
  	Optional<Surface> porfS = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getMale() ? "PORT-M" : "PORT-V")).findData(SurfaceName.of(resolvedPolicy.getMale() ? "portf-man" : "portf-vrouw")) ;
  	Optional<Surface> tarif = refData.getValue(TransitionRatesId.of("OG-Ticker", resolvedPolicy.getTarifId())).findData(SurfaceName.of(resolvedPolicy.getTarifId())) ;
  
  	DiscountFactors df = provider.discountFactors(resolvedPolicy.getCurrency());
  	
  	//PriceIndexValues values = provider.priceIndexValues(EU_EXT_CPI);
  	Curve curve = provider.findData(CurveName.of("EUR-CPI")).get();
  	
  	int exp = Period.between(provider.getValuationDate(),resolvedPolicy.getExpiryDate()).getYears();
  	int age = Period.between(resolvedPolicy.getBirthDate(),provider.getValuationDate()).getYears();
  	int valYear = provider.getValuationDate().getYear();
  
  	
  	int steps = exp;
  	if (steps==0) return  null;//CurrencyAmount.of(Currency.EUR, 0);
  	double GK;
  	double costF= 16.11;
  	double dt=this.dt;
  	
  	double[] fwd = new double[(int) ((steps+1)/dt)];
  	double prevDsc = 1d;
  	for (double i = 1; i < steps+2; i=i+dt) {
  		double curDsc = df.discountFactor(i);
  		fwd[(int) ((i-1)/dt)]=  prevDsc / curDsc -1 ;
  		prevDsc = curDsc;
  	}
  	//System.out.println(fwd[steps]);
  	//Guaranteed capital through proxy/pseudo-account
  	double y0= 0;
  	double mr=resolvedPolicy.getMortalityRestitution();
  	double gr = resolvedPolicy.getRateInvestementAccountGuaranteed();
  	// Two types of policies
  	if (gr!=0){
  		y0=resolvedPolicy.getInvestementAccountProxy();
  	for (double i = 0; i < steps; i=i+dt) {
	  		double tar = tarif.get().zValue(age+i, valYear+i);
	  		y0=(tar*(1-mr/100)+gr*(1+tar)+1/dt)*y0*dt;
	  	//deltaV == P + qx*(1-r)(V + P) + (1+gr*(1-behkosten))(V + P + qx(1-r)*(V + Prem))
	  	//deltaV == qx*(1-mr)F + (1+r)*(1-behkosten))(F  + qx(1-mr)*F)
	  	}
  	};
  	GK=y0;//172
  	//GK=0d;
  	//double factor = resolvedPolicy.getInvestementAccountGuaranteed()>0 ? resolvedPolicy.getInvestementAccount()/resolvedPolicy.getInvestementAccountGuaranteed():1;
  	double terbeh = resolvedPolicy.getExpenseRateinvestementAccount();
  	//double terbehNGua = resolvedPolicy.getExpenseRateInvestementAccountGuaranteed();
  	
  	// Not guaranteed part
  	//startwaarde?	
	//double[] accountDevelopment = new double[steps]; 
	//Account
	//deltaV == P + qx*(V + P) + 0.01*(V + P + qx*(V + Prem))
	
//  	for (int i = 0; i < steps; i++) {
//  		
//  		double tar = tarif.get().zValue(age+i, valYear+i);
//  		
//  		y0=(tar+fwd[i]*(1+tar)+1)*y0;// - kosten beh + ter
//  		accountDevelopment[i]= y0;
//  		
//  	}
//	//Reserve
//  	//D[V[t],t] =(fwd[t])*V[t]-Co1*1.01^t-Lapse1(fw20d1[t]-V[t])-.9qxB[t]*(0-V[t]),V[ K] == poF
//  	
//  	prevDsc = 0d;
//  	//y0=GK+Math.max(y0-GK,0);
//  	
//  	for (int i = 0; i < steps; i++) {
//  		
//  		double qx = qxS.get().zValue(age+i, valYear+i);
//  		double lapse = LapseS.get().zValue(age+i, valYear+i)/100;
//  		double portfCorr = porfS.get().zValue(age+i, valYear+i)/100;
//  		//double tar = tarif.get().zValue(age+i, valYear+i);
//  		y0=-((fwd[i]+lapse+portfCorr*qx+1)*y0 - accountDevelopment[i]*(lapse - costNgua)- costF);// Niet zo handig
//  	}
  	
  	//if not already calculated for this policy  - reuse
  	//if (true){//this.y==null
  		
  		//HUll WHite pars
  		double a0=0.001258720889208218;
   	   	double b0=0.00013;
   	   	double s0=0.00349;
   	   	int ind=42;//171
   	 double fwdCorr= 1.0e-4*0;
  		SimpleMatrix p3S = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "p3S")).getDifferenationMatrix() ;
  		SimpleMatrix p4S = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "p4S")).getDifferenationMatrix() ;
  		SimpleMatrix s2S = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "s2S")).getDifferenationMatrix().plus(fwd[0]+fwdCorr) ;
  		SimpleMatrix s3S = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "s3S")).getDifferenationMatrix().scale(resolvedPolicy.getInvestementAccount());
  		//SimpleMatrix s4S = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "s4S")).getDifferenationMatrix().scale(resolvedPolicy.getInvestementAccount());
  		//DoubleArray s4 = Taylor.toArray(s4S) ;
  		DoubleArray s3 = Taylor.toArray(s3S) ;
		DoubleArray s2 = Taylor.toArray(s2S) ;
		//double scaleValGuar=resolvedPolicy.getInvestementAccountGuaranteed()==0?0:1/(0.1*resolvedPolicy.getInvestementAccountGuaranteed()); 
		double scaleVal=resolvedPolicy.getInvestementAccount()==0?0: 1/(0.1*resolvedPolicy.getInvestementAccount()); 
  		SimpleMatrix s3DerivFirstS = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "s3DerivFirstS")).getDifferenationMatrix().scale(scaleVal) ;
  		//SimpleMatrix s4DerivFirstS = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "s4DerivFirstS")).getDifferenationMatrix() ;
  		SimpleMatrix dS = refData.getValue(DifferentiationMatrixId.of("OG-Ticker", "dS")).getDifferenationMatrix();
//  		
  		
	 	SimpleMatrix yS = new SimpleMatrix(s3.size(),1,true,DoubleArray.copyOf(s3.stream().map(i-> GK + Math.max(0,i-GK)).toArray()).toArray());
	 	SimpleMatrix ySAdj = new SimpleMatrix(s3.size(),1);
	 	ySAdj.set(ind, 0, 1);
	 	SimpleMatrix fullY=yS;
	 	SimpleMatrix fullYadj=ySAdj;
	 	
	 	//double[][] temp =new double[yS.numRows()][steps];
	 	HullWhiteAnalytical HW = new HullWhiteAnalytical(a0,b0,s0);
	 	
	 	long start1 = System.currentTimeMillis();
	 	for (double i = steps; i > -1; i=i-dt) {
	 		double t= steps-i;
	 		double qx = qxS.get().zValue(age+t, valYear+t);
	  		double lapse = LapseS.get().zValue(age+t, valYear+t)/100;
	  		double portfCorr = porfS.get().zValue(age+t, valYear+t)/100;
	  		double inflation = curve.yValue(t)/100;
	  		//double inflation = Math.pow(1.01, steps+1-i);
	  		//double restitutie= (1-mr)/100;
	  		//int pos = (int) Math.floor(i);
	  		//double fwdCorr= fwd[(int) (i*dt)] + 1.0e-4*0 - Math.log(HullWhiteAnalytical.price(0d, Double.valueOf(i), fwd[0])/HullWhiteAnalytical.price(0d, Double.valueOf(i+1), fwd[0]));
	  		
	  		//double test= modelForwardRate(fwd[0],10,a0,b0,s0);
	  		DoubleArray ta = s2.plus(portfCorr*qx+lapse + fwdCorr);	
	  		SimpleMatrix p1S = SimpleMatrix.diag(ta.toArray());
		 	p1S.convertToSparse();
		 	SimpleMatrix t2S = SimpleMatrix.diag(s3.multipliedBy(s2.plus(-terbeh + fwdCorr)).toArray());
		 	s3DerivFirstS.convertToSparse();
		 	t2S.convertToSparse();
		 	SimpleMatrix p2S=t2S.mult(s3DerivFirstS);
		 	p2S.convertToSparse();
		 	//SimpleMatrix t5S = SimpleMatrix.diag(s4.multipliedBy(s2.plus(-terbehNGua+ fwdCorr)).toArray());
		 	//s4DerivFirstS.convertToSparse();
		 	//t5S.convertToSparse();
		 	//SimpleMatrix p5S=t5S.mult(s3DerivFirstS);
		 	//p5S.convertToSparse();
	 		//SimpleMatrix y1 = p1S.minus(p2S).minus(p3S).minus(p4S).minus(dS.scale(1/dt)).mult(yS).plus(s3S.scale(-lapse-portfCorr*qx*mr/100).minus(costF*inflation)).scale(dt);
	 		//SimpleMatrix y1Adj = p1S.minus(p2S).minus(p3S).minus(p4S).transpose().minus(dS.scale(1/dt)).mult(ySAdj).scale(dt);
	 		SimpleMatrix y1 = p1S.minus(p2S).minus(p3S).minus(p4S).minus(dS.scale(1/dt)).mult(yS).plus(s3S.scale(-lapse-portfCorr*qx*mr/100).minus(costF*inflation)).scale(dt);
	 		SimpleMatrix y1Adj = p1S.minus(p2S).minus(p3S).minus(p4S).transpose().minus(dS.scale(1/dt)).mult(ySAdj).scale(dt);
	 		yS=y1.scale(-1);
	 		ySAdj=y1Adj.scale(-1);
	 		fullY=fullY.concatColumns(yS);
	 		fullYadj=fullYadj.concatColumns(ySAdj);
	 		
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
  public PointSensitivityBuilder presentValueSensitivity(ResolvedPolicy resolvedPolicy, RatesProvider provider, Pair<SimpleMatrix, SimpleMatrix> diffMat) {
	  double dt=this.dt;
	  int steps = Period.between(provider.getValuationDate(),resolvedPolicy.getExpiryDate()).getYears();
	  //DiscountFactors discountFactors = provider.discountFactors(Currency.EUR); 
	  SimpleMatrix fwdDer = SimpleMatrix.identity(diffMat.getFirst().numRows()).scale(-1);
	  PointSensitivityBuilder sens = PointSensitivityBuilder.none();
	  List<Double> l= new ArrayList<Double>();
	  DiscountFactors discountFactors = provider.discountFactors(Currency.EUR);
	  double prevTmp=0;
	  double tmp=0;
	  int totalSteps= (int) (steps/dt);
	  for (int i = 0; i < (totalSteps+1); i++) {
		  prevTmp=tmp;
		  tmp = diffMat.getSecond().cols(totalSteps-i, totalSteps-i + 1).transpose().mult(fwdDer).mult(diffMat.getFirst().cols(totalSteps, totalSteps+1)).get(0,0);//*(-i/(Math.log(discountFactors.discountFactor(i+1))));
		  //sensTemp=sensTemp.concatColumns(tmp);
		  l.add(tmp);
		  
		double accrualFactor = 1;//pointSensitivity.getObservation().getYearFraction();
    	double fixingStartDate=i;
    	double fixingEndDate=i+1;
        double forwardBar = tmp;
        double dfForwardStart = discountFactors.discountFactor(i);
        double dfForwardEnd = discountFactors.discountFactor(i+1);
        double dfStartBar = forwardBar / (accrualFactor * dfForwardEnd);
        double dfEndBar = -forwardBar * dfForwardStart / (accrualFactor * dfForwardEnd * dfForwardEnd);
        ZeroRateSensitivity zrsStart = discountFactors.zeroRatePointSensitivity(i, Currency.EUR).multipliedBy(tmp);
        ZeroRateSensitivity zrsEnd = discountFactors.zeroRatePointSensitivity(fixingEndDate, Currency.EUR);
        CurrencyParameterSensitivities psStart = discountFactors.parameterSensitivity(zrsStart);
        CurrencyParameterSensitivities psEnd = discountFactors.parameterSensitivity(zrsEnd).multipliedBy(dfEndBar);
        ZeroRateSensitivity zrsEnd1 = ZeroRateSensitivity.of(Currency.EUR, i+1, tmp);
        ZeroRateSensitivity zrsStart1 = ZeroRateSensitivity.of(Currency.EUR, i, tmp);
        sens = sens.combinedWith(zrsEnd1).combinedWith(zrsStart1);
		  //builder= builder.combinedWith(InflationRateSensitivity.of(PriceIndexObservation.of(EU_EXT_CPI, YearMonth.of(2020+i, 6)),tmp));
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
	double sum = l.stream().mapToDouble(f -> f.doubleValue()/10000).sum();
	System.out.println(sum);
    return sens;
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
	  return CurrencyAmount.of(Currency.EUR, diffMat.getFirst().cols(col-1,col).get(42));
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
