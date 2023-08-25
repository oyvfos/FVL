package mavenBlue;

import com.opengamma.strata.calc.Measure;

/**
 * The standard set of measures that can be calculated by Strata.
 * <p>
 * A measure identifies the calculation result that is required.
 * For example present value, par rate or spread.
 * <p>
 * Note that not all measures will be available for all targets.
 */
public final class Measures1 {

  /**
   * Measure representing the present value of the calculation target.
   * <p>
   * The result is a single currency monetary amount in the reporting currency.
   */
  public static final Measure Z_SPREAD = Measure.of("Z-Spread");
  public static final Measure presentValueWithSpread = Measure.of("presentValueWithSpread");
  /**
   * Measure representing a break-down of the present value calculation on the target.
   * <p>
   * No currency conversion is performed on the monetary amounts.
   */
  
  //-------------------------------------------------------------------------
  private Measures1() {
  }

}
