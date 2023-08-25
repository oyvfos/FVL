package mavenBlue;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.AbstractDerivedCalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.bond.LegalEntityDiscountingMarketDataLookup;
import com.opengamma.strata.measure.bond.LegalEntityDiscountingScenarioMarketData;
import com.opengamma.strata.product.SecuritizedProduct;
import com.opengamma.strata.product.SecuritizedProductTrade;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.bond.FixedCouponBondTrade;

/**
 * Perform calculations on a single {@code FixedCouponBondTrade} or {@code FixedCouponBondPosition}
 * for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * An instance of {@link LegalEntityDiscountingMarketDataLookup} must be specified.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_SUM PV01 calibrated sum}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_BUCKETED PV01 calibrated bucketed}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_SUM PV01 market quote sum}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_BUCKETED PV01 market quote bucketed}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 * </ul>
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bonds in the trade model, pricers and market data.
 * For example, a price of 99.32% is represented in Strata by 0.9932.
 * 
 * @param <T> the trade or position type
 */
public class FixedCouponBondTradeCalculationFunction1 extends AbstractDerivedCalculationFunction<SecurityTrade, DoubleScenarioArray > {

  protected FixedCouponBondTradeCalculationFunction1(Measure measure) {
		super(SecurityTrade.class,measure);
		// TODO Auto-generated constructor stub
	}
  public static final FixedCouponBondTradeCalculationFunction1 INSTANCE = new FixedCouponBondTradeCalculationFunction1();
  FixedCouponBondTradeCalculationFunction1() {
	    this(Measures1.Z_SPREAD);
	  }


  public FunctionRequirements requirements(
		  SecurityTrade target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
	  SecuritizedProductTrade<FixedCouponBond> product = (SecuritizedProductTrade<FixedCouponBond>) target.resolveTarget(refData).getProduct();

    // use lookup to build requirements
    LegalEntityDiscountingMarketDataLookup bondLookup = parameters.getParameter(LegalEntityDiscountingMarketDataLookup.class);
    //return bondLookup.requirements(product.getSecurityId(), product.getLegalEntityId(), product.getCurrency());
    return bondLookup.requirements(product.getSecurityId(), product.getProduct().getLegalEntityId(), product.getCurrency());
  }

//-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract DoubleScenarioArray calculate(
        SecuritizedProductTrade<FixedCouponBond> securitizedProductTrade,
        LegalEntityDiscountingScenarioMarketData marketData, ReferenceData refData);
  }

@Override
public Measure measure() {
	// TODO Auto-generated method stub
	return Measures1.Z_SPREAD;
}
@Override
public DoubleScenarioArray calculate(SecurityTrade target, Map<Measure, Object> requiredMeasures,
		CalculationParameters parameters, ScenarioMarketData marketData, ReferenceData refData) {
	// TODO Auto-generated method stub
	LegalEntityDiscountingMarketDataLookup bondLookup = parameters.getParameter(LegalEntityDiscountingMarketDataLookup.class);
    LegalEntityDiscountingScenarioMarketData md = bondLookup.marketDataView(marketData);
    
		SingleMeasureCalculation calculator = FixedCouponBondMeasureCalculations1.DEFAULT::zSpread;
	
		return calculator.calculate((SecuritizedProductTrade<FixedCouponBond>) target.resolveTarget(refData), md, refData);
}








}