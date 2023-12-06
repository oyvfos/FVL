package basics;

import com.opengamma.strata.collect.named.ExtendedEnum;

public class AssumptionIndices {
	
		  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

		  /**
		   * The extended enum lookup from name to instance.
		   */
		  static final ExtendedEnum<AssumptionIndex> ENUM_LOOKUP = ExtendedEnum.of(AssumptionIndex.class);

		  /**
		   * The harmonized consumer price index for the United Kingdom,
		   * "Non-revised Harmonised Index of Consumer Prices".
		   */
		 // public static final AssumptionIndex MortalityRateIndex = AssumptionIndex.of("MortalityRateIndex");
		  private AssumptionIndices() {
		  }

}
