package mavenBlue;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.function.BiFunction;

import javax.script.ScriptException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.ParameterizedFunctionalCurveDefinition;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorParameterMetadata;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;




public class EIOPA {

	
	
	 
	  private static final ReferenceData REF_DATA = ReferenceData.standard();
	  // Configuration with discounting curve using OIS up to final maturity; Libor forward curve using IRS.
	
	
	  private static final LocalDate VALUATION_DATE = LocalDate.of(2018, 9, 28);
	 
	  
	  //private static final start=
	  
	  public static void main(String[] args) throws IOException, ParseException, ScriptException, URISyntaxException {
		  
			// TODO Auto-generated method stub
		  //marketActiamOIS(VALUATION_DATE);
		  //marketActiamIRS(VALUATION_DATE);
//		  ImmutableRatesProvider mcurves = marketALM(LocalDate.of(2018, 9, 28));
//		  WriteCurvesR(mcurves, "EUR-DSCON-OIS", "ALM");
//		  WriteCurvesR(mcurves, "EUR-EURIBOR6M-IRS", "ALM");
		  //ImmutableRatesProvider mcurves = marketACTIAM(LocalDate.of(2018, 9, 28));
		  //System.out.println(calibrateSwap(LocalDate.of(2018, 9, 28), 0.01,"30Y"));
		  //WriteCurvesR(mcurves, "EUR-DSCON-OIS", "ACTIAM");
		  //WriteCurvesR(mcurves, "EUR-EURIBOR6M-IRS", "ACTIAM");
			//esr(LocalDate.of(2018, 9, 28));
			eiopa(LocalDate.of(2016, 12, 30));
		  
	  }
	  
	 
	  private final static DayCount CURVE_DC = ACT_365F;	
	  private static final String SCHEME = "OG-Ticker";
	  private static final String CURVE_GROUP_NAME_STR = "LIABILTIES";
	  public static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of(CURVE_GROUP_NAME_STR);
	  
	  private static final String[] FWD6_ID_VALUE = new String[] {
			  "EUSA1 CURNCY",
			  "EUSA2 CURNCY",
			  "EUSA3 CURNCY",
			  "EUSA4 CURNCY",
			  "EUSA5 CURNCY",
			  "EUSA6 CURNCY",
			  "EUSA7 CURNCY",
			  "EUSA8 CURNCY",
			  "EUSA9 CURNCY",
			  "EUSA10 CURNCY",
			  "EUSA12 CURNCY",
			  "EUSA15 CURNCY",
			  "EUSA20 CURNCY"};
	  /** Nodes for the Fwd 3M GBP curve  - goes in the definition */
	  private static final int FWD6_NB_NODES = FWD6_ID_VALUE.length;
	  public static final CurveNode[] ALL_NODES = new CurveNode[FWD6_NB_NODES];
	   
	  private static final double[] NODE_TIMES = new double[FWD6_NB_NODES];
	  /** Tenors for the Fwd 3M GBP swaps */
	  private static final Period[] FWD6_IRS_TENORS = new Period[] {
	      Period.ofYears(1), Period.ofYears(2),Period.ofYears(3),Period.ofYears(4),Period.ofYears(5), Period.ofYears(6),Period.ofYears(7),Period.ofYears(8),Period.ofYears(9), Period.ofYears(10), Period.ofYears(12), Period.ofYears(15),
	      Period.ofYears(20)};
	  static {
	    for (int i = 0; i < FWD6_NB_NODES; i++) {
	      ALL_NODES[i] = FixedIborSwapCurveNode.of(
	          FixedIborSwapTemplate.of(Period.ZERO, Tenor.of(FWD6_IRS_TENORS[i]), EUR_FIXED_1Y_EURIBOR_6M),
	          QuoteId.of(StandardId.of(SCHEME, FWD6_ID_VALUE[i])));
	      NODE_TIMES[i] = CURVE_DC.relativeYearFraction(VALUATION_DATE, ALL_NODES[i].date(VALUATION_DATE, REF_DATA));
	    }
	  }
////
	  private static final SmithWilsonCurveFunction SW_CURVE = SmithWilsonCurveFunction.DEFAULT;
	  private static final double ALPHA = 0.136144;
	  private static final BiFunction<DoubleArray, Double, Double> VALUE_FUNCTION = new BiFunction<DoubleArray, Double, Double>() {
	    @Override
	    public Double apply(DoubleArray t, Double u) {
	    	// t= weights, u = x
	      return SW_CURVE.value(u, ALPHA, DoubleArray.copyOf(NODE_TIMES), t);
	    }
	  };
	  private static final BiFunction<DoubleArray, Double, Double> DERIVATIVE_FUNCTION =
	      new BiFunction<DoubleArray, Double, Double>() {
	        @Override
	        public Double apply(DoubleArray t, Double u) {
	          return SW_CURVE.firstDerivative(u, ALPHA, DoubleArray.copyOf(NODE_TIMES), t);
	        }
	      };
	      
	  private static final BiFunction<DoubleArray, Double, DoubleArray> SENSI_FUNCTION =
	      new BiFunction<DoubleArray, Double, DoubleArray>() {
	        @Override
	        public DoubleArray apply(DoubleArray t, Double u) {
	          return SW_CURVE.parameterSensitivity(u, ALPHA, DoubleArray.copyOf(NODE_TIMES));
	        }
	      };
	  final static CurveName CURVE_NAME = CurveName.of("EIOPA");
	  private static final Builder<TenorParameterMetadata> TenorsMD = ImmutableList.<TenorParameterMetadata>builder();
	  
	  static {
		    for (int i = 0; i < FWD6_NB_NODES; i++) {
		    	TenorsMD.add(TenorParameterMetadata.of(Tenor.of(FWD6_IRS_TENORS[i]),"SWAP-"+Tenor.of(FWD6_IRS_TENORS[i]).toString()));
		    }
		    
		  }
	  
	 
	  
	 private static final ParameterizedFunctionalCurveDefinition CURVE_DEFN = ParameterizedFunctionalCurveDefinition.builder()
				      .name(CURVE_NAME)
				      .xValueType(ValueType.YEAR_FRACTION)
				      .yValueType(ValueType.DISCOUNT_FACTOR)
				      .dayCount(CURVE_DC)
				      .initialGuess(DoubleArray.filled(FWD6_NB_NODES, 0d).toList())
				      .valueFunction(VALUE_FUNCTION)
				      .derivativeFunction(DERIVATIVE_FUNCTION)
				      .sensitivityFunction(SENSI_FUNCTION)
				      .parameterMetadata(TenorsMD.build())
				      .nodes(ALL_NODES)
				      .build();
	///  //
	 
	  
	  
	 

	
		 
	
		  // Repo and issuer curves - ignoring right now, but have to match trades 
		
		 
		 private static final double ONE_BP = 1.0e-4;
		 public static final RatesCurveGroupDefinition CURVE_GROUP_DEFN = RatesCurveGroupDefinition.builder()
			      .name(CURVE_GROUP_NAME)
			      .addCurve(CURVE_DEFN, EUR, EUR_EURIBOR_6M)
			      .build();
		 public static  void eiopa(LocalDate VALUATION_DATE) throws IOException, ParseException {
//			 double[] rates =new double[121];
//			 System.out.println(CURVE_DEFN.getParameterCount());
//			 //ImmutableRatesProvider result2 = CALIBRATOR.calibrate(CURVE_GROUP_DEFN, data0(VALUATION_DATE), REF_DATA);
//			 DiscountFactors dsc = result2.discountFactors(EUR);
//			
//			    double prevDsc = 0d;
//			    for (int i = 0; i < 121; ++i) {
//			      double time = ((double) i);
//			      double curDsc = dsc.discountFactor(time);
//			      rates[(int) time]=dsc.zeroRate(time);
//			      if (i > 59) {
//			        double fwd = prevDsc / curDsc - 1d;
//			        System.out.println(fwd);
//			        //assertEquals(fwd, 0.042, 2d * ONE_BP);
//			      }
//			      //assertEquals(curDsc, DSC_EXP.get(i), ONE_PC);
//			      prevDsc = curDsc;
//			    }
			
		  }
	
}
