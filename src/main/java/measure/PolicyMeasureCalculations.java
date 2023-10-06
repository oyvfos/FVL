package measure;

import java.util.List;

import org.ejml.simple.SimpleMatrix;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveGammaCalculator;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;

import liabilities.NonObservableId;
import pricer.DispatchingPolicyComputationFn;
import product.PolicyComputation;
import product.ResolvedPolicyTrade;

/**
 * Multi-scenario measure calculations for FRA trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class PolicyMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final PolicyMeasureCalculations DEFAULT = new PolicyMeasureCalculations(
		  DispatchingPolicyComputationFn.DEFAULT);
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * The cross gamma sensitivity calculator.
   */
  private static final CurveGammaCalculator CROSS_GAMMA = CurveGammaCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedPolicyTrade}.
   */
  private static final MarketQuoteSensitivityCalculator iborSensCalc = MarketQuoteSensitivityCalculator.DEFAULT;
  
  private final DispatchingPolicyComputationFn tradePricer;

  /**
   * Creates an instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedPolicyTrade}
   */
  PolicyMeasureCalculations(
		  DispatchingPolicyComputationFn tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
		  PolicyComputation computation,
		  ResolvedPolicyTrade trade,
      RatesScenarioMarketData marketData,
      ReferenceData refData,
      Object diffMatArray
      ) {
	  List<Pair<SimpleMatrix, SimpleMatrix>> arr = (List<Pair<SimpleMatrix, SimpleMatrix>>) diffMatArray;  
    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> presentValue(computation,trade, marketData.scenario(i).ratesProvider(), refData,arr.get(i)));
  }

  // present value for one scenario
  CurrencyAmount presentValue(
		  PolicyComputation computation,
		  ResolvedPolicyTrade trade,
      RatesProvider ratesProvider,
      ReferenceData refData,
      Pair<SimpleMatrix, SimpleMatrix> dm) {

    return tradePricer.presentValue(computation,trade, ratesProvider, refData,dm);
  }
   //-------------------------------------------------------------------------
  // calculates explain present value for all scenarios
//  ScenarioArray<ExplainMap> explainPresentValue(
//      ResolvedPolicyTrade trade,
//      RatesScenarioMarketData marketData,
//      ReferenceData refData) {
//
//    return ScenarioArray.of(
//        marketData.getScenarioCount(),
//        i -> explainPresentValue(trade, marketData.scenario(i).ratesProvider(),refData));
//  }
//
//  // explain present value for one scenario
//  ExplainMap explainPresentValue(
//      ResolvedPolicyTrade trade,
//      RatesProvider ratesProvider,
//      ReferenceData refData) {
//
//    return tradePricer.explainPresentValue(trade, ratesProvider,refData);
//  }
//
//  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01CalibratedSum(
		  PolicyComputation computation,
      ResolvedPolicyTrade trade,
      RatesScenarioMarketData marketData,
      ReferenceData refData, 
      Object diffMatArray) {
	  List<Pair<SimpleMatrix, SimpleMatrix>> arr = (List<Pair<SimpleMatrix, SimpleMatrix>>) diffMatArray;   
	  return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedSum(computation,trade, marketData.scenario(i).ratesProvider(), refData, arr.get(i)));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01CalibratedSum(
		  PolicyComputation computation,
		  ResolvedPolicyTrade trade,
      RatesProvider ratesProvider,
      ReferenceData refData,
      Pair<SimpleMatrix, SimpleMatrix> diffMat) {
	  Curve fwdCurve = ratesProvider.findData(CurveName.of("ESG")).get();
	  double dt = ratesProvider.data(NonObservableId.of("TimeStep"));
	  PointSensitivities pointSensitivities  = tradePricer.presentValueSensitivity(computation,trade, ratesProvider, refData, diffMat);
	  CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
	  for (PointSensitivity point : pointSensitivities.getSensitivities()) {
		 IborRateSensitivity pt = (IborRateSensitivity) point;
	        IborIndexRates rates = IborIndexRates.of(IborIndices.EUR_LIBOR_3M, ratesProvider.getValuationDate(), fwdCurve) ;
	        sens = sens.combinedWith(rates.parameterSensitivity(pt));
	        //CurrencyParameterSensitivities.of(pt.build().);
	  }
	  //PointSensitivities am = pointSensitivities.;
    return sens.total().multipliedBy(ONE_BASIS_POINT).multipliedBy(dt);
//    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
//    for (PointSensitivity point : pointSensitivity.getSensitivities()) {
//       CurrencyParameterSensitivities.of(curSens)
//        ZeroRateSensitivity pt = (ZeroRateSensitivity) point;
//       // DiscountFactors factors = discountFactors(pt.getCurveCurrency());
//        sens = sens.combinedWith(pt);
//    }
//    return sens.;
  }

//  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
		  PolicyComputation computation,
		  ResolvedPolicyTrade trade,
      RatesScenarioMarketData marketData,
      ReferenceData refData,
      Object diffMatArray) {
	  List<Pair<SimpleMatrix, SimpleMatrix>> arr = (List<Pair<SimpleMatrix, SimpleMatrix>>) diffMatArray;  

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(computation,trade, marketData.scenario(i).ratesProvider(),refData, arr.get(i)));
  }

  // calibrated bucketed PV01 for one scenario
CurrencyParameterSensitivities pv01CalibratedBucketed(
		PolicyComputation computation,
		ResolvedPolicyTrade trade,
	      RatesProvider ratesProvider,
	      ReferenceData refData,
	      Pair<SimpleMatrix, SimpleMatrix> diffMat) {
		  Curve fwdCurve = ratesProvider.findData(CurveName.of("ESG")).get();
		  double dt = ratesProvider.data(NonObservableId.of("TimeStep"));
		  PointSensitivities pointSensitivities  = tradePricer.presentValueSensitivity(computation,trade, ratesProvider, refData, diffMat);
		  CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
		  for (PointSensitivity point : pointSensitivities.getSensitivities()) {
			 IborRateSensitivity pt = (IborRateSensitivity) point;
		        IborIndexRates rates = IborIndexRates.of(IborIndices.EUR_LIBOR_3M, ratesProvider.getValuationDate(), fwdCurve) ;
		        sens = sens.combinedWith(rates.parameterSensitivity(pt));
		        //CurrencyParameterSensitivities.of(pt.build().);
		  }
		  //PointSensitivities am = pointSensitivities.;
	    return sens.multipliedBy(ONE_BASIS_POINT).multipliedBy(dt);

	  }
//
//  //-------------------------------------------------------------------------
//  // calculates market quote sum PV01 for all scenarios
//  MultiCurrencyScenarioArray pv01MarketQuoteSum(
//      ResolvedPolicyTrade trade,
//      RatesScenarioMarketData marketData) {
//
//    return MultiCurrencyScenarioArray.of(
//        marketData.getScenarioCount(),
//        i -> pv01MarketQuoteSum(trade, marketData.scenario(i).ratesProvider()));
//  }
//
//  // market quote sum PV01 for one scenario
//  MultiCurrencyAmount pv01MarketQuoteSum(
//      ResolvedPolicyTrade trade,
//      RatesProvider ratesProvider) {
//
//    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
//    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
//    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
//  }
//
//  //-------------------------------------------------------------------------
//  // calculates market quote bucketed PV01 for all scenarios
//  ScenarioArray<CurrencyParameterSensitivities> pv01MarketQuoteBucketed(
//      ResolvedPolicyTrade trade,
//      RatesScenarioMarketData marketData) {
//
//    return ScenarioArray.of(
//        marketData.getScenarioCount(),
//        i -> pv01MarketQuoteBucketed(trade, marketData.scenario(i).ratesProvider()));
//  }
//
//  // market quote bucketed PV01 for one scenario
//  CurrencyParameterSensitivities pv01MarketQuoteBucketed(
//      ResolvedPolicyTrade trade,
//      RatesProvider ratesProvider) {
//
//    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
//    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
//    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).multipliedBy(ONE_BASIS_POINT);
//  }
//
//  //-------------------------------------------------------------------------
//  // calculates semi-parallel gamma PV01 for all scenarios
////  ScenarioArray<CurrencyParameterSensitivities> pv01SemiParallelGammaBucketed(
////      ResolvedPolicyTrade trade,
////      RatesScenarioMarketData marketData) {
////
////    return ScenarioArray.of(
////        marketData.getScenarioCount(),
////        i -> pv01SemiParallelGammaBucketed(trade, marketData.scenario(i)));
////  }
//
//  // semi-parallel gamma PV01 for one scenario
////  private CurrencyParameterSensitivities pv01SemiParallelGammaBucketed(
////      ResolvedPolicyTrade trade,
////      RatesMarketData marketData) {
////
////    // find the curve identifiers and resolve to a single curve
////    Currency currency = Currency.EUR;
////    Set<IborIndex> indices = Set.of(IborIndex.of("AA"));
////    ImmutableSet<MarketDataId<?>> discountIds = marketData.getLookup().getDiscountMarketDataIds(currency);
////    ImmutableSet<MarketDataId<?>> forwardIds = indices.stream()
////        .flatMap(idx -> marketData.getLookup().getForwardMarketDataIds(idx).stream())
////        .collect(toImmutableSet());
////    Set<MarketDataId<?>> allIds = Sets.union(discountIds, forwardIds);
////    if (allIds.size() != 1) {
////      throw new IllegalArgumentException(Messages.format(
////          "Implementation only supports a single curve, but lookup refers to more than one: {}", allIds));
////    }
////    MarketDataId<?> singleId = allIds.iterator().next();
////    if (!(singleId instanceof CurveId)) {
////      throw new IllegalArgumentException(Messages.format(
////          "Implementation only supports a single curve, but lookup does not refer to a curve: {} {}",
////          singleId.getClass().getName(), singleId));
////    }
////    CurveId curveId = (CurveId) singleId;
////    Curve curve = marketData.getMarketData().getValue(curveId);
////
////    // calculate gamma
////    CurrencyParameterSensitivity gamma = CurveGammaCalculator.DEFAULT.calculateSemiParallelGamma(
////        curve, currency, c -> calculateCurveSensitivity(trade, marketData, curveId, c));
////    return CurrencyParameterSensitivities.of(gamma).multipliedBy(ONE_BASIS_POINT * ONE_BASIS_POINT);
////  }
//
//  // calculates the sensitivity
//  private CurrencyParameterSensitivity calculateCurveSensitivity(
//      ResolvedPolicyTrade trade,
//      RatesMarketData marketData,
//      CurveId curveId,
//      Curve bumpedCurve) {
//
//    MarketData bumpedMarketData = marketData.getMarketData().withValue(curveId, bumpedCurve);
//    RatesProvider bumpedRatesProvider = marketData.withMarketData(bumpedMarketData).ratesProvider();
//    PointSensitivities pointSensitivities = tradePricer.presentValueSensitivity(trade, bumpedRatesProvider);
//    CurrencyParameterSensitivities paramSensitivities = bumpedRatesProvider.parameterSensitivity(pointSensitivities);
//    return Iterables.getOnlyElement(paramSensitivities.getSensitivities());
//  }
//
//  //-------------------------------------------------------------------------
//  // calculates single-node gamma PV01 for all scenarios
//  ScenarioArray<CurrencyParameterSensitivities> pv01SingleNodeGammaBucketed(
//      ResolvedPolicyTrade trade,
//      RatesScenarioMarketData marketData) {
//
//    return ScenarioArray.of(
//        marketData.getScenarioCount(),
//        i -> pv01SingleNodeGammaBucketed(trade, marketData.scenario(i).ratesProvider()));
//  }
//
//  // single-node gamma PV01 for one scenario
//  private CurrencyParameterSensitivities pv01SingleNodeGammaBucketed(
//      ResolvedPolicyTrade trade,
//      RatesProvider ratesProvider) {
//
//    CrossGammaParameterSensitivities crossGamma = CROSS_GAMMA.calculateCrossGammaIntraCurve(
//        ratesProvider,
//        p -> p.parameterSensitivity(tradePricer.presentValueSensitivity(trade, p)));
//    return crossGamma.diagonal().multipliedBy(ONE_BASIS_POINT * ONE_BASIS_POINT);
//  }
//
//  //-------------------------------------------------------------------------
//  // calculates par rate for all scenarios
//  DoubleScenarioArray parRate(
//      ResolvedPolicyTrade trade,
//      RatesScenarioMarketData marketData) {
//
//    return DoubleScenarioArray.of(
//        marketData.getScenarioCount(),
//        i -> parRate(trade, marketData.scenario(i).ratesProvider()));
//  }
//
//  // par rate for one scenario
//  double parRate(
//      ResolvedPolicyTrade trade,
//      RatesProvider ratesProvider) {
//
//    return tradePricer.parRate(trade, ratesProvider);
//  }
//
//  //-------------------------------------------------------------------------
//  // calculates par spread for all scenarios
//  DoubleScenarioArray parSpread(
//      ResolvedPolicyTrade trade,
//      RatesScenarioMarketData marketData) {
//
//    return DoubleScenarioArray.of(
//        marketData.getScenarioCount(),
//        i -> parSpread(trade, marketData.scenario(i).ratesProvider()));
//  }
//
//  // par spread for one scenario
//  double parSpread(
//      ResolvedPolicyTrade trade,
//      RatesProvider ratesProvider) {
//
//    return tradePricer.parSpread(trade, ratesProvider);
//  }
//
//  //-------------------------------------------------------------------------
//  // calculates cash flows for all scenarios
//  ScenarioArray<CashFlows> cashFlows(
//      ResolvedPolicyTrade trade,
//      RatesScenarioMarketData marketData) {
//
//    return ScenarioArray.of(
//        marketData.getScenarioCount(),
//        i -> cashFlows(trade, marketData.scenario(i).ratesProvider()));
//  }
//
//  // cash flows for one scenario
//  CashFlows cashFlows(
//      ResolvedPolicyTrade trade,
//      RatesProvider ratesProvider) {
//
//    return tradePricer.cashFlows(trade, ratesProvider);
//  }
//
//  //-------------------------------------------------------------------------
//  // calculates currency exposure for all scenarios
//  MultiCurrencyScenarioArray currencyExposure(
//      ResolvedPolicyTrade trade,
//      RatesScenarioMarketData marketData,ReferenceData refData) {
//
//    return MultiCurrencyScenarioArray.of(
//        marketData.getScenarioCount(),
//        i -> currencyExposure(trade, marketData.scenario(i).ratesProvider(),refData));
//  }
//
//  // currency exposure for one scenario
//  MultiCurrencyAmount currencyExposure(
//      ResolvedPolicyTrade trade,
//      RatesProvider ratesProvider,ReferenceData refData) {
//
//    return tradePricer.currencyExposure(trade, ratesProvider,refData);
//  }
//
//  //-------------------------------------------------------------------------
//  // calculates current cash for all scenarios
//  CurrencyScenarioArray currentCash(
//      ResolvedPolicyTrade trade,
//      RatesScenarioMarketData marketData,ReferenceData refData) {
//
//    return CurrencyScenarioArray.of(
//        marketData.getScenarioCount(),
//        i -> currentCash(trade, marketData.scenario(i).ratesProvider(),refData));
//  }
//
//  // current cash for one scenario
//  CurrencyAmount currentCash(
//      ResolvedPolicyTrade trade,
//      RatesProvider ratesProvider,ReferenceData refData) {
//
//    return tradePricer.currentCash(trade, ratesProvider,refData);
//  }

}
