package loader;

import java.util.ArrayList;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import java.util.List;
import java.util.stream.DoubleStream;

import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.CsvIterator;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.swaption.SwaptionSurfaceExpiryTenorParameterMetadata;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static java.util.stream.Collectors.toList;

public class  Utils{
	public static Surface addRefDatafromCSV(String csv, String name) {
		
		CsvIterator mortalityrates= CsvIterator.of(ResourceLocator.of("classpath:referenceData/" + csv).getCharSource(), true);
		ArrayList<Integer> AGE = new ArrayList<Integer>();
		ArrayList<Integer> YEAR =new ArrayList<Integer>();
		ArrayList<Double> RATE = new ArrayList<Double>();;
		for (CsvRow row : mortalityrates.asIterable()) {
		      //Currency  cur= row.getValue(CURRENCY_FIELD, LoaderUtils::parseCurrency);
		      YEAR.add(row.getValue("YEAR", LoaderUtils::parseInteger));
		      AGE.add(row.getValue("AGE",LoaderUtils::parseInteger));
		      RATE.add(row.getValue("rate",LoaderUtils::parseDouble));
			}
		
		final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
		
		final List<ParameterMetadata> PARAMETER_METADATA =
			      MapStream.zip(AGE.stream(), YEAR.stream())
			          .map(SwaptionSurfaceExpiryTenorParameterMetadata::of)
			          .collect(toList());
			  final SurfaceMetadata METADATA =
			      Surfaces.normalVolatilityByExpiryTenor(name, ACT_365F);
			  
			  final Surface SURFACE_STD = InterpolatedNodalSurface.of(
			          METADATA.withParameterMetadata(PARAMETER_METADATA),
			          DoubleArray.copyOf(AGE.stream().mapToDouble(num -> (double)num).toArray()),
			          DoubleArray.copyOf(YEAR.stream().mapToDouble(num -> (double)num).toArray()),	
			          DoubleArray.copyOf(RATE.stream() //we start with a stream of objects Stream<int[]>
			        		    .flatMapToDouble(DoubleStream::of) //we I'll map each int[] to IntStream
			        		    .toArray()),
			          INTERPOLATOR_2D);
		
		return SURFACE_STD;

	
}
}