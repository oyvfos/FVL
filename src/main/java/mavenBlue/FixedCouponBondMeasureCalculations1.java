package mavenBlue;

import com.google.common.base.Optional;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.measure.bond.LegalEntityDiscountingScenarioMarketData;
import com.opengamma.strata.pricer.CompoundedRateType;
import com.opengamma.strata.pricer.bond.DiscountingFixedCouponBondProductPricer;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.SecuritizedProductTrade;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.bond.FixedCouponBondTrade;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBondTrade;

/**
 * Multi-scenario measure calculations for fixed coupon bond trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class FixedCouponBondMeasureCalculations1 {

  /**
   * Default implementation.
   */
  public static final FixedCouponBondMeasureCalculations1 DEFAULT = new FixedCouponBondMeasureCalculations1(
      DiscountingFixedCouponBondProductPricer.DEFAULT);
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedFixedCouponBondTrade}.
   */
  private final DiscountingFixedCouponBondProductPricer prodPricer;

  /**
   * Creates an instance.
   * 
   * @param default2  the pricer for {@link ResolvedFixedCouponBondTrade}
   */
  FixedCouponBondMeasureCalculations1(
      DiscountingFixedCouponBondProductPricer default2) {
    this.prodPricer = ArgChecker.notNull(default2, "tradePricer");
  }

  //-------------------------------------------------------------------------
  DoubleScenarioArray zSpread(
		  SecuritizedProductTrade<FixedCouponBond> trade,
	      LegalEntityDiscountingScenarioMarketData marketData, ReferenceData refData) {

	  
	  		

	    // use lookup to query market data
	    
//	     double zs = prodPricer.zSpreadFromCurvesAndDirtyPrice(resolvedTrade.getProduct(), md.scenario(0).discountingProvider(), refData, 
//	    		prodPricer.cleanPriceFromDirtyPrice(resolvedTrade.getProduct(), resolvedTrade.getSettlement().get().getSettlementDate(), resolvedTrade.getInfo().getPrice()/100), 
//	    		CompoundedRateType.CONTINUOUS,resolvedTrade.getProduct().getFrequency().eventsPerYear());
//	     double clp = prodPricer.cleanPriceFromDirtyPrice(resolvedTrade.getProduct(), resolvedTrade.getSettlement().get().getSettlementDate(), target.getPrice()/100);
//	     double dp1 = prodPricer.dirtyPriceFromCurvesWithZSpread(resolvedTrade.getProduct(), md.scenario(0).discountingProvider(), refData, zs, CompoundedRateType.CONTINUOUS,resolvedTrade.getProduct().getFrequency().eventsPerYear());
//	     double clp2 = prodPricer.cleanPriceFromDirtyPrice(resolvedTrade.getProduct(), resolvedTrade.getSettlement().get().getSettlementDate(), dp1);
//	     
////	      double tat = prodPricer.zSpreadFromCurvesAndDirtyPrice(resolvedTrade.getProduct(), marketData.scenario(0).discountingProvider(), refData, 
//	     		prodPricer.cleanPriceFromDirtyPrice(resolvedTrade.getProduct(), resolvedTrade.getSettlement().get().getSettlementDate(), trade.getPrice()/100), 
//	     		CompoundedRateType.CONTINUOUS,resolvedTrade.getProduct().getFrequency().eventsPerYear());
////	     
//	     double zs1 = prodPricer.zSpreadFromCurvesAndDirtyPrice(resolvedTrade.getProduct(), md.scenario(0).discountingProvider(), refData, 
//	     		target.getPrice()/100, 
//	     		CompoundedRateType.CONTINUOUS,resolvedTrade.getProduct().getFrequency().eventsPerYear());
//	      //double clp = prodPricer.cleanPriceFromDirtyPrice(resolvedTrade.getProduct(), resolvedTrade.getSettlement().get().getSettlementDate(), target.getPrice()/100);
//	      double dp2 = prodPricer.dirtyPriceFromCurvesWithZSpread(resolvedTrade.getProduct(), md.scenario(0).discountingProvider(), refData, zs1, CompoundedRateType.CONTINUOUS,resolvedTrade.getProduct().getFrequency().eventsPerYear());
	      //double clp2 = prodPricer.cleanPriceFromDirtyPrice(resolvedTrade.getProduct(), resolvedTrade.getSettlement().get().getSettlementDate(), dp1);
		// TODO Auto-generated method stub
	  
		return DoubleScenarioArray.of(
		        marketData.getScenarioCount(),
		        i -> prodPricer.zSpreadFromCurvesAndDirtyPrice(trade.getProduct().resolve(refData), marketData.scenario(i).discountingProvider(), refData, 
		        		prodPricer.cleanPriceFromDirtyPrice(trade.getProduct().resolve(refData), trade.getInfo().getSettlementDate().get(), trade.getPrice()/100), 
		        		CompoundedRateType.CONTINUOUS,trade.getProduct().resolve(refData).getFrequency().eventsPerYear()));
		  }
  CurrencyScenarioArray presentValueWithSpread(
		  SecuritizedProductTrade<FixedCouponBond> trade,
	      LegalEntityDiscountingScenarioMarketData marketData, ReferenceData refData) {
	  //ResolvedFixedCouponBondTrade resolvedTrade = trade.resolve(refData);
	   double spread = trade.getInfo().getAttribute(AttributeType.of("Z-spread"));
	  
	  return CurrencyScenarioArray.of(
		        marketData.getScenarioCount(),
		        i -> prodPricer.presentValueWithZSpread(trade.getProduct().resolve(refData), marketData.scenario(i).discountingProvider(),spread, 
		        		CompoundedRateType.CONTINUOUS,trade.getProduct().resolve(refData).getFrequency().eventsPerYear()).multipliedBy(trade.getQuantity()));
	  
	  
  }

}
