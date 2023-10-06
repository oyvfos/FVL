package measure;

import static com.opengamma.strata.basics.index.PriceIndices.EU_EXT_CPI;

import java.util.ArrayList;

/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.ejml.simple.SimpleMatrix;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.AdvancedMeasures;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.product.rate.RateComputation;

import pricer.DispatchingPolicyComputationFn;
import product.Policy;
import product.PolicyComputation;
import product.PolicyTrade;
import product.ResolvedPolicyTrade;
import product.StochasticPIDEComputation;

/**
 * Perform calculations on a single {@code PolicyTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * An instance of {@link RatesMarketDataLookup} must be specified.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#EXPLAIN_PRESENT_VALUE Explain present value}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_SUM PV01 calibrated sum}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_BUCKETED PV01 calibrated bucketed}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_SUM PV01 market quote sum}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_BUCKETED PV01 market quote bucketed}
 *   <li>{@linkplain Measures#PAR_RATE Par rate}
 *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
 *   <li>{@linkplain Measures#CASH_FLOWS Cash flows}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 *   <li>{@linkplain AdvancedMeasures#PV01_SEMI_PARALLEL_GAMMA_BUCKETED PV01 semi-parallel gamma bucketed}
 *   <li>{@linkplain AdvancedMeasures#PV01_SINGLE_NODE_GAMMA_BUCKETED PV01 single node gamma bucketed}
 * </ul>
 */
public class PolicyTradeCalculationFunction
    implements CalculationFunction<PolicyTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
      	  //.put(Measure.of("DIFFMAT"), PolicyMeasureCalculations.DEFAULT::diffMat)
      	  //.put(Measure.of("DIFFMATDet"), PolicyMeasureCalculations.DEFAULT::diffMatDet)
      	  //.put(Measure.of("DIFFMATDet0"), PolicyMeasureCalculations.DEFAULT::diffMatDet0)
          .put(Measures.PRESENT_VALUE, PolicyMeasureCalculations.DEFAULT::presentValue)
          //.put(Measures.EXPLAIN_PRESENT_VALUE, PolicyMeasureCalculations.DEFAULT::explainPresentValue)
          .put(Measures.PV01_CALIBRATED_SUM, PolicyMeasureCalculations.DEFAULT::pv01CalibratedSum)
          //.put(Measures.PV01_CALIBRATED_BUCKETED, PolicyMeasureCalculations.DEFAULT::pv01CalibratedBucketed)
//          .put(Measures.PV01_MARKET_QUOTE_SUM, PolicyMeasureCalculations.DEFAULT::pv01MarketQuoteSum)
//          .put(Measures.PV01_MARKET_QUOTE_BUCKETED, PolicyMeasureCalculations.DEFAULT::pv01MarketQuoteBucketed)
//          .put(Measures.PAR_RATE, PolicyMeasureCalculations.DEFAULT::parRate)
//          .put(Measures.PAR_SPREAD, PolicyMeasureCalculations.DEFAULT::parSpread)
//          .put(Measures.CASH_FLOWS, PolicyMeasureCalculations.DEFAULT::cashFlows)
//          .put(Measures.CURRENCY_EXPOSURE, PolicyMeasureCalculations.DEFAULT::currencyExposure)
//          .put(Measures.CURRENT_CASH, PolicyMeasureCalculations.DEFAULT::currentCash)
//          .put(Measures.RESOLVED_TARGET, (rt, smd) -> rt)
//          .put(AdvancedMeasures.PV01_SEMI_PARALLEL_GAMMA_BUCKETED, PolicyMeasureCalculations.DEFAULT::pv01SemiParallelGammaBucketed)
//          .put(AdvancedMeasures.PV01_SINGLE_NODE_GAMMA_BUCKETED, PolicyMeasureCalculations.DEFAULT::pv01SingleNodeGammaBucketed)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();
  
  private static final DispatchingPolicyComputationFn PRICER = DispatchingPolicyComputationFn.DEFAULT;
  /**
   * Creates an instance.
   */
  public PolicyTradeCalculationFunction() {
  }
  /**
   * The trade instance
   */
  public static final PolicyTradeCalculationFunction TRADE =
      new PolicyTradeCalculationFunction();
  /**
//   * The position instance
//   */
//  public static final PolicyTradeCalculationFunction1<PolicyPosition> POSITION =
//      new PolicyTradeCalculationFunction1<>(PolicyPosition.class);

  //-------------------------------------------------------------------------
  @Override
  public Class<PolicyTrade> targetType() {
    return PolicyTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(PolicyTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  
  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      PolicyTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    Policy product = trade.getProduct();
    Set<Index> indices = new HashSet<>();
    indices.add((Index) EU_EXT_CPI);
    //indices.add(IborIndices.EUR_LIBOR_3M);
    indices.add(FxIndices.EUR_USD_ECB);//placeholder equity index
    //product.getIndexInterpolated().ifPresent(indices::add);
    ImmutableSet<Currency> currencies = ImmutableSet.of(Currency.EUR);

    // use lookup to build requirements
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    return ratesLookup.requirements(currencies, indices);
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      PolicyTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedPolicyTrade resolved = trade.resolve(refData);

    // use lookup to query market data
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    RatesScenarioMarketData marketData = ratesLookup.marketDataView(scenarioMarketData);
    // prepare!! only to reuse diffmat for sensitivity calc for speed up combnation of PV and sens
    //SingleMeasureCalculation calculator = CALCULATORS.get(Measure.of("DIFFMATDet"));
    
    List<Pair<SimpleMatrix, SimpleMatrix>> map=  new ArrayList<Pair<SimpleMatrix, SimpleMatrix>>();
	   
	  for (int i = 0; i < marketData.getScenarioCount(); i++) {
		  //dispatch possibilities - not yet neccessary
		  map.add(PRICER.diffMat(StochasticPIDEComputation.of(0.0),resolved.getProduct(), marketData.scenario(i).ratesProvider(), refData));
	  }
    
    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      	
      results.put(measure, calculate(StochasticPIDEComputation.of(0.0), measure, resolved, marketData,refData, map));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
	  PolicyComputation computation,
      Measure measure,
      ResolvedPolicyTrade trade,
      RatesScenarioMarketData marketData,ReferenceData refData, Object map
       ) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for PolicyTrade: {}", measure);
    }
    
    return Result.of(() -> calculator.calculate(computation, trade, marketData, refData, map ));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
    		PolicyComputation computation,	
        ResolvedPolicyTrade trade,
        RatesScenarioMarketData marketData, ReferenceData refData, Object diffMatArray);
  }

  
@Override
public Currency naturalCurrency(PolicyTrade target, ReferenceData refData) {
	// TODO Auto-generated method stub
	return target.getProduct().getCurrency();
}

}
