/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package mavenBlue;

import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.renjin.sexp.ListVector;
import org.renjin.sexp.Vector;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.examples.marketdata.ExampleData;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.measure.AdvancedMeasures;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.swaption.SwaptionMarketDataLookup;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.swaption.SabrParametersSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.CashFlowPayments.CashFlowPayments;
import com.opengamma.strata.product.CashFlowPayments.CashFlowPaymentsTrade;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.bond.FixedCouponBondTrade;
import com.opengamma.strata.product.bond.FixedCouponBondYieldConvention;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionTrade;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Example to illustrate using the calculation API to price a swap.
 * <p>
 * This makes use of the example market data environment.
 */

public class SwapPricingALMvalidation {
	private static final StandardId LEGAL_ENTITY = StandardId.of("EUR-DSCON-OIS", "GOVT1");
	private static final StandardId LEGAL_ENTITY_V = StandardId.of("EIOPA", "VIVAT");
	private static final CurveId ISSUER_CURVE_ID_V = CurveId.of("VIVAT","EIOPA");
	private static final CurveId REPO_CURVE_ID_V = CurveId.of( "LIABILITIES","OG-Ticker");
	private static final CurveId ISSUER_CURVE_ID = CurveId.of("GOVT1","EUR-DSCON-OIS");
	private static final CurveId REPO_CURVE_ID = CurveId.of( "GOVT1 BOND1","OG-Ticker");
	private static final RepoGroup GROUP_REPO_V = RepoGroup.of("LIABILITIES");
	private static final LegalEntityGroup GROUP_ISSUER_V = LegalEntityGroup.of("VIVAT");
	private static final RepoGroup GROUP_REPO = RepoGroup.of("GOVT1 BOND1");
	private static final LegalEntityGroup GROUP_ISSUER = LegalEntityGroup.of("GOVT1");
	private static final String CONFIG_STR = "EUR-DSCONOIS-EURIBOR3MBS-EURIBOR6MIRS";
	 private static final CurveGroupName GROUP_NAME = CurveGroupName.of(CONFIG_STR);
	    
  /**
   * Runs the example, pricing the instruments, producing the output as an ASCII table.
   * 
   * @param args  ignored
 * @throws IOException 
 * @throws ParseExceptionddd 
   */
  public static void main(String[] args) throws IOException, ParseException {
    // setup calculation runner component, which needs life-cycle management
    // a typical application might use dependency injection to obtain the instance
	  
    try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
    	LocalDate valuationDate = LocalDate.of(2018, 9, 28);
    	LocalDate rateDate = LocalDate.of(2018, 9, 28);
    	//LocalDate rateDate = LocalDate.of(2010, 12, 31);
    	//LocalDate rateDate = LocalDate.of(2011, 12, 30);
      calculate(runner,rateDate,valuationDate);
      
    }
  }
  // obtains the data and calculates the grid of results
  private static void calculate(CalculationRunner runner, LocalDate rateDate, LocalDate valuationDate) throws IOException, ParseException {
    // the trades that will have measures calculated
	  
	  boolean liabilities = false;
	  boolean cashflow = false;
	  List<Trade> trades = new ArrayList<>();
	  if (!liabilities) {
	  //trades.addAll(createBondTrades(valuationDate));
		  trades.addAll(createSwaps(valuationDate));
		 //trades.addAll(createSwaptions(valuationDate));	  
	  } else 
	  trades.addAll(createLiabilitiesTrades(valuationDate));
	  
    //trades.add(TRADE);

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PV01_MARKET_QUOTE_BUCKETED),
        Column.of(Measures.PRESENT_VALUE),
        Column.of(Measures.CASH_FLOWS),
        Column.of(Measures.LEG_PRESENT_VALUE),
        Column.of(Measures.PV01_CALIBRATED_SUM),
        Column.of(Measures.PAR_RATE),
        Column.of(Measures.ACCRUED_INTEREST),
        Column.of(Measures.PV01_CALIBRATED_BUCKETED),
        
        Column.of(AdvancedMeasures.PV01_SEMI_PARALLEL_GAMMA_BUCKETED));

    // use the built-in example market data
    
    //ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();
    
   
    CalculationFunctions functions = StandardComponentsOld.calculationFunctions();
    // dual curves
    ImmutableRatesProvider multicurve = EIOPA.market(rateDate);
    
    SabrParametersSwaptionVolatilities swaptionvols = EIOPA.swaptionVols(multicurve, rateDate);
    ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(valuationDate);
	
    ImmutableMarketDataBuilder buildereiopa = ImmutableMarketData.builder(valuationDate);
	 multicurve.getDiscountCurves().forEach(
	            (ccy, curve) -> builder.addValue(CurveId.of(GROUP_NAME, curve.getName()), curve));
	 multicurve.getIndexCurves().forEach(
	            (idx, curve) -> builder.addValue(CurveId.of(GROUP_NAME, curve.getName()), curve));
	
	 /*eiopa.getIndexCurves().forEach(
	            (idx, curve) -> buildereiopa.addValue(CurveId.of(GROUP_NAME, curve.getName()), curve));*/
	 builder.addValue(SwaptionVolatilitiesId.of("EUR1"), swaptionvols);
	 //RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(Currency.of("EUR"), CurveId.of(GROUP_NAME, CurveName.of("EUR-DSCON-OIS")));
	 //SwaptionMarketDataLookup SWAPTION_LOOKUP = SwaptionMarketDataLookup.of(IborIndices.EUR_EURIBOR_6M, SwaptionVolatilitiesId.of("EUR1"));
	 //CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP, SWAPTION_LOOKUP);
	 
	 Map<Currency, CurveName> map = new HashMap<Currency,CurveName>();
	 Map<Index, CurveName> map1 = new HashMap<Index,CurveName>();
	 Map<Currency, CurveName> map2 = new HashMap<Currency,CurveName>();
	 Map<Index, CurveName> map3 = new HashMap<Index,CurveName>();
	 multicurve.getDiscountCurves().forEach(
	            (ccy, curve) -> map.put(ccy, curve.getName()));
	 multicurve.getIndexCurves().forEach(
	            (idx, curve) -> map1.put(idx, curve.getName()));
	

    
    // discounting for bonds
    
	 //multi curve https://quant.stackexchange.com/questions/14567/what-is-the-swap-curve
    ImmutableMarketData marketdata = builder.build();
    
    
    
  //Link curves
    
    CalculationRules rules = CalculationRules.of(functions, RatesMarketDataLookup.of(GROUP_NAME,map,map1), SwaptionMarketDataLookup.of(IborIndices.EUR_EURIBOR_6M, SwaptionVolatilitiesId.of("EUR1")));
    
    // the reference data, such as holidays and securities
    ReferenceData refData = ReferenceData.standard();

    // calculate the results
    Results results;
    results = runner.calculate(rules, trades, columns, marketdata, refData);
    
    // use the report runner to transform the engine results into a trade report
    ReportCalculationResults calculationResults =
        ReportCalculationResults.of(valuationDate, trades, columns, results, functions, refData);
    //ExportUtils.export(results.getCells().toString(), "H:/Mijn Documenten/Ad hoc/SA/cashflows.csv");
    TradeReportTemplate reportTemplate = null ;
    if (!liabilities && cashflow) reportTemplate = ExampleData.loadTradeReportTemplate("all-cashflow-report-template2"); 
    	else if (!liabilities && !cashflow) reportTemplate = ExampleData.loadTradeReportTemplate("swap-report-template");
    	else if (liabilities && !cashflow) reportTemplate = ExampleData.loadTradeReportTemplate("swap-report-template_lia");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
    tradeReport.writeAsciiTable(System.out);
    if (!liabilities && cashflow) tradeReport.writeCsv(new FileOutputStream("H:/Mijn Documenten/Ad hoc/SA/out_cf.csv")); 
	else if (!liabilities && !cashflow) tradeReport.writeCsv(new FileOutputStream("H:/Mijn Documenten/Ad hoc/SA/out"+rateDate.toString() +".csv"));
	else if (liabilities && !cashflow) tradeReport.writeCsv(new FileOutputStream("H:/Mijn Documenten/Ad hoc/SA/out"+rateDate.toString() +"_liabilities.csv"));
    
  }
  final static ReferenceData refData = ReferenceData.standard();
  //-----------------------------------------------------------------------  
  // create swaption trades
  private static List<Trade> createSwaptions(LocalDate VALUATIONDATE) throws IOException, ParseException {
	  List<Trade> trades = new ArrayList<>(); 
	  
	  ListVector r= (ListVector) RWorkSpace.get("H:/Mijn Documenten/Ad hoc/SA/swaptions1.RData","sw");
		Vector rownames = (Vector) r.getAttributes().get("row.names");
		for(int i=0;i<rownames.length();i+=1) { //r.length()
		   long start, end;
		   
		    // Swaption description
		    BuySell payer = BuySell.BUY;
		    //Period expiry = Period.ofMonths(18);
		    
		    double notional = r.get("NominalReportingCurrency").getElementAsSEXP(i).asInt();
		    double strike = r.get("Strike").getElementAsSEXP(i).asReal()/100;
		    LongShort longshort= (r.get("LongShortIndicator").getElementAsSEXP(i).asInt()==1) ? LongShort.LONG : LongShort.SHORT;
		    Double s= r.get("Tenor").getElementAsSEXP(i).asReal();
		    Tenor tenor = Tenor.ofYears(s.intValue());
		    
		    long millis = (long) r.get("ExpiryDate").getElementAsSEXP(i).asInt() * 1000;
		    
		    //DateTime date = new DateTime(millis);
		    LocalDate expiryDate = Instant.ofEpochMilli(millis)
				      .atZone(ZoneId.systemDefault())
				      .toLocalDate();
		    SwapTrade underlying = EUR_FIXED_1Y_EURIBOR_6M.createTrade(expiryDate, tenor, payer, notional, strike, refData);
		    Swaption swaption = Swaption.builder().expiryDate(AdjustableDate.of(expiryDate)).expiryTime(LocalTime.of(11, 00))
		        .expiryZone(ZoneId.of("Europe/Berlin")).underlying(underlying.getProduct()).longShort(longshort)
		        .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT).build();
		    String SEC_ID=r.get("SecurityId").getElementAsSEXP(i).asString();
		    TradeInfo tradeInfo = TradeInfo.builder()
		            .id(StandardId.of("1",SEC_ID))
		            .addAttribute(TradeAttributeType.DESCRIPTION, SEC_ID)
		            .counterparty(StandardId.of("example", "A"))
		            .build();
		    SwaptionTrade trade = SwaptionTrade.of(tradeInfo, swaption, AdjustablePayment.of(CurrencyAmount.of(Currency.EUR, -3050000d), VALUATIONDATE));
		    //ResolvedSwaption resolvedSwaption = swaption.resolve(refData);
		    trades.add(trade);
		}
	    return trades;
  }
//create swap trades
  private static List<Trade> createSwaps(LocalDate VALUATIONDATE) throws IOException, ParseException {
	  LocalDate settle = VALUATIONDATE.plusDays(4);
	List<Trade> trades = new ArrayList<>();	  
    ListVector r= (ListVector) RWorkSpace.get("H:/Mijn Documenten/Ad hoc/SA/swaps.RData","sw");
	Vector rownames = (Vector) r.getAttributes().get("row.names");
	//ImmutableRatesProvider multicurve = RatesLoader.run(VALUATIONDATE);
	//DiscountingSwapTradePricer PRICER_SWAP = DiscountingSwapTradePricer.DEFAULT;
  //final List<FixedCouponBondTrade> trades = new ArrayList<>();
	for(int i=0;i<rownames.length();i+=2) { //r.length()
	 //Fixed - 1st leg
	 //floating - 2st leg
	  //System.out.println(r.get("Fondscode").getElementAsSEXP(i).asString());
		long millis = (long) r.get("MaturityDate").getElementAsSEXP(i).asInt() * 1000;
	    
	    LocalDate END_DATE = Instant.ofEpochMilli(millis)
			      .atZone(ZoneId.systemDefault())
			      .toLocalDate();
	  
	  //LocalDate START_DATE = LocalDate.ofEpochDay(r.get("ISSUEDATE").getElementAsSEXP(i).asInt());
	  String SEC_ID=r.get("SecurityId").getElementAsSEXP(i).asString();
	  //LocalDate PREV_COUPON_DATE = LocalDate.ofEpochDay(r.get("Previous Coupon date").getElementAsSEXP(i).asInt());
	    double fixedRate = r.get("Coupon").getElementAsSEXP(i).asReal()/100;
	    //BuySell buySell= (r.get("NOMINAAL_EUR").getElementAsSEXP(i).asReal()>0) ? BuySell.SELL:BuySell.BUY;
	    TradeInfo tradeInfo = TradeInfo.builder()
	            .id(StandardId.of("1",SEC_ID))
	            .addAttribute(TradeAttributeType.DESCRIPTION, SEC_ID)
	            .counterparty(StandardId.of("example", "A"))
	            .settlementDate(settle)
	            .build();//isBefore(VALUATIONDATE) ? VALUATIONDATE.plusDays(4):START_DATE.plusDays(4) 
	    Trade SWAP_TRADE = EUR_FIXED_1Y_EURIBOR_6M.toTrade(tradeInfo,settle,END_DATE, BuySell.SELL ,1 , fixedRate);
	    trades.add(SWAP_TRADE);
	    //CurrencyAmount pv = PRICER_SWAP.getProductPricer().presentValue(EUR_FIXED_1Y_EURIBOR_6M.toTrade(tradeInfo,VALUATIONDATE.plusDays(4),END_DATE, buySell ,1 , fixedRate).resolve(refData), Currency.EUR, multicurve);
	    //System.out.println(pv.getAmount());
	}
	return trades;
	}
//bb bond trades
 public static List<Trade> createBondTrades(LocalDate VALUATIONDATE) throws IOException, ParseException {
	   List<Trade> trades = new ArrayList<>();
		ListVector r= (ListVector) RWorkSpace.get("H:/Mijn Documenten/Ad hoc/SA/bondsdata.RData","bd");
		Vector rownames = (Vector) r.getAttributes().get("row.names");
		ReferenceData REF_DATA = ReferenceData.standard();
		LocalDate settle = VALUATIONDATE.plusDays(4);
		//final List<ResolvedFixedCouponBond> bonds = new ArrayList<>();
		for(int i=0;i<rownames.length();i+=1) { //r.length()
			LocalDate END_DATE = LocalDate.ofEpochDay(r.get("MATURITY").getElementAsSEXP(i).asInt());
			LocalDate START_DATE = LocalDate.ofEpochDay(r.get("ISSUE_DT").getElementAsSEXP(i).asInt());
			LocalDate FIRST_COUPON = LocalDate.ofEpochDay(r.get("FIRST_CPN_DT").getElementAsSEXP(i).asInt());
			//LocalDate START_ACC = LocalDate.ofEpochDay(r.get("START_ACC_DT").getElementAsSEXP(i).asInt());
			String SEC_ID=r.get("X__1").getElementAsSEXP(i).asString();
			double COUPON = r.get("CPN").getElementAsSEXP(i).asReal()/100;
			int cuplen=r.get("COUPONTERMLENGTH").getElementAsSEXP(i).asInt();
			//Frequency frequency =?COUPON=0 Frequency.TERM :Frequency.P12M;
			Frequency freq;
			LocalDate fc;
			//if (cuplen==1) freq= Frequency.P12M; else if () freq= Frequency.TERM;
			
			if (FIRST_COUPON.getYear()<0) FIRST_COUPON= START_DATE;
			if (cuplen<3 ) {
			//if (cuplen==0 ) {
			FixedCouponBondTrade TRADE = FixedCouponBondTrade.builder()
				      .product(FixedCouponBond.builder()
								 .currency(Currency.EUR)
								 .securityId(SecurityId.of("ISIN", "SEC_ID"))
								 .dayCount(DayCounts.ACT_ACT_ICMA)
								 .fixedRate(COUPON)
								 .notional(1)
								 .accrualSchedule(
								 PeriodicSchedule.builder()
								 .startDate(START_DATE)
								 .endDate(END_DATE)
								 .firstRegularStartDate(FIRST_COUPON)
								 //.firstRegularStartDate(START_ACC)
								 .frequency(Frequency.P12M)
								 .businessDayAdjustment(BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, HolidayCalendarIds.EUTA))
								 .build()
								 )
								 .yieldConvention(FixedCouponBondYieldConvention.DE_BONDS)
								 .legalEntityId(StandardId.of("EUR-DSCON-OIS", "GOVT1"))
								 .settlementDateOffset(DaysAdjustment.ofBusinessDays(2, HolidayCalendarIds.EUTA))
								 .exCouponPeriod(DaysAdjustment.NONE)
								 .build())
				      //.price(1.45259)
				      .quantity(1)
				      .info(TradeInfo.builder()
				  	        .id(StandardId.of("EUR-DSCON-OIS", SEC_ID.substring(0, 12)))
					        .addAttribute(TradeAttributeType.DESCRIPTION, "Bondje")
					        .counterparty(StandardId.of("EUR-DSCON-OIS", "GOVT1"))
					        .settlementDate(settle)
					        .build())
				      .build();
			trades.add(TRADE);
			}
		}
		return trades;
 }
 

  //Liabilities 'trades'
  public static List<Trade> createLiabilitiesTrades(LocalDate VALUATIONDATE) throws IOException, ParseException {
	  LocalDate settle = VALUATIONDATE.plusDays(4);
	    //DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyyMMdd").toFormatter(Locale.ENGLISH); 
	    //NumberFormat NUMFORMAT = NumberFormat.getInstance(Locale.FRENCH);
	   List<Trade> trades = new ArrayList<>();
		ListVector r= (ListVector) RWorkSpace.get("H:/Mijn Documenten/Ad hoc/SA/SACashflow.RData","d1");
		Vector rownames = (Vector) r.getAttributes().get("row.names");
		Vector names = (Vector) r.getAttributes().get("names");
		
		ReferenceData REF_DATA = ReferenceData.standard();
		
		for(int j=0;j<r.length();++j) { 
		Vector cfs = (Vector) r.get(j);
		final List<BulletPayment> cfBonds = new ArrayList<>();
		// Columns with only zeros
		if (cfs.getElementAsDouble(1)>0) {
		
		//System.out.println("SA " + (j+1));
		for(int i=0;i<rownames.length();++i) { //r.length()
		    
		if (cfs.getElementAsDouble(i) > 0) {
	   //for (int i = 0; i < csv.rowCount(); i+=1) { //
	      // product
		   
			BulletPayment payment = BulletPayment.builder().payReceive(PayReceive.PAY)
		      .value(CurrencyAmount.of(Currency.EUR,cfs.getElementAsDouble(i)))
		      .date(AdjustableDate.of(VALUATIONDATE.plusMonths(DoubleMath.roundToLong(rownames.getElementAsDouble(i)*12,RoundingMode.DOWN))))
		      .build();
			  //      
		       
		      cfBonds.add(payment);
		  
		   }
		}
		
		TradeInfo tradeInfo = TradeInfo.builder()
			        .id(StandardId.of("EIOPA", "VIVAT LIABILITY"))
			        .addAttribute(TradeAttributeType.DESCRIPTION, "SA " +names.getElementAsInt(j))
			        .counterparty(StandardId.of("EIOPA", "VIVAT"))
			        .settlementDate(settle)
			        .build();
		Trade CF = CashFlowPaymentsTrade.of(
			      tradeInfo,CashFlowPayments.of(cfBonds));
		trades.add(CF);
		}
		}
	   	
		return  trades;
  }
 
  //-----------------------------------------------------------------------  
 
}
