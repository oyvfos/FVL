/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package liabilities;

import java.time.LocalDate;

import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.product.LegalEntityId;

import lost.MortalityRates;

/**
 * Volatilities for pricing swaptions.
 * <p>
 * This provides access to the volatilities for various pricing models, such as normal, Black and SABR.
 * The price and derivatives are also made available.
 */
public interface TransitionRates
extends MarketDataView, ParameterizedData {

/**
* Obtains an instance from a curve.
* <p>
* If the curve is {@code ConstantCurve}, {@code ConstantRecoveryRates} is always instantiated. 
* 
* @param legalEntityId  the legal entity identifier
* @param valuationDate  the valuation date for which the curve is valid
* @param curve  the underlying curve
* @return the instance
*/
//public static TransitionRates of(StandardId legalEntityId, LocalDate valuationDate, Curve curve) {
//if (curve.getMetadata().getYValueType().equals(ValueType.RECOVERY_RATE)) {
//  ConstantCurve constantCurve = (ConstantCurve) curve;
//  return ConstantRecoveryRates.of(legalEntityId, valuationDate, constantCurve.getYValue());
//}
//throw new IllegalArgumentException("Unknown curve type");
//}

//-------------------------------------------------------------------------
/**
* Gets the valuation date. 
* 
* @return the valuation date
*/
@Override
public abstract LocalDate getValuationDate();

/**
* Gets the standard identifier of a legal entity.
* 
* @return the legal entity ID
*/
public abstract TransitionRatesId getTransitionRatesId();

/**
* Gets the recovery rate for the specified date. 
* 
* @param date  the date
* @return the recovery rate
*/
//public abstract Surface recoveryRate(LocalDate date);

@Override
public abstract MortalityRates withParameter(int parameterIndex, double newValue);

@Override
public abstract MortalityRates withPerturbation(ParameterPerturbation perturbation);



}

