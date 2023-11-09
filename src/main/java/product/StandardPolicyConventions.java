/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package product;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static java.util.stream.Collectors.toList;

import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;

import com.google.common.collect.Lists;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.CsvIterator;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.SwaptionSurfaceExpiryTenorParameterMetadata;

import utilities.Taylor;

/**
 * Market standard Fixed-Ibor swap conventions.
 * <p>
 * https://quant.opengamma.io/Interest-Rate-Instruments-and-Market-Conventions.pdf
 */
public final class StandardPolicyConventions {


	
	private static final UnitLInkedPolicyConvention ul = new UnitLInkedPolicyConvention();
	private static final UnitFixedPolicyConvention uf = new UnitFixedPolicyConvention();
	public static final PolicyConvention UNIT_LINKED =
			ImmutablePolicyConvention.of(
					"UNIT_LINKED", List.of(ul.IC,ul.D,ul.M, ul.R, ul.IR,ul.Derivative,ul.Ind)
					);
	public static final PolicyConvention UNIT_FIXED =
			ImmutablePolicyConvention.of(
					"UNIT_FIXED", List.of(ul.IC,ul.D,ul.M, ul.R, ul.IR,ul.Derivative,ul.Ind)
					);



	//-------------------------------------------------------------------------
	/**
	 * Restricted constructor.
	 */
	private StandardPolicyConventions() {
	}
}