/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package product;

import java.util.Map;

/**
 * Market standard Fixed-Ibor swap conventions.
 * <p>
 * https://quant.opengamma.io/Interest-Rate-Instruments-and-Market-Conventions.pdf
 */
public final class StandardPolicyConventions {


	
	private static final UnitLInkedPolicyConvention ul = new UnitLInkedPolicyConvention();
	private static final UnitFixedDeterminsiticPolicyConvention uf = new UnitFixedDeterminsiticPolicyConvention();
	public static final PolicyConvention UNIT_LINKED =
			ImmutablePolicyConvention.of(
					"UNIT_LINKED", Map.of("InitialCondition",  ul.IC,"DifferentiationMatrix",ul.D,"M-Matrix",ul.M,"R-Matrix", ul.R,"Interest Rate", 
							ul.IR,"CHF_LIBOR_6M",ul.CHF_LIBOR_6M,"EU_EXT_CPI",ul.EU_EXT_CPI,
							"MortalityRateIndex",ul.MortalityRateIndex, "Indicator",ul.Ind) 
					//List.of(ul.IC,ul.D,ul.M, ul.R, ul.IR,ul,List.of(ul.Derivative,ul.Derivative),ul.Ind)
					);
	public static final PolicyConvention UNIT_FIXED =
			ImmutablePolicyConvention.of(
					"UNIT_FIXED", Map.of("InitialCondition",  uf.IC,"DifferntiationMatrix",uf.D,"M-Matrix",uf.M,"R-Matrix", uf.R,"Interest Rate", uf.IR,"IR Derivative",uf.D,"Indicator",uf.Ind)
					);



	//-------------------------------------------------------------------------
	/**
	 * Restricted constructor.
	 */
	private StandardPolicyConventions() {
	}
}