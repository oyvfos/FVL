/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package liabilities;

import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;


/**
 * The standard set of curve extrapolators.
 * <p>
 * These are referenced from {@link CurveExtrapolators} where their name is used to look up an
 * instance of {@link CurveExtrapolator}. This allows them to be referenced statically like a
 * constant but also allows them to be redefined and new instances added.
 */
final class StandardCurveExtrapolators {
  
	 // BH
  public static final CurveExtrapolator BH = BarrieHibbertExtrapolator.INSTANCE;
 
  

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardCurveExtrapolators() {
  }

}
