package product;

/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;
import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataId;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.product.TradeConvention;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.type.ImmutableFixedOvernightSwapConvention;

import liabilities.DifferentiationMatrix;
import liabilities.DifferentiationMatrixId;
import utilities.Taylor;

/**
 * A market convention for Fixed-Overnight swap trades.
 * <p>
 * This defines the market convention for a Fixed-Overnight single currency swap.
 * This is often known as an <i>OIS swap</i>, although <i>Fed Fund swaps</i> are also covered.
 * The convention is formed by combining two swap leg conventions in the same currency.
 * <p>
 * To manually create a convention, see {@link ImmutableFixedOvernightSwapConvention}.
 * To register a specific convention, see {@code FixedOvernightSwapConvention.ini}.
 */
public interface PolicyConvention
    extends TradeConvention, Named {

  /**
   * convention used for information specific for a group of policies 
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static PolicyConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the convention to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<PolicyConvention> extendedEnum() {
	    return PolicyConventions.ENUM_LOOKUP;
	  }

  //-----------------------------------------------------------------------
  
  //-------------------------------------------------------------------------
  /**
   * Creates a spot-starting trade based on this convention.
   * <p>
   * This returns a trade based on the specified tenor. For example, a tenor
   * of 5 years creates a swap starting on the spot date and maturing 5 years later.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the swap, the floating rate is paid to the counterparty, with the fixed rate being received.
   * 
   * @param tradeDate  the date of the trade
   * @param tenor  the tenor of the swap
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param fixedRate  the fixed rate, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
 * @return 
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
 
  public static PolicyTrade createTrade(
      LocalDate tradeDate,
      Tenor tenor,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData) {

    // override for Javadoc
    return PolicyConvention.createTrade(tradeDate, tenor, buySell, notional, fixedRate, refData);
  }
  
  
  /**
   * Creates a forward-starting trade based on this convention.
   * <p>
   * This returns a trade based on the specified period and tenor. For example, a period of
   * 3 months and a tenor of 5 years creates a swap starting three months after the spot date
   * and maturing 5 years later.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the swap, the floating rate is paid to the counterparty, with the fixed rate being received.
   * 
   * @param tradeDate  the date of the trade
   * @param periodToStart  the period between the spot date and the start date
   * @param tenor  the tenor of the swap
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param fixedRate  the fixed rate, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  
//  public static PolicyTrade createTrade(
//		  TradeInfo info, Policy policy, ReferenceData referenceData) {
//
//    // override for Javadoc
//    return PolicyConvention.createTrade(info, policy.toBuilder().calcMethod(StochasticPIDEComputation.of(0)).build(), referenceData);
//  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the swap, the floating rate is paid to the counterparty, with the fixed rate being received.
 * @param referenceData 
   * 
   * @param tradeDate  the date of the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param fixedRate  the fixed rate, typically derived from the market
   * @return the trade
   */
  
//  public default PolicyTrade toTrade(TradeInfo info, Policy policy, ReferenceData referenceData) {
//       
//    // override for Javadoc
//	 return  PolicyConvention.createTrade(info, policy.toBuilder().calcMethod(StochasticPIDEComputation.of(0)).build(), referenceData);
//    //return PolicyTrade.of(info, policy.toBuilder().calcMethod(StochasticPIDEComputation.of(0)).build());
//  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the swap, the floating rate is paid to the counterparty, with the fixed rate being received.
   * 
   * @param tradeInfo  additional information about the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param fixedRate  the fixed rate, typically derived from the market
   * @return the trade
   */
  
//  public abstract PolicyTrade toTrade(
//      TradeInfo tradeInfo,
//      LocalDate startDate,
//      LocalDate endDate,
//      BuySell buySell,
//      double notional,
//      double fixedRate);

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this convention.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
