package measure;

import com.opengamma.strata.calc.ImmutableMeasure;
import com.opengamma.strata.calc.Measure;

/**
 * The standard set of measures that can be calculated by Strata.
 */
public final class StandardMeasures {

  // present value, with currency conversion
  public static final Measure presentValueWithSpread = ImmutableMeasure.of("presentValueWithSpread");
  public static final Measure Z_SPREAD = ImmutableMeasure.of("Z-Spread");
  //public static final Measure PresentValue0 = ImmutableMeasure.of("PresentValue0");
  // explain present value, with no currency conversion
  public static final Measure DIFFMAT = ImmutableMeasure.of("DIFFMAT");
  public static final Measure DIFFMATDet = ImmutableMeasure.of("DIFFMATDet");
  public static final Measure DIFFMATDet0 = ImmutableMeasure.of("DIFFMATDet0");
  //-------------------------------------------------------------------------
  // restricted constructor
  private StandardMeasures() {
  }

}