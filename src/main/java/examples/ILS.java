package examples;


import static com.opengamma.strata.basics.currency.Currency.EUR;

/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */


import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.PriceIndices.EU_EXT_CPI;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.measure.StandardComponents.marketDataFactory;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.type.FixedInflationSwapConventions.EUR_FIXED_ZC_EU_EXT_CPI;
import static com.opengamma.strata.product.swap.type.FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.basics.schedule.Frequency;
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
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.io.CsvIterator;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.examples.data.export.ExportUtils;
import com.opengamma.strata.examples.marketdata.ExampleData;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.loader.csv.TradeCsvLoader;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.CurveNodeDate;
import com.opengamma.strata.market.curve.CurveParallelShifts;
import com.opengamma.strata.market.curve.InflationNodalCurve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FixedInflationSwapCurveNode;
import com.opengamma.strata.market.curve.node.FixedOvernightSwapCurveNode;
import com.opengamma.strata.market.curve.node.TermDepositCurveNode;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.swap.SwapTradeCalculationFunction;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.curve.RatesCurveCalibrator;
import com.opengamma.strata.pricer.deposit.DiscountingTermDepositProductPricer;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.PriceIndexValues;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.deposit.type.ImmutableTermDepositConvention;
import com.opengamma.strata.product.deposit.type.TermDepositConvention;
import com.opengamma.strata.product.deposit.type.TermDepositTemplate;
import com.opengamma.strata.product.rate.InflationMonthlyRateComputation;
import com.opengamma.strata.product.rate.RateComputation;
import com.opengamma.strata.product.swap.PriceIndexCalculationMethod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedInflationSwapConventions;
import com.opengamma.strata.product.swap.type.FixedInflationSwapTemplate;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapTemplate;
import com.opengamma.strata.product.swap.type.FixedRateSwapLegConvention;
import com.opengamma.strata.product.swap.type.InflationRateSwapLegConvention;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;
public class ILS {
	
	private static final ReferenceData REF_DATA = ReferenceData.standard();
	private static final LocalDate VAL_DATE = LocalDate.of(2021, 3, 31);
	private static final TradeCsvLoader standard = TradeCsvLoader.standard();
	private static final    ResourceLocator locatorC = ResourceLocator.of("classpath:trades/inflTRadesCal.csv");
	private static final    ResourceLocator locator = ResourceLocator.of("classpath:trades/inflTRadesDB01.csv");
	private static final    ResourceLocator locator2 = ResourceLocator.of("classpath:trades/inflTRadesDB02.csv");
	private static final    ResourceLocator locator3 = ResourceLocator.of("classpath:trades/inflTRadesNWM01.csv");
	
//	static List<Trade> trades = ImmutableList.of(
//			standard.load(locator).getValue().get(0),
//			standard.load(locator2).getValue().get(0),
//			standard.load(locator3).getValue().get(0)
//			);
	static List<Trade> trades = standard.load(locatorC).getValue();
	//trades.addAll(standard.load(locator2).getValue());
//	static{
//		SwapTrade tr = (SwapTrade) trades.get(0);
//		  //TradeInfo info = tr.getInfo().toBuilder().tradeDate(tr.getInfo().getTradeDate().get().minusYears(0)).build();
//		  //tr= tr.toBuilder().info(info).build();
//		  Swap swap = tr.getProduct();
//		  //ResolvedSwap swapR=swap.resolve(REF_DATA);
//		  com.opengamma.strata.product.swap.RateCalculationSwapLeg.Builder legBuilder = RateCalculationSwapLeg.builder();
//		  for (SwapLeg leg : tr.getProduct().getLegs(SwapLegType.INFLATION)) {
//		      RateCalculationSwapLeg swapLeg = (RateCalculationSwapLeg) leg;
//		      //swapLeg.resolve(REF_DATA).getPaymentPeriods().get(0)
//		      Schedule accrualSchedule = swapLeg.getAccrualSchedule().createSchedule(REF_DATA);
//		      PeriodicSchedule.Builder accrualBUILDER = swapLeg.getAccrualSchedule().toBuilder();
//		      ImmutableList.Builder<RateAccrualPeriod> accrualPeriods = ImmutableList.builder();
//		      //accrualBUILDER.
//		      for (int i = 0; i < accrualSchedule.size(); i++) {
//		        SchedulePeriod period = accrualSchedule.getPeriod(i);
//		        // inflation does not use a day count, so year fraction is 1d
//		        //period.toBuilder().set("", swap)
//		        RateAccrualPeriod rap = RateAccrualPeriod.builder(period).rateComputation(null).gearing(0.9).negativeRateMethod(NegativeRateMethod.NOT_NEGATIVE).build();
//		        accrualPeriods.add(rap);//, .9, 0d, NegativeRateMethod.NOT_NEGATIVE));
//		        
//		      };
//		      RatePaymentPeriod pps = RatePaymentPeriod.builder().accrualPeriods(accrualPeriods.build()).build();
//		      //accrualPeriods.build().;
//		      
////		      legBuilder.add(swapLeg.toBuilder()
////		              .accrualPeriods(accrualPeriods.build())
////		              .build());
//		  };
		
//		   tr.toBuilder()
//	        .product(tr.getProduct().toBuilder()
//	            .legs(legs)
//	            .build())
//	        .build();
	//	};
	static String GROUPS = "classpath:example-calibration/curves/groups-infl.csv";
	static String SETTINGS= "classpath:example-calibration/curves/settings-eur-cpi.csv";
	static String CALIBRATION= "classpath:example-calibration/curves/calibrations-cpi-eur.csv";
	static Map<CurveGroupName, RatesCurveGroupDefinition> configs2= RatesCalibrationCsvLoader.load(
		        ResourceLocator.of(GROUPS),
		        ResourceLocator.of(SETTINGS),
		        ImmutableList.of(ResourceLocator.of(CALIBRATION))); 
	final static CurveGroupName GROUP_NAME = CurveGroupName.of("EUR-DSCON-CPI");
	
	//private static final LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
	
//	static {
//		builder.put(LocalDate.of(2020, 4, 30), 104.77d);
//		builder.put(LocalDate.of(2007, 4, 30), 88.350332d);
//	}
	//private static final LocalDateDoubleTimeSeries TS_EUR_CPI=builder.build();
	//tsbuilder.put(LocalDate.of(2020, 3, 30), 104.77d);
	
	public static ImmutableRatesProvider provider() throws IOException, ParseException, ScriptException, URISyntaxException {
		
		  ImmutableMap<QuoteId, Double> quotes = QuotesCsvLoader.load(VAL_DATE,ResourceLocator.of("classpath:example-calibration/quotes/quotes-infl2021.csv"));
		  ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VAL_DATE);
		  builder.addValueMap(quotes);
		  //builder.addTimeSeries(IndexQuoteId.of(EU_EXT_CPI), TS_EUR_CPI);
		  CsvIterator csvFix= CsvIterator.of(ResourceLocator.of("classpath:example-calibration/fixings/fixingsILS.csv").getCharSource(), true);
		  LocalDateDoubleTimeSeriesBuilder builderFix = LocalDateDoubleTimeSeries.builder();
		  for (CsvRow row : csvFix.asIterable()) {	  
		      String  ref= row.getValue("Reference");
		      LocalDate date = row.getValue("Date", LoaderUtils::parseDate);
		      double value = row.getValue("Value",LoaderUtils::parseDouble);
		      builderFix.put(date, value); ;
		}
		  
		  builder.addTimeSeries(IndexQuoteId.of(EU_EXT_CPI), builderFix.build());
		  ImmutableMarketData data = builder.build();
		
		  //ImmutableMarketData.of(VAL_DATE,quotes);
		  
		  return  CALIBRATOR.calibrate(configs2.get(GROUP_NAME), data, REF_DATA);
	}
	
	public static void main(String[] args) throws IOException, ParseException, ScriptException, URISyntaxException {
		test();
		//report();
		
		//testCalibration();
		//ImmutableRatesProvider multicurve = provider();
		
	}
	public static void testCalibration() throws IOException, ParseException, ScriptException, URISyntaxException {
		ImmutableMap<QuoteId, Double> quotes = QuotesCsvLoader.load(VAL_DATE,ResourceLocator.of("classpath:example-calibration/quotes/quotes-infl2021.csv"));
	   ImmutableRatesProvider result = provider();
	   ImmutableList<CurveNode> cpiNodes =  configs2.get(GROUP_NAME).findCurveDefinition(CurveName.of("EUR-CPI")).get().getNodes();
	   ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VAL_DATE);
		  builder.addValueMap(quotes);
		  //builder.addTimeSeries(IndexQuoteId.of(EU_EXT_CPI), TS_EUR_CPI);
		  ImmutableMarketData data = builder.build();
	   
	   //CurveNode[] cpiNodes = CURVES_NODES.get(1).get(0);
	   List<ResolvedTrade> cpiTrades = new ArrayList<>();
	   for (int i = 0; i < cpiNodes.size(); i++) {
	     cpiTrades.add(cpiNodes.get(i).resolvedTrade(1d, data, REF_DATA));
	   }
//	   // ZC swaps
	   SwapTrade tr2 = (SwapTrade) trades.get(0);
	   ResolvedSwap rs = ((ResolvedSwapTrade) tr2.resolve(REF_DATA)).getProduct();
	   for (int i = 0; i < cpiNodes.size(); i++) {
		   
	   //MultiCurrencyAmount pvInfl = SWAP_PRICER.presentValue(
	     //   ((ResolvedSwapTrade) cpiTrades.get(i)).getProduct(), result);
	     System.out.println(SWAP_PRICER.parRate(((ResolvedSwapTrade) cpiTrades.get(i)).getProduct(), result));
	     //System.out.println(SWAP_PRICER.presentValue(((ResolvedSwapTrade) cpiTrades.get(i)).getProduct(), result).getAmount(EUR).getAmount());
	     //pvInfl.getAmount(EUR).getAmount());
	     //assertThat(pvInfl.getAmount(EUR).getAmount()).isCloseTo(0.0, offset(TOLERANCE_PV));
	     
	   }
	   
	}
	public static void test() throws IOException, ParseException, ScriptException, URISyntaxException {
		ImmutableRatesProvider prov = provider();
		//ResolvedSwapLeg swapLeg = DiscountingSwapLegPricerTest.createInflationSwapLeg(false, PAY).resolve(REF_DATA);
//	    DiscountingSwapLegPricer pricer = DiscountingSwapLegPricer.DEFAULT;
//	    SwapTrade tr2 = (SwapTrade) trades.get(2);
//	    
//	    ResolvedSwapLeg resolved = tr2.getProduct().getLeg(PAY).get().resolve(REF_DATA);//Fixed
//	    ResolvedSwapLeg resolved2 = tr2.getProduct().getLeg(RECEIVE).get().resolve(REF_DATA);
//	    RatePaymentPeriod period = (RatePaymentPeriod) tr2.getProduct().getLeg(PAY).get().resolve(REF_DATA).getPaymentPeriods().get(0);
//	    RateComputation ss = period.getAccrualPeriods().get(0).getRateComputation();
//	   InflationMonthlyRateComputation computation = (InflationMonthlyRateComputation) ss;
//	   PriceIndexValues values = prov.priceIndexValues(EU_EXT_CPI);
//	   double indexStart = values.value(computation.getStartObservation());
//	   double indexEnd = values.value(computation.getEndObservation());
//	   double rate = indexEnd / indexStart;
//	   
//	   
//	   double df = prov.discountFactor(EUR, period.getPaymentDate());
//	   double expiry = period.getAccrualPeriods().get(0).getYearFraction();
//	  
//		double price = df * logNormalprice(rate, 0.02, 3, .0166, true);
//		//double price = df * priceNormal(rate, 0.02, 3, 0.0171, CALL);
//	    //return CurrencyAmount.of(currency, price * period.getNotional());
//		 System.out.println(price);
//	    CashFlows cf = pricer.cashFlows(resolved, prov);
//	    CashFlows cf2 = pricer.cashFlows(resolved2, prov);
//	    //CashFlows computed = pricer.cashFlows(resolved.getLeg(RECEIVE), multicurve);
//	    MultiCurrencyAmount pvInfl = SWAP_PRICER.presentValue(
//		         ((ResolvedSwapTrade) tr2.resolve(REF_DATA)).getProduct(), prov);
//	    //System.out.println(pvInfl);
	    //System.out.println(cf2);
	    
	}
	public static void report() throws IOException, ParseException, ScriptException, URISyntaxException {
		//trades=ImmutableList.of(trades.get(0),trades2.get(0));
		ScenarioMarketData scenarioMarketData = marketdataInfl();
		ImmutableMap<QuoteId, Double> quotes = QuotesCsvLoader.load(VAL_DATE,ResourceLocator.of("classpath:example-calibration/quotes/quotes-infl2021.csv"));
		   
		   ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VAL_DATE);
			  builder.addValueMap(quotes);
			  //builder.addTimeSeries(IndexQuoteId.of(EU_EXT_CPI), TS_EUR_CPI);
			  ImmutableMarketData data = builder.build();
			  //ImmutableRatesProvider multicurve = provider();   
			 // InflationNodalCurve ind = (InflationNodalCurve) multicurve.getCurves(GROUP_NAME).get(CurveId.of("EUR-DSCON-CPI", "EUR-CPI"));
		RatesMarketDataLookup ratesLookup = RatesMarketDataLookup.of(configs2.get(GROUP_NAME));
		CalculationFunctions functions = CalculationFunctions.of(new SwapTradeCalculationFunction());
		//CalculationFunctions functions = CalculationFunctions.of(new SwapTradeCalculationFunction1());
		CalculationRules rules = CalculationRules.of(functions,ratesLookup);
		CalculationRunner runner = CalculationRunner.ofMultiThreaded();
	  //Results results = runner.calculateMultiScenario(rules, trades, columns,scenarioMarketData, REF_DATA);
		
		ImmutableList<CurveNode> cpiNodes =  configs2.get(GROUP_NAME).findCurveDefinition(CurveName.of("EUR-CPI")).get().getNodes();
		List<SwapTrade> cpiTradesC = new ArrayList<>();
		   for (int i = 0; i < cpiNodes.size(); i++) {
		     cpiTradesC.add((SwapTrade) cpiNodes.get(i).trade(1d, data, REF_DATA));
		   }
	   List<SwapTrade> cpiTrades = new ArrayList<>();
	   for (int i = 0; i < 120; i++) {
	     //cpiTrades.add(EUR_FIXED_ZC_EU_EXT_CPI.toTrade(trades.get(0).getInfo(),VAL_DATE, VAL_DATE.plusYears(i+1), BuySell.BUY, 1d, 0.02));
	     cpiTrades.add(SwapTrade.of(TradeInfo.of(VAL_DATE),Swap.of(InflationRateSwapLegConvention.of(EU_EXT_CPI,Period.ofMonths(3) ,PriceIndexCalculationMethod.MONTHLY, BusinessDayAdjustment.NONE).toLeg(VAL_DATE, VAL_DATE.plusYears(i+1), PayReceive.RECEIVE, 1d))));
	   }
		 
	  Results results = runner.calculate(rules, cpiTrades, columns,scenarioMarketData.scenario(0), REF_DATA);
	 
	  ReportCalculationResults calculationResults =
		        ReportCalculationResults.of(VAL_DATE, cpiTrades, columns, results, functions, REF_DATA);
	  TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("ils-report-template2_single");
	  //TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("ils-report-template2");
	  //TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("all-cashflow-report-template3");
	  TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
		  try {
			tradeReport.writeCsv(new FileOutputStream("C:\\Users\\M65H036\\Onedrive - NN\\Expense Inflation\\Rproj\\sensOut.csv"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  tradeReport.writeAsciiTable(System.out);
}
	 public static double logNormalprice(
		      double forward,
		      double strike,
		      double timeToExpiry,
		      double lognormalVol,
		      boolean isCall) {

		    ArgChecker.isTrue(forward >= 0d, "negative/NaN forward; have {}", forward);
		    ArgChecker.isTrue(strike >= 0d, "negative/NaN strike; have {}", strike);
		    ArgChecker.isTrue(timeToExpiry >= 0d, "negative/NaN timeToExpiry; have {}", timeToExpiry);
		    ArgChecker.isTrue(lognormalVol >= 0d, "negative/NaN lognormalVol; have {}", lognormalVol);

		    double sigmaRootT = lognormalVol * Math.sqrt(timeToExpiry);
		    if (Double.isNaN(sigmaRootT)) {
		      log.info("lognormalVol * Math.sqrt(timeToExpiry) ambiguous");
		      sigmaRootT = 1d;
		    }
		    int sign = isCall ? 1 : -1;
		    boolean bFwd = (forward > LARGE);
		    boolean bStr = (strike > LARGE);
		    boolean bSigRt = (sigmaRootT > LARGE);
		    double d1 = 0d;
		    double d2 = 0d;

		    if (bFwd && bStr) {
		      log.info("(large value)/(large value) ambiguous");
		      return isCall ? (forward >= strike ? forward : 0d) : (strike >= forward ? strike : 0d);
		    }
		    if (sigmaRootT < SMALL) {
		      return Math.max(sign * (forward - strike), 0d);
		    }
		    if (Math.abs(forward - strike) < SMALL || bSigRt) {
		      d1 = 0.5 * sigmaRootT;
		      d2 = -0.5 * sigmaRootT;
		    } else {
		      d1 = Math.log(forward / (1+strike)) / sigmaRootT + 0.5 * sigmaRootT;
		      d2 = d1 - sigmaRootT;
		    }

		    double nF = NORMAL.getCDF(sign * d1);
		    double nS = NORMAL.getCDF(sign * d2);
		    double first = nF == 0d ? 0d : forward * nF;
		    double second = nS == 0d ? 0d : Math.pow(1+strike,timeToExpiry) * nS;

		    double res = sign * (first - second);
		    return Math.max(0., res);
		  }

   
	 private static final Logger log = LoggerFactory.getLogger(BlackFormulaRepository.class);

	  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);
	  private static final double LARGE = 1e13;
	  private static final double SMALL = 1e-13;
	  public static double priceNormal(double forward, double strike, double timeToExpiry, double normalVol, PutCall putCall) {
		    double sigmaRootT = normalVol * Math.sqrt(timeToExpiry);
		    int sign = putCall.isCall() ? 1 : -1;
		    double arg = sign * (forward - strike) / sigmaRootT;
		    double cdf = NORMAL.getCDF(arg);
		    double pdf = NORMAL.getPDF(arg);
		    return sign * (forward - Math.pow(1+strike,timeToExpiry)) * cdf + sigmaRootT * pdf;
		  }
	 private static  List<Column> columns = ImmutableList.of(
			   
//		     Column.of(Measures.PV01_CALIBRATED_BUCKETED),
//		     Column.of(Measures.PV01_CALIBRATED_SUM),   
			 Column.of(Measures.PV01_MARKET_QUOTE_BUCKETED),
			 Column.of(Measures.CASH_FLOWS),
//			 Column.of(Measures.PV01_MARKET_QUOTE_SUM),
			 Column.of(Measures.PRESENT_VALUE	) 
		        
		        );
		        //Column.of(Measures.CASH_FLOWS), 
		        //Column.of(Measures.UNIT_PRICE
	public static ScenarioMarketData marketdataInfl() throws IOException, ParseException, ScriptException, URISyntaxException {
		
	  ImmutableRatesProvider multicurve = provider();
	  //marketACTIAM(VALUATION_DATE);
	  InflationNodalCurve ind = (InflationNodalCurve) multicurve.getCurves(GROUP_NAME).get(CurveId.of("EUR-DSCON-CPI", "EUR-CPI"));
	  InterpolatedNodalCurve dis = (InterpolatedNodalCurve) multicurve.getCurves(GROUP_NAME).get(CurveId.of("EUR-DSCON-CPI", "EUR-Disc"));
	  //ConstantNodalCurve.of(configs2.get(GROUP_NAME).findCurveDefinition(CurveName.of("EUR-CPI"))., 0, 0);
	  StringBuilder builder = new StringBuilder();
	  //PriceIndexValues values = multicurve.priceIndexValues(EU_EXT_CPI);
	  //values.getIndex().
//	  for (int i = 1; i < 51; ++i) { 
//	      //builder.append(i).append(';').append(ind.yValue(i*12)).append(';').append("\n");
//		  builder.append(i).append(';').append(ind.yValue(i*12)).append(';').append("\n");
//	    }
	 
	  //double dt= 1d/12;
	  PriceIndexValues values = multicurve.priceIndexValues(EU_EXT_CPI);
	    //double indexStart = values.value(computation.getStartObservation());
	    
	  for (int i = -3; i < 30*12; i++) { 
		  LocalDate date = VAL_DATE.plusMonths(i);
	      //builder.append(ind.getXValues().get(i)).append(',').append(ind.getYValues().get(i)).append(',').append("\n");
		  double v = values.value(PriceIndexObservation.of(EU_EXT_CPI, YearMonth.of(date.getYear(), date.getMonth())));
	      builder.append(date).append(',').append(v).append("\n");
	    }
	  System.out.println(builder.toString());
	  ExportUtils.export(builder.toString(), "C:\\Users\\M65H036\\Onedrive - NN\\Expense Inflation\\Rproj\\INFrates.csv");
	  
	  // disc
	  StringBuilder builders = new StringBuilder();
	  //OvernightIndexRates on = multicurve.overnightIndexRates(EUR_EONIA);
	  DiscountFactors dsc = multicurve.discountFactors(EUR);
	  	  for (int i = 0; i < 50; ++i) { 
	  		LocalDate date = VAL_DATE.plusYears(i);
	  		double v = multicurve.discountFactor(Currency.EUR, date);
	      builders.append(date).append(',').append(v).append("\n");
	    }
	  ExportUtils.export(builders.toString(), "C:\\Users\\M65H036\\Onedrive - NN\\Expense Inflation\\Rproj\\Drates.csv");
	 // Curve cu = multicurve.getIndexCurves().get(EU_EXT_CPI).;
	  
	  // Scenario report
	  ImmutableMarketDataBuilder builder1 = ImmutableMarketData.builder(VAL_DATE);
	  multicurve.getCurves().forEach(
	            (ccy, curve) -> builder1.addValue(CurveId.of(GROUP_NAME, curve.getName()), curve));
	  //builder1.addTimeSeries(IndexQuoteId.of(EU_EXT_CPI), TS_EUR_CPI);
	  //builder1.removeValueIf(CurveId.of(GROUP_NAME, curve.getName()))
	  CsvIterator csvFix= CsvIterator.of(ResourceLocator.of("classpath:example-calibration/fixings/fixingsILS.csv").getCharSource(), true);
	  //builder11.addTimeSeries(IndexQuoteId.of(EU_EXT_CPI), TS_EUR_CPI);
	  //builder11.addValue(SwaptionVolatilitiesId.of("SABR"), SABR.swaptionVols(multicurve, VAL_DATE));
	  //LocalDateDoubleTimeSeriesBuilder builderEU = LocalDateDoubleTimeSeries.builder();
	  LocalDateDoubleTimeSeriesBuilder builderFix = LocalDateDoubleTimeSeries.builder();
	  for (CsvRow row : csvFix.asIterable()) {	  
	      String  ref= row.getValue("Reference");
	      LocalDate date = row.getValue("Date", LoaderUtils::parseDate);
	      double value = row.getValue("Value",LoaderUtils::parseDouble);
	      builderFix.put(date, value); ;
	}
	  
	  builder1.addTimeSeries(IndexQuoteId.of(EU_EXT_CPI), builderFix.build());
	  ImmutableMarketData data1 = builder1.build();
	  final ScenarioMarketData MARKET_DATA1 = ScenarioMarketData.of(
		      1,
		      data1);
	  //double shift = 0.1;
	  PerturbationMapping<Curve> mapping = PerturbationMapping.of(
		        //MarketDataFilter.ofName(CurveName.of("EUR-CPI")),
		        //CurveParallelShifts.absolute(0,0,-0.01,0.01)).of(
		        MarketDataFilter.ofName(CurveName.of("EUR-Disc")),
		        CurveParallelShifts.absolute(0, 0.01,-0.01)	);
	  PerturbationMapping<Curve> mappingSpread = PerturbationMapping.of(
		        //MarketDataFilter.ofName(CurveName.of("EUR-CPI")),
		        //CurveParallelShifts.absolute(0,0,-0.01,0.01)).of(
		        MarketDataFilter.ofName(CurveName.of("EUR-Disc")),
		        CurveParallelShifts.absolute(0,-0.000838184403681433 )	);
	  
	  RatesMarketDataLookup ratesLookup = RatesMarketDataLookup.of(configs2.get(GROUP_NAME));
	  CalculationFunctions functions = StandardComponents.calculationFunctions();
	  CalculationRules rules = CalculationRules.of(functions,ratesLookup);
	  
	  //List<Trade> trades = new ArrayList<>();
	  //trades.addAll(trades);
	  
		       
	  MarketDataRequirements reqs = MarketDataRequirements.of(rules, trades, columns, REF_DATA);
	    // create a scenario definition containing the single mapping above
	    // this creates two scenarios - one for each perturbation in the mapping
	  //ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mappingSpread);
	  //ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mapping);
	  ScenarioDefinition scenarioDefinition= ScenarioDefinition.empty();
	  ScenarioMarketData scenarioMarketData =marketDataFactory().createMultiScenario(reqs, MarketDataConfig.empty(), MARKET_DATA1, REF_DATA, scenarioDefinition);
	  return scenarioMarketData;
	}
	public static CurveDefinition lookup() throws IOException, ParseException, ScriptException, URISyntaxException {
		
		return configs2.get(GROUP_NAME).findCurveDefinition(CurveName.of("EUR-CPI")).get();
	}

	
	  
	  
	  
  private static final CurveInterpolator INTERPOLATOR_LINEAR = CurveInterpolators.LINEAR;
  private static final CurveExtrapolator EXTRAPOLATOR_FLAT = CurveExtrapolators.FLAT;
  private static final CurveInterpolator INTERPOLATOR_LOGLINEAR = CurveInterpolators.LOG_LINEAR;
  private static final CurveExtrapolator EXTRAPOLATOR_EXP = CurveExtrapolators.EXPONENTIAL;
  private static final DayCount CURVE_DC = ACT_365F;

  // reference data
  
  private static final String SCHEME = "CALIBRATION";

  /** Curve names */
  private static final String DSCON_NAME = "USD-DSCON-OIS";
  private static final CurveName DSCON_CURVE_NAME = CurveName.of(DSCON_NAME);
  private static final String CPI_NAME = "USD-CPI-ZC";
  private static final CurveName CPI_CURVE_NAME = CurveName.of(CPI_NAME);
  /** Curves associations to currencies and indices. */
  private static final Map<CurveName, Currency> DSC_NAMES = new HashMap<>();
  private static final Map<CurveName, Set<Index>> IDX_NAMES = new HashMap<>();
  private static final LocalDateDoubleTimeSeries TS_USD_CPI =
      LocalDateDoubleTimeSeries.builder().put(LocalDate.of(2020, 4, 30), 104.5).build();
  static {
    DSC_NAMES.put(DSCON_CURVE_NAME, USD);
    Set<Index> usdFedFundSet = new HashSet<>();
    usdFedFundSet.add(USD_FED_FUND);
    IDX_NAMES.put(DSCON_CURVE_NAME, usdFedFundSet);
    Set<Index> usdLibor3Set = new HashSet<>();
    usdLibor3Set.add(USD_LIBOR_3M);
    IDX_NAMES.put(CPI_CURVE_NAME, usdLibor3Set);
  }

  /** Data for USD-DSCON curve */
  /* Market values */
  private static final double[] DSC_MARKET_QUOTES = new double[] {
      0.0005, 0.0005,
      0.00072000, 0.00082000, 0.00093000, 0.00090000, 0.00105000,
      0.00118500, 0.00318650, 0.00318650, 0.00704000, 0.01121500, 0.01515000,
      0.01845500, 0.02111000, 0.02332000, 0.02513500, 0.02668500};
  private static final int DSC_NB_NODES = DSC_MARKET_QUOTES.length;
  private static final String[] DSC_ID_VALUE = new String[] {
      "USD-ON", "USD-TN",
      "USD-OIS-1M", "USD-OIS-2M", "USD-OIS-3M", "USD-OIS-6M", "USD-OIS-9M",
      "USD-OIS-1Y", "USD-OIS-18M", "USD-OIS-2Y", "USD-OIS-3Y", "USD-OIS-4Y", "USD-OIS-5Y",
      "USD-OIS-6Y", "USD-OIS-7Y", "USD-OIS-8Y", "USD-OIS-9Y", "USD-OIS-10Y"};
  /* Nodes */
  private static final CurveNode[] DSC_NODES = new CurveNode[DSC_NB_NODES];
  /* Tenors */
  private static final int[] DSC_DEPO_OFFSET = new int[] {0, 1};
  private static final int DSC_NB_DEPO_NODES = DSC_DEPO_OFFSET.length;
  private static final Period[] DSC_OIS_TENORS = new Period[] {
      Period.ofMonths(1), Period.ofMonths(2), Period.ofMonths(3), Period.ofMonths(6), Period.ofMonths(9),
      Period.ofYears(1), Period.ofMonths(18), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5),
      Period.ofYears(6), Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10)};
  private static final int DSC_NB_OIS_NODES = DSC_OIS_TENORS.length;
  static {
    for (int i = 0; i < DSC_NB_DEPO_NODES; i++) {
      BusinessDayAdjustment bda = BusinessDayAdjustment.of(FOLLOWING, USNY);
      TermDepositConvention convention =
          ImmutableTermDepositConvention.of(
              "USD-Dep", USD, bda, ACT_360, DaysAdjustment.ofBusinessDays(DSC_DEPO_OFFSET[i], USNY));
      DSC_NODES[i] = TermDepositCurveNode.of(TermDepositTemplate.of(Period.ofDays(1), convention),
          QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])));
    }
    for (int i = 0; i < DSC_NB_OIS_NODES; i++) {
      DSC_NODES[DSC_NB_DEPO_NODES + i] = FixedOvernightSwapCurveNode.of(
          FixedOvernightSwapTemplate.of(Period.ZERO, Tenor.of(DSC_OIS_TENORS[i]), USD_FIXED_1Y_FED_FUND_OIS),
          QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[DSC_NB_DEPO_NODES + i])));
    }
  }

  /** Data for USD-CPI curve */
  /* Market values */
  private static final double[] CPI_MARKET_QUOTES = new double[] {
      0.0200, 0.0200, 0.0200, 0.0200, 0.0200};
  private static final int CPI_NB_NODES = CPI_MARKET_QUOTES.length;
  private static final String[] CPI_ID_VALUE = new String[] {
      "USD-CPI-1Y", "USD-CPI-2Y", "USD-CPI-3Y", "USD-CPI-4Y", "USD-CPI-5Y"};
  /* Nodes */
  private static final CurveNode[] CPI_NODES = new CurveNode[CPI_NB_NODES];
  /* Tenors */
  private static final Period[] CPI_TENORS = new Period[] {
      Period.ofYears(1), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5)};
  static {
    for (int i = 0; i < CPI_NB_NODES; i++) {
      CPI_NODES[i] = FixedInflationSwapCurveNode.builder()
          .template(FixedInflationSwapTemplate.of(Tenor.of(CPI_TENORS[i]), FixedInflationSwapConventions.USD_FIXED_ZC_US_CPI))
          .rateId(QuoteId.of(StandardId.of(SCHEME, CPI_ID_VALUE[i])))
          .date(CurveNodeDate.LAST_FIXING)
          .build();
    }
  }

  /** All quotes for the curve calibration */
  private static final ImmutableMarketData ALL_QUOTES;
  static {
    ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VAL_DATE);
    for (int i = 0; i < DSC_NB_NODES; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])), DSC_MARKET_QUOTES[i]);
    }
    for (int i = 0; i < CPI_NB_NODES; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, CPI_ID_VALUE[i])), CPI_MARKET_QUOTES[i]);
    }
    builder.addTimeSeries(IndexQuoteId.of(US_CPI_U), TS_USD_CPI);
    ALL_QUOTES = builder.build();
  }

  /** All nodes by groups. */
  private static final List<List<CurveNode[]>> CURVES_NODES = new ArrayList<>();
  static {
    List<CurveNode[]> groupDsc = new ArrayList<>();
    groupDsc.add(DSC_NODES);
    CURVES_NODES.add(groupDsc);
    List<CurveNode[]> groupCpi = new ArrayList<>();
    groupCpi.add(CPI_NODES);
    CURVES_NODES.add(groupCpi);
  }

  private static final DiscountingSwapProductPricer SWAP_PRICER =
      DiscountingSwapProductPricer.DEFAULT;
  private static final DiscountingTermDepositProductPricer DEPO_PRICER =
      DiscountingTermDepositProductPricer.DEFAULT;

  private static final RatesCurveCalibrator CALIBRATOR = RatesCurveCalibrator.of(1e-9, 1e-9, 100);

  // Constants
  private static final double TOLERANCE_PV = 1.0E-6;

  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("USD-DSCON-LIBOR3M");
  private static final InterpolatedNodalCurveDefinition DSC_CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(DSCON_CURVE_NAME)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_FLAT)
          .extrapolatorRight(EXTRAPOLATOR_FLAT)
          .nodes(DSC_NODES).build();
  private static final InterpolatedNodalCurveDefinition CPI_CURVE_UNDER_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(CPI_CURVE_NAME)
          .xValueType(ValueType.MONTHS)
          .yValueType(ValueType.PRICE_INDEX)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LOGLINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_FLAT)
          .extrapolatorRight(EXTRAPOLATOR_EXP)
          .nodes(CPI_NODES).build();
  private static final RatesCurveGroupDefinition CURVE_GROUP_CONFIG =
      RatesCurveGroupDefinition.builder()
          .name(CURVE_GROUP_NAME)
          .addCurve(DSC_CURVE_DEFN, USD, USD_FED_FUND)
          .addForwardCurve(CPI_CURVE_UNDER_DEFN, US_CPI_U).build();

  //-------------------------------------------------------------------------
//  @Test
//  public void calibration_present_value_oneGroup() {
//    RatesProvider result = CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, ALL_QUOTES, REF_DATA);
//    assertPresentValue(result);
//  }
//
//  private void assertPresentValue(RatesProvider result) {
//    // Test PV Dsc
//    CurveNode[] dscNodes = CURVES_NODES.get(0).get(0);
//    List<ResolvedTrade> dscTrades = new ArrayList<>();
//    for (int i = 0; i < dscNodes.length; i++) {
//      dscTrades.add(dscNodes[i].resolvedTrade(1d, ALL_QUOTES, REF_DATA));
//    }
//    // Depo
//    for (int i = 0; i < DSC_NB_DEPO_NODES; i++) {
//      CurrencyAmount pvIrs = DEPO_PRICER.presentValue(
//          ((ResolvedTermDepositTrade) dscTrades.get(i)).getProduct(), result);
//      assertThat(pvIrs.getAmount()).isCloseTo(0.0, offset(TOLERANCE_PV));
//    }
//    // OIS
//    for (int i = 0; i < DSC_NB_OIS_NODES; i++) {
//      MultiCurrencyAmount pvIrs = SWAP_PRICER.presentValue(
//          ((ResolvedSwapTrade) dscTrades.get(DSC_NB_DEPO_NODES + i)).getProduct(), result);
//      assertThat(pvIrs.getAmount(USD).getAmount()).isCloseTo(0.0, offset(TOLERANCE_PV));
//    }
//    // Test PV Infaltion swaps
//    CurveNode[] cpiNodes = CURVES_NODES.get(1).get(0);
//    List<ResolvedTrade> cpiTrades = new ArrayList<>();
//    for (int i = 0; i < cpiNodes.length; i++) {
//      cpiTrades.add(cpiNodes[i].resolvedTrade(1d, ALL_QUOTES, REF_DATA));
//    }
//    // ZC swaps
//    for (int i = 0; i < CPI_NB_NODES; i++) {
//      MultiCurrencyAmount pvInfl = SWAP_PRICER.presentValue(
//          ((ResolvedSwapTrade) cpiTrades.get(i)).getProduct(), result);
//      assertThat(pvInfl.getAmount(USD).getAmount()).isCloseTo(0.0, offset(TOLERANCE_PV));
//    }
//  }
//
//  //-------------------------------------------------------------------------
//  @SuppressWarnings("unused")
//  @Disabled
//  public void performance() {
//    long startTime, endTime;
//    int nbTests = 100;
//    int nbRep = 3;
//    int count = 0;
//
//    for (int i = 0; i < nbRep; i++) {
//      startTime = System.currentTimeMillis();
//      for (int looprep = 0; looprep < nbTests; looprep++) {
//        RatesProvider result = CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, ALL_QUOTES, REF_DATA);
//        count += result.getValuationDate().getDayOfMonth();
//      }
//      endTime = System.currentTimeMillis();
//      System.out.println("Performance: " + nbTests + " calibrations for 2 curves with 35 nodes in "
//          + (endTime - startTime) + " ms.");
//    }
//    System.out.println("Avoiding hotspot: " + count);
//    // Previous run: 275 ms for 100 calibrations (2 curves simultaneous - 35 nodes)
//  }

}