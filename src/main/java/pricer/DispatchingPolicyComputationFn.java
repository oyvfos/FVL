/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package pricer;

import java.time.LocalDate;

import org.ejml.simple.SimpleMatrix;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.FixedOvernightCompoundedAnnualRateComputation;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborAveragedRateComputation;
import com.opengamma.strata.product.rate.IborInterpolatedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.rate.InflationEndInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationEndMonthRateComputation;
import com.opengamma.strata.product.rate.InflationInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationMonthlyRateComputation;
import com.opengamma.strata.product.rate.OvernightAveragedDailyRateComputation;
import com.opengamma.strata.product.rate.OvernightAveragedRateComputation;
import com.opengamma.strata.product.rate.OvernightCompoundedAnnualRateComputation;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;

import product.PolicyComputation;
import product.ResolvedPolicy;
import product.ResolvedPolicyTrade;
import product.StochasticPIDEComputation;

/**
 * Rate computation implementation using multiple dispatch.
 * <p>
 * Dispatches the request to the correct implementation.
 */
public class DispatchingPolicyComputationFn
    implements PolicyComputationFn<PolicyComputation> {

  /**
   * Default implementation.
   */
  public static final DispatchingPolicyComputationFn DEFAULT = new DispatchingPolicyComputationFn(
      StochasticPIDEComputationFn.DEFAULT);

  /**
   * Rate provider for {@link IborRateComputation}.
   */
  private final PolicyComputationFn<StochasticPIDEComputation> stochPIDEComputationFn;
  /**
   * Rate provider for {@link IborInterpolatedRateComputation}.
   */
 

  /**
   * Creates an instance.
   *
   * @param iborRateComputationFn  the rate provider for {@link IborRateComputation}
   * @param iborInterpolatedRateComputationFn  the rate computation for {@link IborInterpolatedRateComputation}
   * @param iborAveragedRateComputationFn  the rate computation for {@link IborAveragedRateComputation}
   * @param overnightCompoundedRateComputationFn  the rate computation for {@link OvernightCompoundedRateComputation}
   * @param overnightCompundedAnnualRateComputationFn  the rate computation for {@link OvernightCompoundedAnnualRateComputation}
   * @param overnightAveragedRateComputationFn  the rate computation for {@link OvernightAveragedRateComputation}
   * @param overnightAveragedDailyRateComputationFn  the rate computation for {@link OvernightAveragedDailyRateComputation}
   * @param inflationMonthlyRateComputationFn  the rate computation for {@link InflationMonthlyRateComputation}
   * @param inflationInterpolatedRateComputationFn  the rate computation for {@link InflationInterpolatedRateComputation}
   * @param inflationEndMonthRateComputationFn  the rate computation for {@link InflationEndMonthRateComputation}
   * @param inflationEndInterpolatedRateComputationFn  the rate computation for {@link InflationEndInterpolatedRateComputation}
   */
  public DispatchingPolicyComputationFn(
      PolicyComputationFn<StochasticPIDEComputation> stochPIDEComputationFn)
//      RateComputationFn<IborInterpolatedRateComputation> iborInterpolatedRateComputationFn,
//      RateComputationFn<IborAveragedRateComputation> iborAveragedRateComputationFn,
//      RateComputationFn<OvernightCompoundedRateComputation> overnightCompoundedRateComputationFn,
//      RateComputationFn<OvernightCompoundedAnnualRateComputation> overnightCompundedAnnualRateComputationFn,
//      RateComputationFn<OvernightAveragedRateComputation> overnightAveragedRateComputationFn,
//      RateComputationFn<OvernightAveragedDailyRateComputation> overnightAveragedDailyRateComputationFn,
//      RateComputationFn<InflationMonthlyRateComputation> inflationMonthlyRateComputationFn,
//      RateComputationFn<InflationInterpolatedRateComputation> inflationInterpolatedRateComputationFn,
//      RateComputationFn<InflationEndMonthRateComputation> inflationEndMonthRateComputationFn,
//      RateComputationFn<InflationEndInterpolatedRateComputation> inflationEndInterpolatedRateComputationFn) 
      {

    this.stochPIDEComputationFn =
        ArgChecker.notNull(stochPIDEComputationFn, "stochPIDEComputationFn");
//    this.iborInterpolatedRateComputationFn =
//        ArgChecker.notNull(iborInterpolatedRateComputationFn, "iborInterpolatedRateComputationFn");
//    this.iborAveragedRateComputationFn =
//        ArgChecker.notNull(iborAveragedRateComputationFn, "iborAverageRateComputationFn");
//    this.overnightCompoundedRateComputationFn =
//        ArgChecker.notNull(overnightCompoundedRateComputationFn, "overnightCompoundedRateComputationFn");
//    this.overnightCompundedAnnualRateComputationFn =
//        ArgChecker.notNull(overnightCompundedAnnualRateComputationFn, "overnightCompundedAnnualRateComputationFn");
//    this.overnightAveragedRateComputationFn =
//        ArgChecker.notNull(overnightAveragedRateComputationFn, "overnightAveragedRateComputationFn");
//    this.overnightAveragedDailyRateComputationFn =
//        ArgChecker.notNull(overnightAveragedDailyRateComputationFn, "overnightAveragedDailyRateComputationFn");
//    this.inflationMonthlyRateComputationFn =
//        ArgChecker.notNull(inflationMonthlyRateComputationFn, "inflationMonthlyRateComputationFn");
//    this.inflationInterpolatedRateComputationFn =
//        ArgChecker.notNull(inflationInterpolatedRateComputationFn, "inflationInterpolatedRateComputationFn");
//    this.inflationEndMonthRateComputationFn =
//        ArgChecker.notNull(inflationEndMonthRateComputationFn, "inflationEndMonthRateComputationFn");
//    this.inflationEndInterpolatedRateComputationFn =
//        ArgChecker.notNull(inflationEndInterpolatedRateComputationFn, "inflationEndInterpolatedRateComputationFn");
  }

  //-------------------------------------------------------------------------

  







public CurrencyAmount presentValue(PolicyComputation computation,ResolvedPolicyTrade trade, RatesProvider ratesProvider, ReferenceData refData,Pair<SimpleMatrix,SimpleMatrix> dm) {
	if (computation instanceof StochasticPIDEComputation) {
	      // inline code (performance) avoiding need for FixedRateComputationFn implementation
	      return ((StochasticPIDEComputation) computation).presentValue(trade.getProduct(), ratesProvider, refData, dm);}
	else
	// TODO Auto-generated method stub
	return null;
}

public PointSensitivities presentValueSensitivity(PolicyComputation computation, ResolvedPolicyTrade trade, RatesProvider ratesProvider,
		ReferenceData refData, Pair<SimpleMatrix, SimpleMatrix> diffMat) {
	if (computation instanceof StochasticPIDEComputation) {
	      // inline code (performance) avoiding need for FixedRateComputationFn implementation
	      return ((StochasticPIDEComputation) computation).sensBuilder(trade.getProduct(), ratesProvider, refData,diffMat ).build();}
	else
	// TODO Auto-generated method stub
	return null;
}

public Pair<SimpleMatrix, SimpleMatrix> diffMat(PolicyComputation computation, ResolvedPolicy product, RatesProvider ratesProvider, ReferenceData refData) {
	if (computation instanceof StochasticPIDEComputation) {
	      // inline code (performance) avoiding need for FixedRateComputationFn implementation
	      return ((StochasticPIDEComputation) computation).diffMat(product, ratesProvider, refData);}
	else
	// TODO Auto-generated method stub
	return null;
	
}

@Override
public double rate(PolicyComputation computation, LocalDate startDate, LocalDate endDate, RatesProvider provider) {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public PointSensitivityBuilder rateSensitivity(PolicyComputation computation, LocalDate startDate, LocalDate endDate,
		RatesProvider provider) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public double explainRate(PolicyComputation computation, LocalDate startDate, LocalDate endDate, RatesProvider provider,
		ExplainMapBuilder builder) {
	// TODO Auto-generated method stub
	return 0;
}

  
  
}
