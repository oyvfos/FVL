package mavenBlue;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.market.ValueType.NORMAL_VOLATILITY;
import static com.opengamma.strata.market.ValueType.SIMPLE_MONEYNESS;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.script.ScriptException;

import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.io.CsvIterator;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.pricer.option.TenorRawOptionData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.SabrParametersSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SabrSwaptionCalibrator;
import com.opengamma.strata.pricer.swaption.SabrSwaptionDefinition;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesName;

public class SABR {
	 public static final List<Period> EXPIRIES = new ArrayList<>();
	  public static final List<Tenor> TENORS = new ArrayList<>();
	  public static final DoubleArray MONEYNESS =
		      DoubleArray.of(0.0000,-0.0200, -0.0100, -0.0050, -0.0025, 0.0025, 0.0050, 0.0100, 0.0200,0.0300, 0.0400,0.0500);

	  static {
		    EXPIRIES.add(Period.ofMonths(1));
		    EXPIRIES.add(Period.ofMonths(3));
		    EXPIRIES.add(Period.ofMonths(6));
		    EXPIRIES.add(Period.ofMonths(9));
		    EXPIRIES.add(Period.ofYears(1));
		    EXPIRIES.add(Period.ofYears(2));
		    EXPIRIES.add(Period.ofYears(3));
		    EXPIRIES.add(Period.ofYears(4));
		    EXPIRIES.add(Period.ofYears(5));
		    EXPIRIES.add(Period.ofYears(6));
		    EXPIRIES.add(Period.ofYears(7));
		    EXPIRIES.add(Period.ofYears(8));
		    EXPIRIES.add(Period.ofYears(9));
		    EXPIRIES.add(Period.ofYears(10));
		    EXPIRIES.add(Period.ofYears(12));
		    EXPIRIES.add(Period.ofYears(15));
		    EXPIRIES.add(Period.ofYears(20));
		    EXPIRIES.add(Period.ofYears(25));
		    EXPIRIES.add(Period.ofYears(30));
		    
		    TENORS.add(Tenor.TENOR_1Y);
		    TENORS.add(Tenor.TENOR_2Y);
		    TENORS.add(Tenor.TENOR_3Y);
		    TENORS.add(Tenor.TENOR_4Y);
		    TENORS.add(Tenor.TENOR_5Y);
		    TENORS.add(Tenor.TENOR_7Y);
		    TENORS.add(Tenor.TENOR_10Y);
		    TENORS.add(Tenor.TENOR_15Y);
		    TENORS.add(Tenor.TENOR_20Y);
		    TENORS.add(Tenor.TENOR_25Y);
		    TENORS.add(Tenor.TENOR_30Y);
		  }
	  
	private static double[][][] DATAARRAY_FULL;
	static{
		CsvIterator vols= CsvIterator.of(ResourceLocator.of("classpath:example-calibration/quotes/normalVols2018.csv").getCharSource(), true);
		
		
		int  ks= TENORS.size();
		int  is= EXPIRIES.size();
		int  js= MONEYNESS.size();
		double[][][] d3 = new double[ks][js][is];
		for (CsvRow row : vols.asIterable()) {
		      //Currency  cur= row.getValue(CURRENCY_FIELD, LoaderUtils::parseCurrency);
			Integer k = row.getValue("Maturity", LoaderUtils::parseInteger);
			Integer j = row.getValue("Expiry", LoaderUtils::parseInteger);
			Integer i = row.getValue("Strike", LoaderUtils::parseInteger);
			double val = row.getValue("Waarde",LoaderUtils::parseDouble);
			d3[k-1][j-1][i-1]=val; 
			
		}
		
		
		
		
        //int[] d1 = new int[width*height*depth];

        //3D Array :
                  
	  DATAARRAY_FULL= d3;  		
		}
	
	  
		  
		//  private static final TenorRawOptionData DATA_SPARSE = rawData(DATA_ARRAY_SPARSE);
		  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
		  private static final SwaptionVolatilitiesName NAME_SABR = SwaptionVolatilitiesName.of("Calibrated-SABR");
		  private static final SabrSwaptionDefinition DEFINITION =
		      SabrSwaptionDefinition.of(NAME_SABR, EUR_FIXED_1Y_EURIBOR_6M, ACT_365F, INTERPOLATOR_2D);

		 private static TenorRawOptionData rawData(double[][][] dataArray) {
			    Map<Tenor, RawOptionData> raw = new TreeMap<>();
			    for (int looptenor = 0; looptenor < dataArray.length; looptenor++) {
			      DoubleMatrix matrix = DoubleMatrix.ofUnsafe(dataArray[looptenor]).multipliedBy(0.01).transpose();
			      raw.put(TENORS.get(looptenor), RawOptionData.of(EXPIRIES, MONEYNESS, SIMPLE_MONEYNESS, matrix, NORMAL_VOLATILITY));
			    }
			    return TenorRawOptionData.of(raw);
			  }
		 
		 public static SabrParametersSwaptionVolatilities swaptionVols(RatesProvider multicurve, LocalDate VALUATION_DATE){
			 LocalDate CALIBRATION_DATE = VALUATION_DATE;
			 ZonedDateTime CALIBRATION_TIME = CALIBRATION_DATE.atTime(10, 0).atZone(ZoneId.of("Europe/Berlin"));
			 TenorRawOptionData volsdata = rawData(DATAARRAY_FULL);
			 SabrSwaptionCalibrator SABR_CALIBRATION = SabrSwaptionCalibrator.DEFAULT;
			 double beta = 0.50;
			    SurfaceMetadata betaMetadata = DefaultSurfaceMetadata.builder()
			        .xValueType(ValueType.YEAR_FRACTION)
			        .yValueType(ValueType.YEAR_FRACTION)
			        .zValueType(ValueType.SABR_BETA)
			        .surfaceName("Beta").build();
			    Surface betaSurface = ConstantSurface.of(betaMetadata, beta);
			    double shift = 0.0300;
			    Surface shiftSurface = ConstantSurface.of("SABR-Shift", shift);
			    SabrParametersSwaptionVolatilities sabr = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
			        DEFINITION, CALIBRATION_TIME, volsdata, multicurve, betaSurface, shiftSurface);
			    return sabr;
		 }		
		

}
