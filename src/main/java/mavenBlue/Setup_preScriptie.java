package mavenBlue;


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
import static com.opengamma.strata.measure.StandardComponents.marketDataFactory;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
import static java.util.stream.Collectors.toList;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import javax.script.ScriptException;

import org.ejml.simple.SimpleMatrix;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndexObservation;
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
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.PerturbationMapping;
import com.opengamma.strata.calc.marketdata.ScenarioDefinition;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.CsvIterator;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.examples.data.export.ExportUtils;
import com.opengamma.strata.examples.marketdata.ExampleData;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.loader.csv.TradeCsvLoader;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParallelShifts;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.ParameterMetadata;
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
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.swaption.SwaptionSurfaceExpiryTenorParameterMetadata;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesId;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.bond.FixedCouponBondSecurity;
import com.opengamma.strata.product.bond.FixedCouponBondYieldConvention;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapPaymentPeriod;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionTrade;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

import liabilities.DifferentiationMatrix;
import liabilities.DifferentiationMatrixId;
import liabilities.MortalityRates;
import liabilities.TransitionRatesId;
import measure.PolicyTradeCalculationFunction;
import utilities.Taylor;
import valILS.ILS;
public class Setup_preScriptie {
	
	
	private static LocalDate VAL_DATE = LocalDate.of(2020, 6, 30);
	private static final TradeCsvLoader standard = TradeCsvLoader.standard();
	
//	private static final DiscountingSwapProductPricer SWAP_PRICER =
//		      DiscountingSwapProductPricer.DEFAULT;
	private static final RatesCurveCalibrator CALIBRATOR = RatesCurveCalibrator.of(1e-9, 1e-9, 100);
	//static List<Trade> trades = standard.load(locator).getValue();
	//static List<Trade> totTrades = standard.load(ResourceLocator.of("classpath:trades/swaptions.csv")).getValue();
	//static List<Trade> trades = new ArrayList<Trade>();
	//static List<Trade> totTrades = standard.load(ResourceLocator.of("classpath:trades/BulletPaymentTrades.csv")).getValue();
	//static List<Trade> totTrades = standard.load(ResourceLocator.of("classpath:trades/Policies2.csv")).getValue();
	static List<Trade> totTrades = standard.load(ResourceLocator.of("classpath:trades/tradesCurvefac30-6.csv")).getValue();
	//static List<Trade> totTrades = standard.load(ResourceLocator.of("classpath:trades/totTrades.csv")).getValue();
	
	
	
	//private static final double TOLERANCE_PV = 1.0E-6;
	private static final String GROUPS = "classpath:example-calibration/curves/actiam/groups-eur_ALM.csv";
	private static final String SETTINGS = "classpath:example-calibration/curves/actiam/settings-eur_ALM.csv";
	private static final String CALIBRATION = "classpath:example-calibration/curves/actiam/calibrations-eur_ALM.csv";
	
	static Map<CurveGroupName, RatesCurveGroupDefinition> configs2= RatesCalibrationCsvLoader.load(
		        ResourceLocator.of(GROUPS),
		        ResourceLocator.of(SETTINGS),
		        ImmutableList.of(ResourceLocator.of(CALIBRATION))); 
	final static CurveGroupName GROUP_NAME = CurveGroupName.of("EUR-USD");
	private static final LocalDateDoubleTimeSeriesBuilder builder0 = LocalDateDoubleTimeSeries.builder();
	
	private static final LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
	static {
		builder0.put(LocalDate.of(2020, 4, 30), 104.77d);
		builder0.put(LocalDate.of(2007, 4, 30), 88.350332d);
	}
	private static final LocalDateDoubleTimeSeries TS_EUR_CPI=builder0.build();
	static  CsvIterator csvFix= CsvIterator.of(ResourceLocator.of("classpath:example-calibration/fixings/fixingsold.csv").getCharSource(), true);
	static {
		
		 builder.put(LocalDate.of(2020, 8, 27), -0.00477);
		//builder.put(LocalDate.of(2007, 4, 30), 88.350332d);
	}
	private static final LocalDateDoubleTimeSeries EURIBOR=builder.build();
	//tsbuilder.put(LocalDate.of(2020, 3, 30), 104.77d);
	
	
	static RatesCurveGroupDefinition configL = EIOPA.CURVE_GROUP_DEFN;
	static CurveGroupName groupName = EIOPA.CURVE_GROUP_NAME;
	
	
	static RatesCurveGroupDefinition configA = configs2.get(GROUP_NAME);
			
	
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
		test();
		report();
		long end = System.currentTimeMillis();
		System.out.println((end-start) + " msec");
		//testCalibration();
		
	}

	public static void test() throws IOException, ParseException, ScriptException, URISyntaxException {
		ImmutableRatesProvider prov = provider().getFirst();
		//SabrParametersSwaptionVolatilities test = SABR.swaptionVols(prov, VAL_DATE);
		StringBuilder sbuilder = new StringBuilder();
		
		IborIndexRates indIb = prov.iborIndexRates(IborIndices.EUR_EURIBOR_6M);
		  for (int i = 0; i < 500; i++) { 
		      sbuilder.append(indIb.rate(IborIndexObservation.of(IborIndices.EUR_EURIBOR_6M,VAL_DATE.plusMonths(i),REF_DATA))).append(';').append("\n");
		    }
		  ExportUtils.export(sbuilder.toString(), "C:\\\\Users\\\\9318300\\\\Documents\\\\projs\\\\ALMvalidations\\\\EURIBOR6M-30-6.csv");
		  StringBuilder sbuilder2 = new StringBuilder();
		  Curve ind = prov.findData(CurveName.of("EUR-DSC")).get();
		  for (int i = 0; i < 121; ++i) { 
		      sbuilder2.append(ind.yValue(i)).append(';').append("\n");
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
			  .composedWith(FixedCouponBondTradeCalculationFunction1.INSTANCE)
			  .composedWith(new PresentValueWithZspread())
			  .composedWith(fn2);
	  
	  
	  //Lookup
	  
	  configL=configL.toBuilder().addForwardCurve(ILS.lookup(), (Index) EU_EXT_CPI).build();
	  RatesMarketDataLookup ratesLookup = RatesMarketDataLookup.of(configA);
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
	  CalculationRules rules = CalculationRules.of(functions,ratesLookup, LegalEntityR,swaptionLookup);
	  
	  CalculationRunner runner = CalculationRunner.ofMultiThreaded();
	  
	  ArrayList<Column> columns = Lists.newArrayList(		        
		        Column.of(Measures.PRESENT_VALUE),
		        		//Column.of(Measures.CASH_FLOWS)
		        Column.of(Measures.PV01_MARKET_QUOTE_BUCKETED),
		       //Column.of(Measures1.Z_SPREAD)
		       Column.of(Measures.PV01_CALIBRATED_SUM),
		        Column.of(Measures.PV01_MARKET_QUOTE_SUM)
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
	  //ImmutableRatesProvider multicurve = CALIBRATOR.calibrate(EIOPA.CURVE_GROUP_DEFN, data, REF_DATA);//provider().getSecond();
	  ImmutableRatesProvider multicurve =CALIBRATOR.calibrate(configA, data, REF_DATA);
	  groupName=GROUP_NAME;
//	  int steps= 3;
//	  double dt= 1;
//	  for (int k = 0; k < steps; k++) {
//		  VAL_DATE=VAL_DATE.plusYears((long) k);
	  	
		//builder1.addValue(GROUP_NAME, curve.getName(), curve);
	  	 
	  	 
	  	 
//	  	 multicurve.getCurves().forEach(
//		            (ccy, curve) -> builder1.addValue(CurveId.of(GROUP_NAME, curve.getName()), curve));
//		  
//		  // Use same for repo and any issuer 
//		  multicurve.getDiscountCurves().forEach(
//		            (ccy, curve) -> builder1.addValue(ISSUER_CURVE_ID1, curve));
//		  multicurve.getDiscountCurves().forEach(
//		            (ccy, curve) -> builder1.addValue(REPO_CURVE_ID1, curve));
	
		  //builder1.addTimeSeries(IndexQuoteId.of(EUR_EURIBOR_3M), EURIBOR);;
		
	  	 ImmutableMarketDataBuilder builder1 = ImmutableMarketData.builder(VAL_DATE);
	  	multicurve.getCurves().forEach(
	            (ccy, curve) -> builder1.addValue(CurveId.of(groupName, curve.getName()), curve));
//	  	builder1.removeValueIf(id -> ((CurveId) id).getCurveName() == CurveName.of("USD-DSC"));
//	  	builder1.removeValueIf(id -> ((CurveId) id).getCurveName() == CurveName.of("EUR-DSC"));
	  	
	  	ImmutableMarketData data1 = builder1.build();
		  ImmutableRatesProvider provInfl = ILS.provider();
		  ImmutableMarketDataBuilder builder11 = ImmutableMarketData.builder(VAL_DATE);
		  
		  Curve curve = provInfl.findData(CurveName.of("EUR-CPI")).get();
		  
		  builder11.addValue(CurveId.of(groupName, CurveName.of("EUR-CPI")), curve);
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
		  
		  //builder11.addTimeSeries(IndexQuoteId.of(IborIndex.of("EUR-EURIBOR-6M")), builderEU.build());
		  //builder11.addTimeSeries(IndexQuoteId.of(IborIndex.of("USD-LIBOR-3M")), builderUS.build());
		  ScenarioMarketData MARKET_DATA1 = ScenarioMarketData.of(
			      1,
			      data1.combinedWith(builder11.build()));
		  PerturbationMapping<Curve> mapping = PerturbationMapping.of(
			        MarketDataFilter.ofName(CurveName.of("EIOPA")),
			        CurveParallelShifts.absolute(0.0,0.0001)	);
		  ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mapping);
		  scenarioDefinition = ScenarioDefinition.empty();
		  //Results results = runner.calculateMultiScenario(rules, trades, columns,scenarioMarketData, REF_DATA);
		 
		  //Trades
//		  List<Trade> totTrades = Stream.concat(trades.stream(), trades1.stream())
//                  .collect(Collectors.toList());
//		  int steps= 1;
//		  double dt= 1;
//		  for (int k = 0; k < steps; k++) {
//			  //int k=(int) i ;
//			  
//			  
//			  List<Trade> lt= new ArrayList<Trade>();
//			  for (Trade e : totTrades) {
//				  if (e instanceof SwaptionTrade){
//					  SwaptionTrade tr = (SwaptionTrade) e;
//					  TradeInfo info = tr.getInfo().toBuilder().tradeDate(tr.getInfo().getTradeDate().get().minusYears(k)).build();
//				  	  tr= tr.toBuilder().info(info).build();
//					  Swaption pr = tr.getProduct();
//					  Swap swap=pr.getUnderlying();
//					  ResolvedSwap swapR=pr.getUnderlying().resolve(REF_DATA);
//					  ResolvedSwapLeg fixedLeg = swapR.getLegs(SwapLegType.FIXED).get(0);
//					  RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) fixedLeg.getPaymentPeriods().get(0);
//					  PayReceive pay = swapR.getLegs(SwapLegType.FIXED).get(0).getPayReceive();
//					  BuySell bs = pay==PayReceive.PAY?BuySell.BUY:BuySell.SELL;
//					  SwapPaymentPeriod firstPeriod = fixedLeg.getPaymentPeriods().get(0);
//					  RatePaymentPeriod payment = (RatePaymentPeriod) firstPeriod;
//					  RateAccrualPeriod accrualPeriod0 = ratePaymentPeriod.getAccrualPeriods().get(0);
//					  double fr = ((FixedRateComputation) accrualPeriod0.getRateComputation()).getRate();
//					  SwapTrade underlying = EUR_FIXED_1Y_EURIBOR_6M.toTrade(tr.getInfo().getTradeDate().get(),swap.getStartDate().getUnadjusted().minusYears(k), swap.getEndDate().getUnadjusted().minusYears(k), bs, payment.getNotional(), fr);
//					  pr= pr.toBuilder().expiryDate(AdjustableDate.of(pr.getExpiryDate().getUnadjusted().minusYears(k))).underlying(underlying.getProduct())
//					  .build();
//					  tr= tr.toBuilder().product(pr).build();
//					  lt.add(tr);
//				  } else if(e instanceof SwapTrade)  {
//				  	  SwapTrade tr = (SwapTrade) e;
//				  	  //TradeInfo info = tr.getInfo().toBuilder().tradeDate(tr.getInfo().getTradeDate().get().minusYears(0)).build();
//				  	  //tr= tr.toBuilder().info(info).build();
//				  	  Swap swap = tr.getProduct();
//				  	  ResolvedSwap swapR=swap.resolve(REF_DATA);
//					  ResolvedSwapLeg fixedLeg = swapR.getLegs(SwapLegType.FIXED).get(0);
//					  RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) fixedLeg.getPaymentPeriods().get(0);
//					  PayReceive pay = swapR.getLegs(SwapLegType.FIXED).get(0).getPayReceive();
//					  BuySell bs = pay==PayReceive.PAY?BuySell.BUY:BuySell.SELL;
//					  SwapPaymentPeriod firstPeriod = fixedLeg.getPaymentPeriods().get(0);
//					  RatePaymentPeriod payment = (RatePaymentPeriod) firstPeriod;
//					  RateAccrualPeriod accrualPeriod0 = ratePaymentPeriod.getAccrualPeriods().get(0);
//					  double fr = ((FixedRateComputation) accrualPeriod0.getRateComputation()).getRate();
//					  if (payment.getCurrency().equals(EUR)){
//						  SwapTrade underlying2 = EUR_FIXED_1Y_EURIBOR_6M.toTrade(tr.getInfo(),swap.getStartDate().getUnadjusted().minusYears(k), swap.getEndDate().getUnadjusted().minusYears(k), bs, payment.getNotional(), fr);
//						  tr= tr.toBuilder().product(underlying2.getProduct()).build();
//					  } else {  
//						  SwapTrade underlying2 = USD_FIXED_6M_LIBOR_3M.toTrade(tr.getInfo(),swap.getStartDate().getUnadjusted().minusYears(k), swap.getEndDate().getUnadjusted().minusYears(k), bs, payment.getNotional(), fr);
//						  tr= tr.toBuilder().product(underlying2.getProduct()).build();
//						  }
//					  //pr= pr.toBuilder().expiryDate(AdjustableDate.of(pr.getExpiryDate().getUnadjusted().minusYears(k))).underlying(underlying.getProduct());
//					  
//					  lt.add(tr);
//					} else if(e instanceof SecurityTrade)  {
//						SecurityTrade tr = (SecurityTrade) e;
//						lt.add(tr);
//					}
//			  }
//			  totTrades= lt;
//			// Requirements
//			   InterpolatedNodalCurve dsE = (InterpolatedNodalCurve) multicurve.findData(CurveName.of("EUR-DSC")).get();
//			   InterpolatedNodalCurve dsU = (InterpolatedNodalCurve) multicurve.findData(CurveName.of("USD-DSC")).get();
//			   double[] yratesEUR= new double[120] , yratesUSD= new double[120], xval= new double[120];
//			   
//			   for (int i = 0; i < 120; i++) {
//				   
//				   yratesEUR[i]= -Math.log(multicurve.discountFactors(EUR).discountFactor(i)/multicurve.discountFactors(EUR).discountFactor(k+1))/(k+1);
//				   yratesUSD[i]= -Math.log(multicurve.discountFactors(Currency.USD).discountFactor(i)/multicurve.discountFactors(Currency.USD).discountFactor(k+1))/(k+1);
//				   xval[i]=i;
//				   
//			   }
//			   CurveMetadata metaEUR = Curves.zeroRates(CurveName.of("EUR-DSC"), ACT_365F, ParameterMetadata.listOfEmpty(120));
//			   CurveMetadata metaUSD = Curves.zeroRates(CurveName.of("USD-DSC"), ACT_365F, ParameterMetadata.listOfEmpty(120));
//			   InterpolatedNodalCurve dscEurNext = InterpolatedNodalCurve.of(metaEUR, DoubleArray.copyOf(xval), DoubleArray.copyOf(yratesEUR), dsE.getInterpolator());
//			   InterpolatedNodalCurve dscUSNext = InterpolatedNodalCurve.of(metaUSD, DoubleArray.copyOf(xval), DoubleArray.copyOf(yratesUSD), dsU.getInterpolator());			   
//			  	MarketData updEURdsc = MARKET_DATA1.scenario(0).withValue(CurveId.of(GROUP_NAME, dscUSNext.getName()),  dscUSNext);
//			  	MarketData updUSDdsc = MARKET_DATA1.scenario(0).withValue(CurveId.of(GROUP_NAME, dscEurNext.getName()),  dscEurNext);
//			  	
//			  	MARKET_DATA1 = ScenarioMarketData.of(
//					      1,
//					      updUSDdsc.combinedWith(updEURdsc).combinedWith(data1)); 	
			  	MarketDataRequirements reqs = MarketDataRequirements.of(rules, totTrades, columns, REF_DATA);
			  //ScenarioDefinition scenarioDefinition = ScenarioDefinition.empty();
			  ScenarioMarketData scenarioMarketData =marketDataFactory().createMultiScenario(reqs, MarketDataConfig.empty(), MARKET_DATA1, REF_DATA, scenarioDefinition);
			  //l1.get(0).expiryDate(AdjustableDate.of(VAL_DATE)).build();
//			  if (k==0 ) {
//				  columns.add(Column.of(Measures1.presentValueWithSpread));
//			  }
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
					tradeReport.writeCsv(new FileOutputStream("C:\\Users\\9318300\\Documents/tradesOut"+ k + ".csv"));
				  } catch (FileNotFoundException ed) {
					// TODO Auto-generated catch block
					ed.printStackTrace();
				  }
			  //Update bond trades with results from t= 0 
//			  List<CalculationTarget> l1 = calculationResults.getTargets();
//			  List<Result<?>> l2 = calculationResults.getCalculationResults().columnResults(1).collect(Collectors.toList());
//			  
//			  Map<CalculationTarget, Result<?>> map = zipToMap(l1,l2);
//			  List<Trade> lt2= new ArrayList<Trade>();
//			  if (k==0 ) {
//				 
//				  for (Map.Entry<CalculationTarget, Result<?>> entry : map.entrySet()) {
//					  SecurityTrade tr = (SecurityTrade) entry.getKey(); 
//					  
//					  TradeInfo tradeInfoUpdt = tr.getInfo().toBuilder().addAttribute(AttributeType.of("Z-spread"),entry.getValue().getValue()).build();
//					  tr= tr.toBuilder().info(tradeInfoUpdt).build();
//					  lt2.add(tr);
//					  }
//					  
//				  
//				  totTrades.clear();
//				  totTrades.addAll(lt2);
//				  columns.remove(Column.of(Measures1.Z_SPREAD));
//				  columns.add(Column.of(Measures1.presentValueWithSpread));
//			  }
			  
			  
			  //StringBuilder sbuilder21 = new StringBuilder();
			  
			  //List<Result<?>> l21 = calculationResults.getCalculationResults().columnResults(1).collect(Collectors.toList());
			  //Map<CalculationTarget, Result<?>> map1 = zipToMap(l1,l21);
//			  if (k==1){
//			  for (Map.Entry<CalculationTarget, Result<?>> entry : map1.entrySet()) {
//				  SecurityTrade tr = (SecurityTrade) entry.getKey();
////				  CurrencyAmount c= CurrencyAmount.of(Currency.EUR,1);
////				  c.getAmount();
//				  CurrencyAmount c =(CurrencyAmount) entry.getValue().getValue();
//			      sbuilder21.append(tr.getId().get().getValue()).append(';').append(c.getAmount()).append("\n");
//			    }
//			  ExportUtils.export(sbuilder21.toString(), "C:/Users/9318300/OneDrive - Athora Netherlands/Mijn Documenten/ALMvalidations/strataOut/tradesOut"+ k + ".csv");
//			  }
			 
				
//		}
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
static{
	double a0=0.001258720889208218;
	double b0=0.00013;
	double s0=0.00349;
	Object[] g1 = IntStream.range(-2, 2).mapToDouble(i -> i*0.01).boxed().toArray();
 	Object[] g2 = IntStream.range(-2, 2).mapToDouble(i -> i*.1+1).boxed().toArray();
 	Object[] g3 = IntStream.range(-2, 2).mapToDouble(i -> i*.1+1).boxed().toArray();
 	List<List<Object>> s = Lists.cartesianProduct(Arrays.asList(g1),Arrays.asList(g2),Arrays.asList(g3));
 	List<Object> l= new ArrayList<Object>();l.add(0.0);
 	l.add(1.0);
 	l.add(1.0);
 	s.indexOf(l);
 	//s vectors
 	DoubleArray s2 = DoubleArray.copyOf(Taylor.toArray(0,s));
 	DoubleArray s3 = DoubleArray.copyOf(Taylor.toArray(1,s));
 	DoubleArray s4 = DoubleArray.copyOf(Taylor.toArray(2,s));
 	int size = g1.length*g2.length*g3.length;
 	SimpleMatrix s2S = new SimpleMatrix(size, 1,true, s2.toArray()); 
 	SimpleMatrix s3S = new SimpleMatrix(size, 1,true, s3.toArray());
 	SimpleMatrix s4S = new SimpleMatrix(size, 1,true, s4.toArray());
 	// Differentiation matrices
 	SimpleMatrix ds2S = SimpleMatrix.identity(g1.length);
 	SimpleMatrix ds3S= SimpleMatrix.identity(g2.length);
 	SimpleMatrix ds4S= SimpleMatrix.identity(g3.length);
 	SimpleMatrix s2DerivFirstS = Taylor.UDFtoMatrixS(g1.length,1,2,0.01).kron(ds3S).kron(ds4S);
 	SimpleMatrix s2DerivSecS =  Taylor.UDFtoMatrixS(g1.length,2,3,0.01).kron(ds3S).kron(ds4S);
 	SimpleMatrix s3DerivFirstS = ds2S.kron(Taylor.UDFtoMatrixS(g2.length,1,2,1).kron(ds4S));
 	SimpleMatrix s4DerivFirstS = ds2S.kron(ds3S).kron(Taylor.UDFtoMatrixS(g3.length,1,2,1));
 	
 	SimpleMatrix dS= SimpleMatrix.identity(size);
 	SimpleMatrix p3S = s2DerivSecS.scale(.5*s0*s0);
 	SimpleMatrix t2S = SimpleMatrix.diag(s2.multipliedBy(b0).plus(a0).toArray());
 	s2DerivFirstS.convertToSparse();
 	t2S.convertToSparse();
 	SimpleMatrix p4S = s2DerivFirstS.mult(t2S);
 	p3S.convertToSparse();
 	p4S.convertToSparse();
 	dS.convertToSparse();
 	builderRefData1
 	.put(DifferentiationMatrixId.of("OG-Ticker", "s2S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s2S"), s2S))
 	.put(DifferentiationMatrixId.of("OG-Ticker", "s3S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s3S"), s3S))
 	.put(DifferentiationMatrixId.of("OG-Ticker", "s4S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s4S"), s4S))
 	.put(DifferentiationMatrixId.of("OG-Ticker", "p3S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "p3S"), p3S))
 	.put(DifferentiationMatrixId.of("OG-Ticker", "p4S"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "p4S"), p4S))
 	.put(DifferentiationMatrixId.of("OG-Ticker", "s3DerivFirstS"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s3DerivFirstS"), s3DerivFirstS))
 	.put(DifferentiationMatrixId.of("OG-Ticker", "s4DerivFirstS"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "s4DerivFirstS"), s4DerivFirstS))
 	.put(DifferentiationMatrixId.of("OG-Ticker", "dS"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "dS"), dS))
 	.build();
	REF_DATA= REF_DATA.combinedWith((ImmutableReferenceData) ImmutableReferenceData.of(builderRefData1.build()));
}
public static <K, V> Map<K, V> zipToMap(List<K> keys, List<V> values) {
    return IntStream.range(0, keys.size()).boxed()
            .collect(Collectors.toMap(keys::get, values::get));
}
//rd= ImmutableReferenceData.of(ImmutableMap.of("1", aag));
	//REF_DATA= REF_DATA.combinedWith((ImmutableReferenceData) ImmutableReferenceData.of(builder.build()));
}