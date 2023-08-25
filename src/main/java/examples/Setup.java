package examples;


import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.PriceIndices.EU_EXT_CPI;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.SECURITY_ID_SCHEME_FIELD;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static java.util.stream.Collectors.toList;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import javax.script.ScriptException;

import org.ejml.simple.SimpleMatrix;
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
import com.opengamma.strata.calc.marketdata.MarketDataFactory;
import com.opengamma.strata.calc.marketdata.MarketDataFilter;
import com.opengamma.strata.calc.marketdata.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.ObservableDataProvider;
import com.opengamma.strata.calc.marketdata.PerturbationMapping;
import com.opengamma.strata.calc.marketdata.ScenarioDefinition;
import com.opengamma.strata.calc.marketdata.TimeSeriesProvider;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.CsvFile;
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
import com.opengamma.strata.examples.marketdata.ExampleData;
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
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.measure.bond.LegalEntityDiscountingMarketDataLookup;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.swaption.SwaptionMarketDataLookup;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.curve.RatesCurveCalibrator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.swaption.SwaptionSurfaceExpiryTenorParameterMetadata;
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
import liabilities.DifferentiationMatrix;
import liabilities.DifferentiationMatrixId;
import liabilities.MortalityRates;
import liabilities.NonObservableId;
import liabilities.TransitionRatesId;
import measure.PolicyTradeCalculationFunction;
import product.ImmutablePolicyConvention;
import product.PolicyConvention;
import product.StandardPolicyConventions;
import umontreal.ssj.rng.MRG31k3p;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.stochprocess.OrnsteinUhlenbeckProcess;
import utilities.HWAnalytical;
import utilities.Taylor;
import valILS.ILS;
public class Setup {
	
	
	private static LocalDate VAL_DATE = LocalDate.of(2020, 6, 30);
	private static final TradeCsvLoader standard = TradeCsvLoader.standard();
	
//	private static final DiscountingSwapProductPricer SWAP_PRICER =
//		      DiscountingSwapProductPricer.DEFAULT;
	private static final RatesCurveCalibrator CALIBRATOR = RatesCurveCalibrator.of(1e-9, 1e-9, 100);
	//static List<Trade> trades = standard.load(locator).getValue();
	//static List<Trade> totTrades = standard.load(ResourceLocator.of("classpath:trades/swaptions.csv")).getValue();
	//static List<Trade> trades = new ArrayList<Trade>();
	//static List<Trade> totTrades = standard.load(ResourceLocator.of("classpath:trades/BulletPaymentTrades.csv")).getValue();
	static List<Trade> totTrades = standard.load(ResourceLocator.of("classpath:trades/policies_test2_deb.csv")).getValue();
	//static List<Trade> totTrades = standard.load(ResourceLocator.of("classpath:trades/totTrades.csv")).getValue();
	
	
	
	//private static final double TOLERANCE_PV = 1.0E-6;
	private static final String GROUPS = "classpath:example-calibration/curves/actiam/groups-eur_ALM.csv";
	private static final String SETTINGS = "classpath:example-calibration/curves/actiam/settings-eur_ALM.csv";
	private static final String CALIBRATION = "classpath:example-calibration/curves/actiam/calibrations-eur_ALM_curvefac.csv";
	
	static Map<CurveGroupName, RatesCurveGroupDefinition> configs2= RatesCalibrationCsvLoader.load(
		        ResourceLocator.of(GROUPS),
		        ResourceLocator.of(SETTINGS),
		        ImmutableList.of(ResourceLocator.of(CALIBRATION))); 
	public final static CurveGroupName GROUP_NAME = CurveGroupName.of("EUR-USD");
	private static final LocalDateDoubleTimeSeriesBuilder builder0 = LocalDateDoubleTimeSeries.builder();
	
	private static final LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
	static {
		builder0.put(LocalDate.of(2020, 4, 30), 104.77d);
		builder0.put(LocalDate.of(2007, 4, 30), 88.350332d);
	}
	private static final LocalDateDoubleTimeSeries TS_EUR_CPI=builder0.build();
	static  CsvIterator csvFix= CsvIterator.of(ResourceLocator.of("classpath:example-calibration/fixings/fixings.csv").getCharSource(), true);
	static {
		
		 builder.put(LocalDate.of(2020, 8, 27), -0.00477);
		//builder.put(LocalDate.of(2007, 4, 30), 88.350332d);
	}
	private static final LocalDateDoubleTimeSeries EURIBOR=builder.build();
	//tsbuilder.put(LocalDate.of(2020, 3, 30), 104.77d);
	
	
	static RatesCurveGroupDefinition configL = EIOPA.CURVE_GROUP_DEFN;
	static CurveGroupName groupName = EIOPA.CURVE_GROUP_NAME;
	
	public static RatesCurveGroupDefinition configA = configs2.get(GROUP_NAME);
			
	
	public static Pair<ImmutableRatesProvider,ImmutableRatesProvider> provider() throws IOException, ParseException, ScriptException, URISyntaxException {
		
		ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VAL_DATE);
		  ImmutableMap<QuoteId, Double> quotes = QuotesCsvLoader.load(VAL_DATE,ResourceLocator.of("classpath:example-calibration/quotes/quotesALL.csv"));
		  builder.addValueMap(quotes);
	  //builder.addTimeSeries(IndexQuoteId.of(EUR_EURIBOR_3M), EURIBOR);
		  //builder.addTimeSeries(IndexQuoteId.of(EU_EXT_CPI), TS_EUR_CPI);
		  ImmutableMarketData data = builder.build();
		
		  //ImmutableMarketData.of(VAL_DATE,quotes);
		  //combined= RatesCurveGroupDefinition.builder().addCurve(configA)
		  return  Pair.of(CALIBRATOR.calibrate(configA, data, REF_DATA),CALIBRATOR.calibrate(EIOPA.CURVE_GROUP_DEFN, data, REF_DATA));
		  //return CALIBRATOR.calibrate(config, data, REF_DATA); 		  
	}
	
	public static void main(String[] args) throws IOException, ParseException, ScriptException, URISyntaxException {
		//test();
		long start = System.currentTimeMillis();
		report();
		long end = System.currentTimeMillis();
		System.out.println((end-start) + " msec");
		//testCalibration();
		
	}

	public static void test() throws IOException, ParseException, ScriptException, URISyntaxException {
		ImmutableRatesProvider prov = provider().getFirst();
		//SabrParametersSwaptionVolatilities test = SABR.swaptionVols(prov, VAL_DATE);
		StringBuilder sbuilder = new StringBuilder();
		DiscountFactors dsc = prov.discountFactors(EUR);
		  for (int i = 0; i < 121; ++i) { 
		      sbuilder.append(dsc.discountFactor(i)).append(';').append("\n");
		    }
		 // ExportUtils.export(sbuilder.toString(), "C:/Users/9318300/OneDrive - Athora Netherlands/Mijn Documenten/ALMvalidations/dscRatesEUR.csv");
		  StringBuilder sbuilder2 = new StringBuilder();
		  Curve ind = prov.findData(CurveName.of("EUR-DSC")).get();
		  for (int i = 0; i < 121; ++i) { 
		      sbuilder2.append(ind.yValue(i)).append(',').append("\n");
		    }
		  //ExportUtils.export(sbuilder2.toString(), "C:/Users/9318300/OneDrive - Athora Netherlands/Mijn Documenten/ALMvalidations/eurdsc.csv");
		
	        
	}
	 
	final static LegalEntityId ISSUER_A = LegalEntityId.of("OG-Ticker", "GOVT");
	
	public static void report() throws IOException, ParseException, ScriptException, URISyntaxException {
		
	  
	  
	  final CurveId ISSUER_CURVE_ID1 = CurveId.of("GOVT","EUR-DSC");
	  final CurveId REPO_CURVE_ID1 = CurveId.of( "GOVT1 BOND1","OG-Ticker");
	  final RepoGroup GROUP_REPO_V1 = RepoGroup.of("BONDS");
	  final LegalEntityGroup GROUP_ISSUER_V1 = LegalEntityGroup.of("ATHORA");
  
	  
	  //Functions
	  
	  //Class<SecuritizedProductTrade<FixedCouponBond>> targetType = null;
	  CalculationFunctions fn2 = CalculationFunctions.of(new PolicyTradeCalculationFunction());
	  CalculationFunctions functions = StandardComponents.calculationFunctions()
			  //.composedWith(FixedCouponBondTradeCalculationFunction1.INSTANCE)
			  //.composedWith(new PresentValueWithZspread())
			  .composedWith(fn2);
	  
	  
	 // configL=configL.toBuilder().addForwardCurve(ILS.lookup(), (Index) EU_EXT_CPI).build();
	  //RatesMarketDataLookup ratesLookup = RatesMarketDataLookup.of(configL);
	  //RatesMarketDataLookup ratesLookup = RatesMarketDataLookup.of(configA);
	  //Change asset liab 
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
	  
	  LegalEntityDiscountingMarketDataLookup  LegalEntityR = LegalEntityDiscountingMarketDataLookup.of(
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
	
	  // Scenarios
	  
	  //Data	t=0
	  ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VAL_DATE);
	  ImmutableMap<QuoteId, Double> quotes = QuotesCsvLoader.load(VAL_DATE,ResourceLocator.of("classpath:example-calibration/quotes/quotesALL.csv"));
	  builder.addValueMap(quotes);
  //builder.addTimeSeries(IndexQuoteId.of(EUR_EURIBOR_3M), EURIBOR);
	  //builder.addTimeSeries(IndexQuoteId.of(EU_EXT_CPI), TS_EUR_CPI);
	  ImmutableMarketData data = builder.build();
	  ImmutableRatesProvider multicurve = CALIBRATOR.calibrate(EIOPA.CURVE_GROUP_DEFN, data, REF_DATA);//provider().getSecond();
	 //marketdata
		
	  	 ImmutableMarketDataBuilder builder1 = ImmutableMarketData.builder(VAL_DATE);
	  	multicurve.getCurves().forEach(
	            (ccy, curve) -> builder1.addValue(CurveId.of(groupName, curve.getName()), curve));
//	  	builder1.removeValueIf(id -> ((CurveId) id).getCurveName() == CurveName.of("USD-DSC"));
//	  	builder1.removeValueIf(id -> ((CurveId) id).getCurveName() == CurveName.of("EUR-DSC"));
	  	
	  	ImmutableMarketData data1 = builder1.build();
		  ImmutableRatesProvider provInfl = ILS.provider();
		  ImmutableMarketDataBuilder builder11 = ImmutableMarketData.builder(VAL_DATE);
		  
		  Curve curveInfl = provInfl.findData(CurveName.of("EUR-CPI")).get();
		  
		  builder11.addValue(CurveId.of(groupName, CurveName.of("EUR-CPI")), curveInfl);
		  builder11.addTimeSeries(IndexQuoteId.of(EU_EXT_CPI), TS_EUR_CPI);
		  //builder11.addValue(SwaptionVolatilitiesId.of("SABR"), SABR.swaptionVols(multicurve, VAL_DATE));
		  LocalDateDoubleTimeSeriesBuilder builderEU = LocalDateDoubleTimeSeries.builder();
		  LocalDateDoubleTimeSeriesBuilder builderUS = LocalDateDoubleTimeSeries.builder();
		  for (CsvRow row : csvFix.asIterable()) {	  
		      String  ref= row.getValue("Reference");
		      LocalDate date = row.getValue("Date", LoaderUtils::parseDate);
		      double value = row.getValue("Value",LoaderUtils::parseDouble);
		      if (ref.equals("EUR-EURIBOR-6M")) builderEU.put(date, value); else builderUS.put(date, value); ;
		}
		  
		  builder11.addTimeSeries(IndexQuoteId.of(IborIndex.of("EUR-EURIBOR-6M")), builderEU.build());
		  builder11.addTimeSeries(IndexQuoteId.of(IborIndex.of("USD-LIBOR-3M")), builderUS.build());
		  //ESG based on EIOPA
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
		  builder11.addValue(CurveId.of(groupName, CurveName.of("ESG")), curveESG);
		  builder11.addValue(CurveId.of(groupName, CurveName.of("Equity")), curveEQ);
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
//		  PerturbationMapping<ParameterizedData> mapping2 = PerturbationMapping.of(
//				  MarketDataFilter.ofName(CurveName.of("ESG")),
//			        // no shift for the base scenario, 1bp absolute shift to calibrated curves (zeros)
//			        buildShifts(curveESG,ResourceLocator.of("file:src/main/resources/csv/scen2_R10K (1).csv")));
//		PerturbationMapping<ParameterizedData> mapping3 = PerturbationMapping.of(
//				  MarketDataFilter.ofName(CurveName.of("Equity")),
//			        // no shift for the base scenario, 1bp absolute shift to calibrated curves (zeros)
//			        buildShifts(curveEQ,ResourceLocator.of("file:src/main/resources/csv/scen2_S10K (2).csv")));
//		  
//		 
		  //ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mapping,mappingPar);
		  //ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mappingPar,mapping_bp);
		  //ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mappingPar,mapping_bp);
		//ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mapping_deb);
//		PerturbationMapping<ParameterizedData> mapping2 = PerturbationMapping.of(
//				  MarketDataFilter.ofName(CurveName.of("ESG")),
//			        // no shift for the base scenario, 1bp absolute shift to calibrated curves (zeros)
//			        buildShifts1(curveESG,1));
//		PerturbationMapping<ParameterizedData> mapping3 = PerturbationMapping.of(
//				  MarketDataFilter.ofName(CurveName.of("Equity")),
//			        // no shift for the base scenario, 1bp absolute shift to calibrated curves (zeros)
//			        buildShifts1(curveEQ,5));
//		 ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mapping2, mapping3);
//		 //
		ScenarioDefinition scenarioDefinition = ScenarioDefinition.empty();
		  //LabelDateParameterMetadataBuilder nodes= LabelDateParameterMetadataBuilder.builder();
		  
		  //Lookup w/o curve settings
		  ImmutableMap<Currency, CurveId> map = ImmutableMap.of(EUR, CurveId.of(groupName, CurveName.of("ESG")));
		  //ImmutableMap<Currency, CurveId> map0 = ImmutableMap.of();
		  ImmutableMap<Index, CurveId> map1 = ImmutableMap.of(FxIndices.EUR_USD_ECB, CurveId.of(groupName, CurveName.of("Equity")),IborIndices.EUR_LIBOR_3M, CurveId.of(groupName, CurveName.of("VOLS")),(Index)EU_EXT_CPI, CurveId.of(groupName, CurveName.of("EUR-CPI")));
		  RatesMarketDataLookup ratesLookup1 = 
				  RatesMarketDataLookup.of(map,map1);
		  // pre calculate the diff. matrices 
		 
		  ReferenceData b = ((ImmutablePolicyConvention) StandardPolicyConventions.UNIT_LINKED).addRefdata(REF_DATA);
		REF_DATA= REF_DATA.combinedWith(b); 

		  //configL=configL.toBuilder().addForwardCurve(ILS.lookup(), (Index) EU_EXT_CPI).build();
		  
		  CalculationRules rules = CalculationRules.of(functions,ratesLookup1, LegalEntityR,swaptionLookup);
			  	MarketDataRequirements reqs = MarketDataRequirements.of(rules, totTrades, columns, REF_DATA);
			  	//reqs.builder().addValues(id1).build();
			  //ScenarioDefinition scenarioDefinition = ScenarioDefinition.empty();
			  	reqs= MarketDataRequirements.combine(Arrays.asList(reqs,MarketDataRequirements.builder().addValues(bp).addValues(id1).build()));
			  	//MarketDataFactory mf = marketDataFactory();
			  	//List<MarketDataFunction<?, ?>> stdFuncts = new ArrayList();
			  	List<MarketDataFunction<?, ?>> li = StandardComponents.marketDataFunctions();
			  	ArrayList li2 = new ArrayList<>(li);
			  	li2.add(new TestMarketDataFunction());
			  	 MarketDataFactory factory = MarketDataFactory.of(ObservableDataProvider.none(), TimeSeriesProvider.none(),li2);
//			  	MarketDataBox<Curve> md23 = MARKET_DATA1.getValue(CurveId.of(groupName, CurveName.of("ESG")));
//			  	 MarketDataBox<Double> md2 = MARKET_DATA1.getValue(id1);
			  	 ScenarioMarketData scenarioMarketData =factory.createMultiScenario(reqs, MarketDataConfig.empty(), MARKET_DATA1, REF_DATA, scenarioDefinition);
			  //l1.get(0).expiryDate(AdjustableDate.of(VAL_DATE)).build();
//			  if (k==0 ) {
//				  columns.add(Column.of(Measures1.presentValueWithSpread));
//			  }
			  //CalculationRules rules = CalculationRules.of(functions,ratesLookup, LegalEntityR,swaptionLookup);
			  Results results = runner.calculateMultiScenario(rules, totTrades, columns,scenarioMarketData, REF_DATA);
			  ReportCalculationResults calculationResults =
				        ReportCalculationResults.of(VAL_DATE, totTrades, columns, results, functions, REF_DATA);
			  
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
	private static PointShifts buildShifts(Curve basisCurve, ResourceLocator resource) {
		PointShiftsBuilder  builder = PointShifts.builder(ShiftType.ABSOLUTE);
	    CsvFile csv = CsvFile.of(resource.getCharSource(), false);
	    //Map<CurveName, List<CurveNode>> allNodes = new HashMap<>();
		double a0=0.001258720889208218;
		double b0=0.00013;
		double s0=0.00349;
	    HWAnalytical HW = new HWAnalytical(a0,b0,s0);
	    for (int curveNodeIdxCSV = 0; curveNodeIdxCSV< (csv.rowCount()-2); curveNodeIdxCSV++) {
	      ImmutableList<String> scenarios = csv.row(curveNodeIdxCSV).fields().subList(5000,9999);
	    	//ImmutableList<String> scenarios = csv.row(curveNodeIdxCSV).fields();
	      List<ParameterMetadata> curveNodeMetadata = basisCurve.getMetadata().getParameterMetadata().get();
		    
	      // build up the shifts to apply to each node
	      // these are calculated as the actual change in the zero rate at that node between the two scenarios 
	      // for each row
	      int scenarioIndex = 0;
	      //double fwdRate = basisCurve.yValue(curveNodeIdxCSV);
	      //double initrate= basisCurve.yValue(0);
	      //double fwdRate = Math.log(HWAnalytical.price(0d, Double.valueOf(curveNodeIdxCSV), initrate)/HWAnalytical.price(0d, Double.valueOf(curveNodeIdxCSV+1), initrate));
	      for (String item:scenarios) { //column iterations		
	    	//  if (basisCurve.getName().toString()=="DFAScen1") {	  
	        //double zeroRate = basisCurve.yValue(curveNodeIdx);
	        //System.out.println(zeroRate);
	    	  
	        double shift = Double.parseDouble(item);
	        //builder.addShift(scenarioIndex, curveNodeMetadata.get(curveNodeIdx).getIdentifier(), 0d);
	        builder.addShift(scenarioIndex, curveNodeMetadata.get(curveNodeIdxCSV).getIdentifier(), shift);
	        scenarioIndex++;
	        
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
	
public static Surface addRefDatafromCSV(String csv, String name) {
		
	CsvIterator mortalityrates= CsvIterator.of(ResourceLocator.of("classpath:referenceData/" + csv).getCharSource(), true);
	ArrayList<Integer> AGE = new ArrayList<Integer>();
	ArrayList<Integer> YEAR =new ArrayList<Integer>();
	ArrayList<Double> RATE = new ArrayList<Double>();;
	for (CsvRow row : mortalityrates.asIterable()) {
	      //Currency  cur= row.getValue(CURRENCY_FIELD, LoaderUtils::parseCurrency);
	      YEAR.add(row.getValue("YEAR", LoaderUtils::parseInteger));
	      AGE.add(row.getValue("AGE",LoaderUtils::parseInteger));
	      RATE.add(row.getValue("rate",LoaderUtils::parseDouble));
		}
	
	final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
	
	final List<ParameterMetadata> PARAMETER_METADATA =
		      MapStream.zip(AGE.stream(), YEAR.stream())
		          .map(SwaptionSurfaceExpiryTenorParameterMetadata::of)
		          .collect(toList());
		  final SurfaceMetadata METADATA =
		      Surfaces.normalVolatilityByExpiryTenor(name, ACT_365F);
		  
		  final Surface SURFACE_STD = InterpolatedNodalSurface.of(
		          METADATA.withParameterMetadata(PARAMETER_METADATA),
		          DoubleArray.copyOf(AGE.stream().mapToDouble(num -> (double)num).toArray()),
		          DoubleArray.copyOf(YEAR.stream().mapToDouble(num -> (double)num).toArray()),	
		          DoubleArray.copyOf(RATE.stream() //we start with a stream of objects Stream<int[]>
		        		    .flatMapToDouble(DoubleStream::of) //we I'll map each int[] to IntStream
		        		    .toArray()),
		          INTERPOLATOR_2D);
	
	return SURFACE_STD;
	
}
static{
	builderRefData
	.put(TransitionRatesId.of("OG-Ticker", "AAG-M"), MortalityRates.of(TransitionRatesId.of("OG-Ticker", "AAG-M"), VAL_DATE, addRefDatafromCSV("AG2018_man.csv","AAG-man")))
	.put(TransitionRatesId.of("OG-Ticker", "AAG-V"), MortalityRates.of(TransitionRatesId.of("OG-Ticker", "AAG-V"), VAL_DATE, addRefDatafromCSV("AG2018_vrouw.csv","AAG-vrouw")))
	.put(TransitionRatesId.of("OG-Ticker", "PORT-M"), MortalityRates.of(TransitionRatesId.of("OG-Ticker", "PORT-M"), VAL_DATE, addRefDatafromCSV("PORT_STERFTE_MAN_2019.csv","portf-man")))
	.put(TransitionRatesId.of("OG-Ticker", "PORT-V"), MortalityRates.of(TransitionRatesId.of("OG-Ticker", "PORT-V"), VAL_DATE, addRefDatafromCSV("PORT_STERFTE_VRW_2019.csv","portf-vrouw")))
	.put(TransitionRatesId.of("OG-Ticker", "207"), MortalityRates.of(TransitionRatesId.of("OG-Ticker", "207"), VAL_DATE, addRefDatafromCSV("ZL1001SN.csv","207")))
	.put(TransitionRatesId.of("OG-Ticker", "220"), MortalityRates.of(TransitionRatesId.of("OG-Ticker", "220"), VAL_DATE, addRefDatafromCSV("PREMTAB21POSNEG.csv","220")))
	.put(TransitionRatesId.of("OG-Ticker", "221"), MortalityRates.of(TransitionRatesId.of("OG-Ticker", "221"), VAL_DATE, addRefDatafromCSV("PREMTAB32NEG.csv","221")))
	.put(TransitionRatesId.of("OG-Ticker", "VERVAL_UL_2019M"), MortalityRates.of(TransitionRatesId.of("OG-Ticker", "VERVAL_UL_2019M"), VAL_DATE, addRefDatafromCSV("VERVAL_UL_2019M.csv","verv-man")))
	.put(TransitionRatesId.of("OG-Ticker", "VERVAL_UL_2019V"), MortalityRates.of(TransitionRatesId.of("OG-Ticker", "VERVAL_UL_2019V"), VAL_DATE, addRefDatafromCSV("VERVAL_UL_2019V.csv","verv-vrouw")))
	.build();
	REF_DATA= REF_DATA.combinedWith((ImmutableReferenceData) ImmutableReferenceData.of(builderRefData.build()));
}
//
static ImmutableMap.Builder<ReferenceDataId<?>, Object> builderRefData1 = ImmutableMap.builder();
//static{
//	double a0=0.001258720889208218;
//	double b0=0.00013;
//	double s0=0.00349;
//	double stepsizes2=0.01;//rente  
//	double stepsizes3=0.05;//eq 
//	double stepsize1=.5d;//fonds
//	double s1= 5*s0;
//	double b1= .5d*b0;
//	
//	List<Double> g2 = IntStream.range(-2, 3).mapToDouble(i -> i*stepsizes2).boxed().toList();//rente
// 	//Object[] g2 = IntStream.range(-2, 3).mapToDouble(i -> i*stepsize1+1).boxed().toArray();
//	List<Double> g1 = IntStream.range(-1, 7).mapToDouble(i -> i*stepsize1 ).boxed().toList();
//	List<Double> g3 = IntStream.range(-2,3).mapToDouble(i -> i*stepsizes3).boxed().toList();//eq
// 	List<List<Double>> s = Lists.cartesianProduct(g1,g2,g3);
// 	List<Object> l= new ArrayList<Object>();
// 	l.add(1.0);
// 	l.add(0.0);
// 	l.add(0.0);
// 	int ind= s.indexOf(l);
// 	//s vectors
// 	DoubleArray s2 = DoubleArray.copyOf(Taylor.toArray(0,s));
// 	DoubleArray s3 = DoubleArray.copyOf(Taylor.toArray(1,s));
// 	DoubleArray s4 = DoubleArray.copyOf(Taylor.toArray(2,s));
// 	int size = g1.size()*g2.size()*g3.size();
// 	//test
// 	//SimpleMatrix sm1 = new SimpleMatrix(5, 5,false, s3.toArray());
// 	//end
// 	SimpleMatrix s2S = new SimpleMatrix(size, 1,true, s2.toArray()); 
// 	SimpleMatrix s3S = new SimpleMatrix(size, 1,true, s3.toArray());
// 	SimpleMatrix s4S = new SimpleMatrix(size, 1,true, s4.toArray());
// 	
// 	SimpleMatrix ds2S = SimpleMatrix.identity(g1.size());
// 	SimpleMatrix ds3S= SimpleMatrix.identity(g2.size());
// 	SimpleMatrix ds4S= SimpleMatrix.identity(g3.size());
// // Differentiation matrices
// 	SimpleMatrix s2DerivFirstS = Taylor.UDFtoMatrixS(g1.size(),1,2,stepsize1).kron(ds3S).kron(ds4S);//;
// 	SimpleMatrix s3DerivFirstS = ds2S.kron(Taylor.UDFtoMatrixS(g2.size(),1,2,stepsizes2)).kron(ds4S);//.kron(ds4S));
// 	SimpleMatrix s4DerivFirstS = ds2S.kron(ds3S).kron(Taylor.UDFtoMatrixS(g3.size(),1,2,stepsizes3));//;
// 	
// 	SimpleMatrix s3DerivSecS =  Taylor.UDFtoMatrixS(g1.size(),2,3,stepsizes2).kron(ds3S).kron(ds4S);
// 	SimpleMatrix s4DerivSecS =  ds2S.kron(ds3S).kron(Taylor.UDFtoMatrixS(g3.size(),2,3,stepsizes3));
// 	//SimpleMatrix s4DerivFirstS = ds2S.kron(ds3S);
// 	
// 	SimpleMatrix dS= SimpleMatrix.identity(size);
// 	//SimpleMatrix dSDet= SimpleMatrix.identity(g2.length);
// 	SimpleMatrix p3S = s3DerivSecS.scale(.5*s0*s0);
// 	SimpleMatrix q3S = s4DerivSecS.scale(.5*s1*s1);
// 	//SimpleMatrix t2S = SimpleMatrix.diag(s2.multipliedBy(-b0).plus(a0).toArray());
// 	List<double[]> cop = Collections.nCopies(dS.numCols(), s3.multipliedBy(-b0).plus(a0).toArray());
// 	double[] vals = cop.stream().flatMapToDouble(x -> Arrays.stream(x)).toArray();
// 	SimpleMatrix sm1 = new SimpleMatrix(dS.numCols(), dS.numCols(), false, vals);
// 	SimpleMatrix p4S=s3DerivFirstS.elementMult(sm1);
// 	
// 	cop = Collections.nCopies(dS.numCols(), s4.multipliedBy(-b1).plus(a0).toArray());
// 	vals = cop.stream().flatMapToDouble(x -> Arrays.stream(x)).toArray();
// 	sm1 = new SimpleMatrix(dS.numCols(), dS.numCols(), false, vals);
// 	SimpleMatrix q4S=s4DerivFirstS.elementMult(sm1);
// 	
// 	
// 	//t2S.convertToSparse();
// 	//SimpleMatrix p4S = s2DerivFirstS.mult(t2S);
// 	//s3DerivFirstS.convertToSparse();
// 	p3S.convertToSparse();
// 	p4S.convertToSparse();
// 	q3S.convertToSparse();
// 	q4S.convertToSparse();
// 	dS.convertToSparse();
//// 	builderRefData1
//// 	.put(DifferentiationMatrixId.of("OG-Ticker", "ind"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "ind"), SimpleMatrix.diag(ind)))
//// 	.put(DifferentiationMatrixId.of("OG-Ticker", "s2S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s2S"), s2S))
//// 	.put(DifferentiationMatrixId.of("OG-Ticker", "s3S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s3S"), s3S))
//// 	.put(DifferentiationMatrixId.of("OG-Ticker", "s4S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s4S"), s4S))
//// 	.put(DifferentiationMatrixId.of("OG-Ticker", "p3S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "p3S"), p3S))//sec der s2
//// 	.put(DifferentiationMatrixId.of("OG-Ticker", "p4S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "p4S"), p4S))//first der s2
//// 	.put(DifferentiationMatrixId.of("OG-Ticker", "q3S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "q3S"), q3S))//sec der s4
//// 	.put(DifferentiationMatrixId.of("OG-Ticker", "q4S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "q4S"), q4S))//first der s4
//// 	.put(DifferentiationMatrixId.of("OG-Ticker", "s3DerivFirstS"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s3DerivFirstS"), s3DerivFirstS))
//// 	//.put(DifferentiationMatrixId.of("OG-Ticker", "s4DerivFirstS"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s4DerivFirstS"), s4DerivFirstS))
//// 	.put(DifferentiationMatrixId.of("OG-Ticker", "dS"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "dS"), dS))
//// 	.build();
//	REF_DATA= REF_DATA.combinedWith((ImmutableReferenceData) ImmutableReferenceData.of(builderRefData1.build()));
//}

//static{
//	double a0=0.001258720889208218;
//	double b0=0.00013;
//	double s0=0.00349;
//	Object[] g1 = IntStream.range(-2, 3).mapToDouble(i -> i*0.01).boxed().toArray();
// 	Object[] g2 = IntStream.range(-2, 3).mapToDouble(i -> i*.1+1).boxed().toArray();
// 	Object[] g3 = IntStream.range(-2, 3).mapToDouble(i -> i*.1+1).boxed().toArray();
// 	List<List<Object>> s = Lists.cartesianProduct(Arrays.asList(g1),Arrays.asList(g2),Arrays.asList(g3));
// 	List<Object> l= new ArrayList<Object>();l.add(0.0);
// 	l.add(1.0);
// 	l.add(1.0);
// 	s.indexOf(l);
// 	//s vectors
// 	DoubleArray s2 = DoubleArray.copyOf(Taylor.toArray(0,s));
// 	DoubleArray s3 = DoubleArray.copyOf(Taylor.toArray(1,s));
// 	DoubleArray s4 = DoubleArray.copyOf(Taylor.toArray(2,s));
// 	int size = g1.length*g2.length*g3.length;
// 	//test
// 	//SimpleMatrix sm1 = new SimpleMatrix(5, 5,false, s3.toArray());
// 	//end
// 	SimpleMatrix s2S = new SimpleMatrix(size, 1,true, s2.toArray()); 
// 	SimpleMatrix s3S = new SimpleMatrix(size, 1,true, s3.toArray());
// 	SimpleMatrix s4S = new SimpleMatrix(size, 1,true, s4.toArray());
// 	// Differentiation matrices
// 	SimpleMatrix ds2S = SimpleMatrix.identity(g1.length);
// 	SimpleMatrix ds3S= SimpleMatrix.identity(g2.length);
// 	SimpleMatrix ds4S= SimpleMatrix.identity(g3.length);
// 	SimpleMatrix s2DerivFirstS = Taylor.UDFtoMatrixS(g1.length,1,2,0.01).kron(ds3S).kron(ds4S);
// 	SimpleMatrix s2DerivSecS =  Taylor.UDFtoMatrixS(g1.length,2,3,0.01).kron(ds3S).kron(ds4S);
// 	SimpleMatrix s3DerivFirstS = ds2S.kron(Taylor.UDFtoMatrixS(g2.length,1,2,.1).kron(ds4S));
// 	//SimpleMatrix s3DerivFirstSDet = Taylor.UDFtoMatrixS(g2.length,1,2,1);
// 	SimpleMatrix s4DerivFirstS = ds2S.kron(ds3S).kron(Taylor.UDFtoMatrixS(g3.length,1,2,1));
// 	
// 	SimpleMatrix dS= SimpleMatrix.identity(size);
// 	//SimpleMatrix dSDet= SimpleMatrix.identity(g2.length);
// 	SimpleMatrix p3S = s2DerivSecS.scale(.5*s0*s0);
// 	SimpleMatrix t2S = SimpleMatrix.diag(s2.multipliedBy(b0).plus(a0).toArray());
// 	s2DerivFirstS.convertToSparse();
// 	t2S.convertToSparse();
// 	SimpleMatrix p4S = s2DerivFirstS.mult(t2S);
// 	p3S.convertToSparse();
// 	p4S.convertToSparse();
// 	dS.convertToSparse();
// 	builderRefData1
// 	.put(DifferentiationMatrixId.of("OG-Ticker", "s2S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s2S"), s2S))
// 	.put(DifferentiationMatrixId.of("OG-Ticker", "s3S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s3S"), s3S))
// 	.put(DifferentiationMatrixId.of("OG-Ticker", "s4S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s4S"), s4S))
// 	.put(DifferentiationMatrixId.of("OG-Ticker", "p3S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "p3S"), p3S))
// 	.put(DifferentiationMatrixId.of("OG-Ticker", "p4S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "p4S"), p4S))
// 	.put(DifferentiationMatrixId.of("OG-Ticker", "s3DerivFirstS"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s3DerivFirstS"), s3DerivFirstS))
// 	.put(DifferentiationMatrixId.of("OG-Ticker", "s4DerivFirstS"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s4DerivFirstS"), s4DerivFirstS))
// 	.put(DifferentiationMatrixId.of("OG-Ticker", "dS"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "dS"), dS))
// 	.build();
//	REF_DATA= REF_DATA.combinedWith((ImmutableReferenceData) ImmutableReferenceData.of(builderRefData1.build()));
//}

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