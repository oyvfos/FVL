package loader;
/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */


import static com.opengamma.strata.loader.csv.CsvLoaderColumns.CURRENCY_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.DIRECTION_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_CAL_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.PAYMENT_DATE_CNV_FIELD;
import static com.opengamma.strata.loader.csv.CsvLoaderColumns.START_DATE_FIELD;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.loader.csv.TradeCsvInfoResolver;
import com.opengamma.strata.loader.csv.TradeCsvParserPlugin;
//import com.opengamma.strata.loader.csv.TradeTypeCsvWriter;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;

import pricer.StochasticPIDEComputation;
import product.Policy;
import product.PolicyTrade;
import product.PolicyConvention;

/**
 * Handles the CSV file format for Bullet Payment trades.
 */
final class PolicyTradeCsvPlugin implements TradeCsvParserPlugin {
	
  /**
   * The singleton instance of the plugin.
   */
  public static final PolicyTradeCsvPlugin INSTANCE = new PolicyTradeCsvPlugin();



  /** The headers. */
  private static final ImmutableList<String> HEADERS = ImmutableList.<String>builder()
      .add(DIRECTION_FIELD)
      .add(CURRENCY_FIELD)
      .add(NOTIONAL_FIELD)
      .add(START_DATE_FIELD)
      .add(END_DATE_FIELD)
      .add(PAYMENT_DATE_CNV_FIELD)
      .add(PAYMENT_DATE_CAL_FIELD)
      .build();

  //-------------------------------------------------------------------------
  @Override
  public Set<String> tradeTypeNames() {
    return ImmutableSet.of("POLICY");
  }

  @Override
  public Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(PolicyTrade.class)) {
    	//return Optional.of(resolver.parsePolicyTrade(baseRow, info));
    	return Optional.of(PolicyTradeCsvPlugin.parseRow(baseRow, info,resolver));
    }
   return Optional.empty();
  }

  @Override
  public String getName() {
    return "Policy";
  }

  //-------------------------------------------------------------------------
  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the trade info
   * @param resolver  the resolver used to parse additional information
   * @return the parsed trade
   */
//  static PolicyTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
//    PolicyTrade trade = parseRow(row, info, resolver);
//    return resolver.completeTrade(row, trade);
//  }

  // parse the row to a trade
  private static PolicyTrade parseRow(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
	  Boolean male = row.getValue("MALE",LoaderUtils::parseBoolean);
		LocalDate birthDate = row.getValue("DATE_OF_BIRTH",LoaderUtils::parseDate);
		LocalDate expiryDate = row.getValue("EXPIRY_DATE",LoaderUtils::parseDate);
		Double investementAccount = row.getValue("INV_ACC",LoaderUtils::parseDouble);
		Double investementAccountProxy = row.getValue("INV_ACC_PROXY",LoaderUtils::parseDouble);
		Double expenseRateinvestementAccount = row.getValue("INV_ACC_EXP_RATE",LoaderUtils::parseDouble);
		Double rateInvestementAccountGuaranteed = row.getValue("INV_ACC_RATE_GUAR",LoaderUtils::parseDouble);
		Double mortalityRestitution = row.getValue("MORT_REST",LoaderUtils::parseDouble);
		Currency currency= row.getValue("CURRENCY",LoaderUtils::parseCurrency);
		String tarifId= row.getValue("TARIF_ID");
		String convention= row.getValue("CONVENTION");
    Policy payment = Policy.builder()
    .male(male)
    .birthDate(birthDate)
    .expiryDate(expiryDate)
 
    .investementAccount(investementAccount)
    .investementAccountProxy(investementAccountProxy)
    .expenseRateinvestementAccount(expenseRateinvestementAccount)
 
    .rateInvestementAccountGuaranteed(rateInvestementAccountGuaranteed)
    .mortalityRestitution(mortalityRestitution)
    .currency(currency)
    .tarifId(tarifId)
    .convention(PolicyConvention.of(convention))
    .build();
    //PolicyConvention conventionP = PolicyConvention.of(convention);    
    return PolicyTrade.of(info, payment);
  }

  //-------------------------------------------------------------------------
  public List<String> headers(List<PolicyTrade> trades) {
    return HEADERS;
  }

//  @Override
//  public void writeCsv(CsvRowOutputWithHeaders csv, FixedCouponBondTrade trade) {
//    FixedCouponBond product = trade.getProduct();
//    csv.writeCell(TRADE_TYPE_FIELD, "FixedCouponBond");
//    csv.writeCell(DIRECTION_FIELD, product.getPayReceive());
//    csv.writeCell(CURRENCY_FIELD, product.getValue().getCurrency());
//    csv.writeCell(NOTIONAL_FIELD, product.getValue().getAmount());
//    csv.writeCell(PAYMENT_DATE_FIELD, product.getDate().getUnadjusted());
//    csv.writeCell(PAYMENT_DATE_CAL_FIELD, product.getDate().getAdjustment().getCalendar());
//    csv.writeCell(PAYMENT_DATE_CNV_FIELD, product.getDate().getAdjustment().getConvention());
//    csv.writeNewLine();
//  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private PolicyTradeCsvPlugin() {
  }

}


