package examples;


import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.index.IborIndices.CHF_LIBOR_6M;
import static com.opengamma.strata.basics.index.PriceIndices.EU_EXT_CPI;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_SCHEME_FIELD;
import static com.opengamma.strata.measure.StandardComponents.marketDataFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.script.ScriptException;

import org.joda.convert.FromString;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataFilter;
import com.opengamma.strata.calc.marketdata.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.PerturbationMapping;
import com.opengamma.strata.calc.marketdata.ScenarioDefinition;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.CsvIterator;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.loader.csv.TradeCsvLoader;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParallelShifts;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.param.PointShifts;
import com.opengamma.strata.market.param.PointShiftsBuilder;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.measure.bond.LegalEntityDiscountingMarketDataLookup;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.swaption.SwaptionMarketDataLookup;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.curve.RatesCurveCalibrator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesId;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.bond.FixedCouponBondSecurity;
import com.opengamma.strata.product.bond.FixedCouponBondYieldConvention;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

import liabilities.AbsoluteDoubleShift;
import liabilities.NonObservableId;
import measure.PolicyTradeCalculationFunction;
import product.ImmutablePolicyConvention;
import product.StandardPolicyConventions;
import umontreal.ssj.rng.MRG31k3p;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.stochprocess.OrnsteinUhlenbeckProcess;
import utilities.HWAnalytical;
public class Setup {
	//test
	
	private static LocalDate VAL_DATE = LocalDate.of(2020, 6, 30);
	private static final TradeCsvLoader standard = TradeCsvLoader.standard();
	
//	private static final DiscountingSwapProductPricer SWAP_PRICER =
//		      DiscountingSwapProductPricer.DEFAULT;
	private static final RatesCurveCalibrator CALIBRATOR = RatesCurveCalibrator.of(1e-9, 1e-9, 100);
	
	static List<Trade> totTrades = standard.load(ResourceLocator.of("classpath:trades/ALMall.csv")).getValue();
	//static List<Trade> totTrades = standard.load(ResourceLocator.of("classpath:trades/totTrades.csv")).getValue();
	static List<Trade> totPOlTrades = standard.load(ResourceLocator.of("classpath:trades/policies_test2_deb.csv")).getValue();
	
	
	//Curve definitions
	private static final String GROUPS = "classpath:example-calibration/curves/actiam/groups-eur_ALM.csv";
	private static final String SETTINGS = "classpath:example-calibration/curves/actiam/settings-eur_ALM.csv";
	private static final String CALIBRATION = "classpath:example-calibration/curves/actiam/calibrations-eur_ALM.csv";
	
	static Map<CurveGroupName, RatesCurveGroupDefinition> configs2= RatesCalibrationCsvLoader.load(
		        ResourceLocator.of(GROUPS),
		        ResourceLocator.of(SETTINGS),
		        ImmutableList.of(ResourceLocator.of(CALIBRATION))); 
	public final static CurveGroupName GROUP_NAME = CurveGroupName.of("EUR-USD");
	
	public static RatesCurveGroupDefinition configA = configs2.get(GROUP_NAME);
	static RatesCurveGroupDefinition cfg= configA.toBuilder().addCurve(EIOPA.CURVE_DEFN, CHF, CHF_LIBOR_6M).build(); // EIOPA in CHF to avoid conflict in discounting in EUR
	
	
	public static void main(String[] args) throws IOException, ParseException, ScriptException, URISyntaxException {
		//test();
		long start = System.currentTimeMillis();
		report();
		long end = System.currentTimeMillis();
		System.out.println((end-start) + " msec");
		//testCalibration();
		
	}

//	public static void test() throws IOException, ParseException, ScriptException, URISyntaxException {
//		ImmutableRatesProvider prov = provider();//;.getFirst();
//		//SabrParametersSwaptionVolatilities test = SABR.swaptionVols(prov, VAL_DATE);
//		StringBuilder sbuilder = new StringBuilder();
//		DiscountFactors dsc = prov.discountFactors(EUR);
//		  for (int i = 0; i < 121; ++i) { 
//		      sbuilder.append(dsc.discountFactor(i)).append(';').append("\n");
//		    }
//		 // ExportUtils.export(sbuilder.toString(), "C:/Users/9318300/OneDrive - /Mijn Documenten/ALMvalidations/dscRatesEUR.csv");
//		  StringBuilder sbuilder2 = new StringBuilder();
//		  Curve ind = prov.findData(CurveName.of("EUR-DSC")).get();
//		  for (int i = 0; i < 121; ++i) { 
//		      sbuilder2.append(ind.yValue(i)).append(',').append("\n");
//		    }
//		  //ExportUtils.export(sbuilder2.toString(), "C:/Users/9318300/OneDrive - /Mijn Documenten/ALMvalidations/eurdsc.csv");
//		
//	        
//	}
	 
	final static LegalEntityId ISSUER_A = LegalEntityId.of("OG-Ticker", "GOVT");
	
	public static void report() throws IOException, ParseException, ScriptException, URISyntaxException {
  
	  final CurveId ISSUER_CURVE_ID1 = CurveId.of("GOVT","EUR-DSC");
	  final CurveId REPO_CURVE_ID1 = CurveId.of( "GOVT1 BOND1","OG-Ticker");
	  final RepoGroup GROUP_REPO_V1 = RepoGroup.of("BONDS");
	  final LegalEntityGroup GROUP_ISSUER_V1 = LegalEntityGroup.of("INSURER");
	  //Functions
	  
	  //Class<SecuritizedProductTrade<FixedCouponBond>> targetType = null;
	  CalculationFunctions fn2 = CalculationFunctions.of(new PolicyTradeCalculationFunction());
	  CalculationFunctions functions = StandardComponents.calculationFunctions()
			  //.composedWith(FixedCouponBondTradeCalculationFunction1.INSTANCE)
			  //.composedWith(new PresentValueWithZspread())
			  .composedWith(fn2);
	  
	  //Bond curves and swaption lookups  
	  ImmutableMap<LegalEntityId, RepoGroup> repoGroups = ImmutableMap.<LegalEntityId, RepoGroup>builder()
			    .put(
				ISSUER_A, GROUP_REPO_V1).build();
		
	  ImmutableMap<Pair<RepoGroup, Currency>, CurveId> repoCurves = ImmutableMap.<Pair<RepoGroup, Currency>, CurveId>builder()
			    .put(Pair.of(GROUP_REPO_V1, Currency.EUR), REPO_CURVE_ID1)
			    .put(Pair.of(GROUP_REPO_V1, Currency.USD), REPO_CURVE_ID1).build();
	  
	  ImmutableMap<LegalEntityId, LegalEntityGroup> issuerGroups = ImmutableMap.of(
	                ISSUER_A, GROUP_ISSUER_V1);
	  
	  ImmutableMap<Pair<LegalEntityGroup, Currency>, CurveId> issuerCurves = ImmutableMap.<Pair<LegalEntityGroup, Currency>, CurveId>builder()
			    .put(Pair.of(GROUP_ISSUER_V1, Currency.EUR), ISSUER_CURVE_ID1)
			    .put(Pair.of(GROUP_ISSUER_V1, Currency.USD), ISSUER_CURVE_ID1)
			    .build();     
	  
	  LegalEntityDiscountingMarketDataLookup  LegalEntityLookup = LegalEntityDiscountingMarketDataLookup.of(
	    		repoGroups,repoCurves,issuerGroups, issuerCurves);

	  SwaptionMarketDataLookup swaptionLookup = SwaptionMarketDataLookup.of(IborIndices.EUR_EURIBOR_6M, SwaptionVolatilitiesId.of("SABR"));
	 
	  
	  CalculationRunner runner = CalculationRunner.ofMultiThreaded();
	  
	  ArrayList<Column> columns = Lists.newArrayList(		        
		        Column.of(Measures.PRESENT_VALUE),
		        		//Column.of(Measures.CASH_FLOWS)
		        //Column.of(Measures.PV01_CALIBRATED_BUCKETED),
		       //Column.of(Measures1.Z_SPREAD)
		       Column.of(Measures.PV01_CALIBRATED_SUM)
		        //Column.of(Measures.PV01_MARKET_QUOTE_SUM)
		       // Column.of(Measures.PV01_MARKET_QUOTE_BUCKETED)
		        );
	
	  //calibrating  curves  
	  ImmutableMap<QuoteId, Double> quotes = QuotesCsvLoader.load(VAL_DATE,ResourceLocator.of("classpath:example-calibration/quotes/quotes-infl.csv"));
	  ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VAL_DATE);
	  builder.addValueMap(quotes);
	  //builder.addTimeSeries(IndexQuoteId.of(EU_EXT_CPI), TS_EUR_CPI);
	  CsvIterator csvFix= CsvIterator.of(ResourceLocator.of("classpath:example-calibration/fixings/fixingsILS.csv").getCharSource(), true);
	  LocalDateDoubleTimeSeriesBuilder builderFix = LocalDateDoubleTimeSeries.builder();
	  for (CsvRow row : csvFix.asIterable()) {	  
	      String  ref= row.getValue("Reference");
	      LocalDate date = row.getValue("Date", LoaderUtils::parseDate);
	      double value = row.getValue("Value",LoaderUtils::parseDouble);
	      builderFix.put(date, value);
	      }
	  //builder.addValueMap(quotes); 
	  builder.addTimeSeries(IndexQuoteId.of(EU_EXT_CPI), builderFix.build());
		
	  ImmutableMap<QuoteId, Double> quotesA = QuotesCsvLoader.load(VAL_DATE,ResourceLocator.of("classpath:example-calibration/quotes/quotesALL.csv"));
      builder.addValueMap(quotesA);
  
	  ImmutableMarketData data = builder.build();
	  ImmutableRatesProvider multicurve = CALIBRATOR.calibrate(cfg, data, REF_DATA);
	  
	  // add all curves to market data/Used for perturbation?
	  ImmutableMarketDataBuilder builder1 = ImmutableMarketData.builder(VAL_DATE);
	  	multicurve.getCurves().forEach(
	            (ccy, curve) -> builder1.addValue(CurveId.of(GROUP_NAME, curve.getName()), curve));
	//	  	builder1.removeValueIf(id -> ((CurveId) id).getCurveName() == CurveName.of("USD-DSC"));
	//	  	builder1.removeValueIf(id -> ((CurveId) id).getCurveName() == CurveName.of("EUR-DSC"));
	  	
	  ImmutableMarketData data1 = builder1.build();
	  //ImmutableRatesProvider provInfl = ILS.provider();
	  ImmutableMarketDataBuilder builder11 = ImmutableMarketData.builder(VAL_DATE);
	  
	  Builder<LabelDateParameterMetadata> nodeMetadata = ImmutableList.<LabelDateParameterMetadata>builder();
	  Curve discountCurve = multicurve.findData(CurveName.of("EIOPA")).get();
	  
	 
	  StringBuilder sbuilder = new StringBuilder();
	 
	  
	  double prevDsc=1;
	  double d=4;
	  double[] dr = new double[120*(int)d];
	  for (int i=0; i < 120*d; i++) {
		  nodeMetadata.add(LabelDateParameterMetadata.of(VAL_DATE.plusMonths(i*3),i+"M"));
		  double curDsc=  discountCurve.yValue(i/d);
		  dr[i]= 1*(i==0? -Math.log(discountCurve.yValue(1/d)):Math.log(prevDsc / curDsc)*d);
		  sbuilder.append(dr[i]).append(',').append("\n");
		  prevDsc = curDsc;
		   
	  }
		  
	  //ExportUtils.export(sbuilder.toString(), "C:\\Users\\9318300\\Documents\\projs\\ALMvalidations\\dscRatesEUR.csv");
	  final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
	  Curve curveESG = InterpolatedNodalCurve.of(
		        Curves.forwardRates(CurveName.of("ESG"), DayCounts.ACT_365F, nodeMetadata.build()),
		        DoubleArray.copyOf(IntStream.range(0, 120*(int)d).mapToDouble(i->(i/d)).toArray()),
		        //DoubleArray.copyOf(dr),
		        DoubleArray.filled(dr.length,0d),
		        INTERPOLATOR);
	  
	  Curve curveEQ = InterpolatedNodalCurve.of(
		        Curves.forwardRates(CurveName.of("Equity"), DayCounts.ACT_365F, nodeMetadata.build()),
		        DoubleArray.copyOf(IntStream.range(0, 120*(int)d).mapToDouble(i->(i/d)).toArray()),
		        DoubleArray.filled(dr.length,0d),
		        INTERPOLATOR);
//		  Curve vols = InterpolatedNodalCurve.of(
//			        Curves.forwardRates(CurveName.of("vols"), DayCounts.ACT_365F, nodeMetadata.build()),
//			        DoubleArray.copyOf(IntStream.range(0, 120*(int)d).mapToDouble(i->(i/d)).toArray()),
//			        DoubleArray.copyOf(volrates),
//			        INTERPOLATOR);
	  builder11.addValue(CurveId.of(GROUP_NAME, CurveName.of("ESG")), curveESG);
	  builder11.addValue(CurveId.of(GROUP_NAME, CurveName.of("Equity")), curveEQ);
	  //builder11.addValue(CurveId.of(groupName, CurveName.of("VOLS")), vols);
	  builder11.addValue(NonObservableId.of("TimeStep"), new Double(.25d)).addValue(NonObservableId.of("BasisPointShift"), new Double(0d));
	  ScenarioMarketData MARKET_DATA1 = ScenarioMarketData.of(
		      1,
		      data1.combinedWith(builder11.build()));
	  PerturbationMapping<Curve> mapping = PerturbationMapping.of(
		        MarketDataFilter.ofName(CurveName.of("ESG")),
		        CurveParallelShifts.absolute(0,0.0001,0,0.0001,0,0.0001)
	);
	  PerturbationMapping<Curve> mapping_deb = PerturbationMapping.of(
		        MarketDataFilter.ofName(CurveName.of("ESG")),
		        CurveParallelShifts.absolute(0,0.0001)
	);
	  //NonObservableId id = new NonObservableId("TimeStep");
	  NonObservableId id1 = NonObservableId.of("TimeStep");
	PerturbationMapping<Double> mappingPar= PerturbationMapping.of(
			MarketDataFilter.ofId(NonObservableId.of("TimeStep")),
		        new AbsoluteDoubleShift(0,0,.25,.25,.75,.75));
				//new AbsoluteDoubleShift(0,.25,.75));
	NonObservableId bp = NonObservableId.of("BasisPointShift");
	PerturbationMapping<Double> mapping_bp= PerturbationMapping.of(
			  new ExactIdFilter<>(bp),
		        new AbsoluteDoubleShift(0,0.0001,0,0.0001,0,0.0001));
				//new AbsoluteDoubleShift(0,.25,.75));
	PerturbationMapping<Double> mapping_bpD= PerturbationMapping.of(
			MarketDataFilter.ofId(NonObservableId.of("BasisPointShift")),
		        new AbsoluteDoubleShift(0,0,0));
				//new AbsoluteDoubleShift(0,.25,.75));
	PointShiftsBuilder  builderP = PointShifts.builder(ShiftType.ABSOLUTE);
	List<ParameterMetadata> curveNodeMetadata = curveESG.getMetadata().getParameterMetadata().get();
	builderP.addShift(0, curveNodeMetadata.get(0).getIdentifier(), 0.000);//scenario 0 - first node is known  
	for (int i=0; i < 120*d; i++) {
		builderP.addShift(i+1, curveNodeMetadata.get(i).getIdentifier(), 0.0001);
	}
	PerturbationMapping<ParameterizedData> mappingPS = PerturbationMapping.of(
	        MarketDataFilter.ofName(CurveName.of("ESG")),
			        builderP.build()
	);
//		 
	ScenarioDefinition scenarioDefinition = ScenarioDefinition.empty();
	//Lookup w/o curve settings
	 
	  cfg=cfg.toBuilder()
	  .addForwardCurve(CurveName.of("Equity"), FxIndices.EUR_USD_ECB)
	  .addForwardCurve(CurveName.of("EUR-CPI"), (Index)EU_EXT_CPI)
	  //.addForwardCurve(CurveName.of("Equity"), FxIndices.EUR_USD_ECB)
	  .build();
	 RatesMarketDataLookup ratesLookup = RatesMarketDataLookup.of(cfg);
	  //store policy convention related information  
	  ReferenceData b = ((ImmutablePolicyConvention) StandardPolicyConventions.UNIT_LINKED).addRefdata(REF_DATA);
	  REF_DATA= REF_DATA.combinedWith(b); 
	
	  //configL=configL.toBuilder().addForwardCurve(ILS.lookup(), (Index) EU_EXT_CPI).build();
	  
	  CalculationRules rules = CalculationRules.of(functions, ratesLookup, LegalEntityLookup,swaptionLookup);
	  	MarketDataRequirements reqs = MarketDataRequirements.of(rules, totPOlTrades, columns, REF_DATA);
	  	reqs= MarketDataRequirements.combine(Arrays.asList(reqs,MarketDataRequirements.builder().addValues(bp).addValues(id1).build()));
	  	 ScenarioMarketData scenarioMarketData =marketDataFactory().createMultiScenario(reqs, MarketDataConfig.empty(), MARKET_DATA1, REF_DATA, scenarioDefinition);
	  Results results = runner.calculateMultiScenario(rules, totPOlTrades, columns,scenarioMarketData, REF_DATA);
	  ReportCalculationResults calculationResults =
		        ReportCalculationResults.of(VAL_DATE, totPOlTrades, columns, results, functions, REF_DATA);
	  
	  //Report
	  TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("ils-report-template2"); 
	  //TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("all-cashflow-report-template3");
	  
	  TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
	 tradeReport.writeAsciiTable(System.out);
	  int k=1;
	  try {
			tradeReport.writeCsv(new FileOutputStream("C:\\Users\\M65H036\\Onedrive - NN\\Documents\\ALMvalidations\\tradesOut.csv"));
		  } catch (FileNotFoundException ed) {
			// TODO Auto-generated catch block
			ed.printStackTrace();
		  }
	  
				
//		}
}
	private static PointShifts buildShifts1(Curve basisCurve,double k) {
		PointShiftsBuilder  builder = PointShifts.builder(ShiftType.ABSOLUTE);
	    //CsvFile csv = CsvFile.of(resource.getCharSource(), false);
	    //Map<CurveName, List<CurveNode>> allNodes = new HashMap<>();
		double a0=0.001258720889208218;
		double b0=0.00013;
		double s0=0.00349;
		RandomStream stream1 = new MRG31k3p();
	     // Create 3 parallel streams of normal random variates
	    //RandomVariateGen gen1 = new NormalGen (stream1);
	    OrnsteinUhlenbeckProcess ou = new OrnsteinUhlenbeckProcess(0, b0*k*.1, a0/(b0*k*.1), s0*k,
	  	        stream1); 
	    ou.setObservationTimes(.25, 60*4);
	      //double[] times = IntStream.range(0, 120).mapToDouble(i -> i*.25).toArray();
	    
	    HWAnalytical HW = new HWAnalytical(a0,b0,s0);
	    	List<ParameterMetadata> curveNodeMetadata = basisCurve.getMetadata().getParameterMetadata().get();
		      for (int  i= 0;i<10000;i++) { //column iterations		
	    	  double[] out = ou.generatePath();
	    	  for (int curveNodeIdx = 0; curveNodeIdx<out.length ; curveNodeIdx++) {
	    	  
	    		  //double shift = Double.parseDouble(out[curveNodeIdxCSV]);
	    		  builder.addShift(i, curveNodeMetadata.get(curveNodeIdx).getIdentifier(), out[curveNodeIdx]);
	        
	      }
	    }
	    return builder.build();
	   //return  CurvePointShifts.builder(ShiftType.ABSOLUTE).build();
	  }
	
	static  CsvIterator cvs= CsvIterator.of(ResourceLocator.of("classpath:referenceData/refData.csv").getCharSource(), true);
	//Bond reference data
	private static final FixedCouponBondYieldConvention YIELD_CONVENTION = FixedCouponBondYieldConvention.DE_BONDS;
	  
	private static final DaysAdjustment SETTLEMENT_DAYS = DaysAdjustment.ofBusinessDays(2, HolidayCalendarIds.EUTA);
	private static final DayCount DAY_COUNT = DayCounts.ACT_ACT_ICMA;
	//private static final BusinessDayAdjustment BUSINESS_ADJUST = BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, HolidayCalendarIds.EUTA);
	private static final DaysAdjustment EX_COUPON = DaysAdjustment.NONE;
	private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.1, CurrencyAmount.of(EUR, 25));
	private static ReferenceData REF_DATA = ReferenceData.standard();  
	
	
	
	private static double[] volrates= new double[120*12]; 
	static {
		int i=0;
		CsvIterator cvs= CsvIterator.of(ResourceLocator.of("classpath:csv/drift.csv").getCharSource(), true);
		for (CsvRow row : cvs.asIterable()) {
			volrates[i] = row.getValue("vol",LoaderUtils::parseDouble);
			i++;
		}
		
	}
	
	static {
		
		ImmutableMap.Builder<ReferenceDataId<?>, Object> builder = ImmutableMap.builder();
		for (CsvRow row : cvs.asIterable()) {
	      Currency  cur= row.getValue(CURRENCY_FIELD, LoaderUtils::parseCurrency);
	      double notional = row.getValue(NOTIONAL_FIELD, LoaderUtils::parseDouble);
	      int cupFreq = row.getValue("Coupon Frequency", LoaderUtils::parseInteger);
	      double fixedRate = row.getValue(FIXED_RATE_FIELD,LoaderUtils::parseDouble);
	      //Optional<LocalDate> firstCoupon = row.findValue("First coupon").map(s -> LoaderUtils.parseDate(s));
	      Optional<LocalDate> startDateOpt = row.findValue("Start Date").map(s -> LoaderUtils.parseDate(s));
	      Optional<LocalDate> endDateOpt = row.findValue(END_DATE_FIELD).map(s -> LoaderUtils.parseDate(s));
	      String securityIdValue = row.getValue(SECURITY_ID_FIELD);
	      Optional<String> securityIdScheme = row.findValue(SECURITY_ID_SCHEME_FIELD);
	      //LegalEntityId ISSUER_ID = LegalEntityId.of("OG-Ticker", "GOVT");
	      Frequency f = Frequency.P12M;
	      if (cupFreq==1)  f= Frequency.P12M; else if (cupFreq==0) f= Frequency.TERM; else if (cupFreq==2) f= Frequency.P6M; else f= Frequency.P3M;   
	     LocalDate fc;
	      //if (startDateOpt.get().isBefore(firstCoupon.get())) fc=startDateOpt.get(); else fc = firstCoupon.get() ;
	      FixedCouponBondSecurity  fbsec = FixedCouponBondSecurity.builder()
	          .info(SecurityInfo.of(SecurityId.of(securityIdScheme.get(), securityIdValue), PRICE_INFO))
	          .dayCount(DAY_COUNT)
	          .fixedRate(fixedRate/100)
	          .legalEntityId(ISSUER_A)
	          .currency(cur)
	          .notional(notional)
	          .accrualSchedule(
						 PeriodicSchedule.builder()
						 .startDate(startDateOpt.get())
						 .endDate(endDateOpt.get())
						 .firstRegularStartDate(startDateOpt.get())
						 //.firstRegularStartDate(START_ACC)
						 .frequency(f)
						 .businessDayAdjustment(BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, HolidayCalendarIds.EUTA))
						 .stubConvention(StubConvention.SHORT_INITIAL)
						 .build())
	          .settlementDateOffset(SETTLEMENT_DAYS)
	          .yieldConvention(YIELD_CONVENTION)
	          .exCouponPeriod(EX_COUPON)
	          .build();
	      builder.put(SecurityId.of(securityIdScheme.get(), securityIdValue),fbsec).build();
	    }
		REF_DATA= REF_DATA.combinedWith((ImmutableReferenceData) ImmutableReferenceData.of(builder.build()));
		//REF_DATA= REF_DATA.combinedWith((ReferenceData) builder.build());
	  }



static ImmutableMap.Builder<ReferenceDataId<?>, Object> builderRefData = ImmutableMap.builder();
	


public static <K, V> Map<K, V> zipToMap(List<K> keys, List<V> values) {
    return IntStream.range(0, keys.size()).boxed()
            .collect(Collectors.toMap(keys::get, values::get));
}
//Help classes to implement additional market id for timesteps
private static final class ExactIdFilter<T, I extends MarketDataId<T>> implements MarketDataFilter<T, I> {

    private final I id;

    private ExactIdFilter(I id) {
      this.id = id;
    }

    @Override
    public boolean matches(I marketDataId, MarketDataBox<T> marketData, ReferenceData refData) {
      return true;
    }

    @Override
    public Class<?> getMarketDataIdType() {
      return id.getClass();
    }
  }

  /**
   * Market data ID for a piece of non-observable market data that is a string.
   */
  

  private static final class TestMarketDataFunction implements MarketDataFunction<Double, NonObservableId> {

	    @Override
	    public MarketDataRequirements requirements(NonObservableId id, MarketDataConfig marketDataConfig) {
	      return MarketDataRequirements.builder()
	          //.addTimeSeries(new TestIdA(id.str))
	          .build();
	    }

	    @Override
	    public MarketDataBox<Double> build(
	    		NonObservableId id,
	        MarketDataConfig marketDataConfig,
	        ScenarioMarketData marketData,
	        ReferenceData refData) {

	      //LocalDateDoubleTimeSeries timeSeries = marketData.getTimeSeries(new NonObservableId(id.str));
	      return marketData.getValue(id);
	    }

	    @Override
	    public Class<NonObservableId> getMarketDataIdType() {
	      return NonObservableId.class;
	    }
//rd= ImmutableReferenceData.of(ImmutableMap.of("1", aag));
	//REF_DATA= REF_DATA.combinedWith((ImmutableReferenceData) ImmutableReferenceData.of(builder.build()));
  }
  public final static class ParamaterName
  extends MarketDataName<Double>
  implements Serializable {

	/** Serialization version. */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The name.
	 */
	private final String name;
	
	//-------------------------------------------------------------------------
	/**
	 * Obtains an instance from the specified name.
	 * <p>
	 * Curve names may contain any character, but must not be empty.
	 *
	 * @param name  the name of the curve
	 * @return a curve with the specified name
	 */
	@FromString
	public static ParamaterName of(String name) {
	  return new ParamaterName(name);
	}
	
	/**
	 * Creates an instance.
	 * 
	 * @param name  the name of the curve
	 */
	private ParamaterName(String name) {
	  this.name = ArgChecker.notEmpty(name, "name");
	}
	
	//-------------------------------------------------------------------------
	@Override
	public Class<Double> getMarketDataType() {
	  return Double.class;
	}
	
	@Override
	public String getName() {
	  return name;
	}

}
  }