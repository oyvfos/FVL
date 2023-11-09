package product;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static java.util.stream.Collectors.toList;

import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;

import com.google.common.collect.Lists;
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

public class UnitLInkedPolicyConvention {
	static List<Pair<Integer, Integer>> ranges= List.of(Pair.of(-2, 3),Pair.of(-2, 3),Pair.of(-2, 3));
	//static List<Pair<Integer, Integer>> ranges= List.of(Pair.of(-2, 3),Pair.of(0, 1),Pair.of(0, 1));
	static List<Double> stepsizes= List.of(0.5,.01,.05);
	static List<Double> shift= List.of(1d,0d,0d);
	//static List<BiFunction<Double, SimpleMatrix,SimpleMatrix>> mu=new ArrayList<BiFunction<Double,SimpleMatrix,SimpleMatrix>>();
	//static List<BiFunction<Double, SimpleMatrix,SimpleMatrix>> B=new ArrayList<BiFunction<Double,SimpleMatrix,SimpleMatrix>>();
	static List<SimpleMatrix> firstOrder=new ArrayList<SimpleMatrix>();
	static List<SimpleMatrix> secondOrder=new ArrayList<SimpleMatrix>();
	static List<DoubleArray> vectors=new ArrayList<DoubleArray>();
	static int blockDim;
	static SimpleMatrix ind;
	static SimpleMatrix ind1;

	static DoubleArray detVector;
	static SimpleMatrix firstOrderDet;
	//static ReferenceData referenceDataGrid ;
	// Set up Differnation matrices Ds;
	static {
		//ImmutableMap.Builder<ReferenceDataId<?>, Object> builderRefData1 = ImmutableMap.builder();

		double a0=0.001258720889208218;
		double b0=0.00013;
		double s0=0.00349;
		//     double stepsize1=.25d;//fonds
		//     double stepsizes2=0.01;//rente  
		//     double stepsizes3=0.05;//eq 
		//    
		double s1= 10*s0;
		double b1= b0;

		List<List<Double>> s;
		List<Double> sd;
		//List<List<Object>> out;
		//   for (Pair<Integer,List<Integer>> state : statesDef) // for each state
		//{
		//int id= state.getFirst();
		List<List<Double>> prv = new ArrayList<List<Double>>();

		for (int def = 0; def < ranges.size(); ++def) {// for each random variable  
			Double st = stepsizes.get(def);
			Double sh = shift.get(def);
			List<Double> g1 = IntStream.range(ranges.get(def).getFirst(), ranges.get(def).getSecond()).mapToDouble(i -> i*st).boxed().toList();                 
			g1= g1.stream().mapToDouble(i->i+sh).boxed().collect(Collectors.toList());
			prv.add(g1);                         
		}
		s=Lists.cartesianProduct(prv);// as columns of s  to a matrix
		List<Object> l= new ArrayList<Object>();
		l.add(1.0);
		l.add(0.0);
		l.add(0.0);

		ind= SimpleMatrix.diag(s.indexOf(l)); //origin

		// Deterministic case
		sd=prv.get(0);// as columns of s  to a matrix

		ind1 = SimpleMatrix.diag(sd.indexOf(0)); //origin
		switch(ranges.size()) {

		// max 3 rv's 
		case 3:
			//vectors
			int size = s.size();
			blockDim=size;
			DoubleArray s2_det = DoubleArray.copyOf(sd.stream().mapToDouble(num -> num).toArray());//fonds
			detVector= s2_det;
			DoubleArray s2 = DoubleArray.copyOf(Taylor.toArray(0,s));//fonds
			DoubleArray s3 = DoubleArray.copyOf(Taylor.toArray(1,s));//rente
			DoubleArray s4 = DoubleArray.copyOf(Taylor.toArray(2,s));//equity
			vectors.addAll(List.of(s2,s3,s4));
			//first order
			SimpleMatrix ds2S = SimpleMatrix.identity(prv.get(0).size());
			SimpleMatrix ds3S= SimpleMatrix.identity(prv.get(1).size());
			SimpleMatrix ds4S= SimpleMatrix.identity(prv.get(2).size());
			SimpleMatrix s2DerivFirstS = Taylor.UDFtoMatrixS(prv.get(0).size(),1,2,stepsizes.get(0)).kron(ds3S).kron(ds4S);//;
			SimpleMatrix s3DerivFirstS = ds2S.kron(Taylor.UDFtoMatrixS(prv.get(1).size(),1,2,stepsizes.get(1))).kron(ds4S);//.kron(ds4S));
			SimpleMatrix s4DerivFirstS = ds2S.kron(ds3S).kron(Taylor.UDFtoMatrixS(prv.get(2).size(),1,2,stepsizes.get(2)));//;

			// deterministic 
			firstOrderDet=  Taylor.UDFtoMatrixS(prv.get(0).size(),1,2,stepsizes.get(0));

			//rente
			SimpleMatrix dS= SimpleMatrix.identity(size);
			List<double[]> cop = Collections.nCopies(dS.getNumCols(), s3.multipliedBy(-b0).plus(a0).toArray());
			double[] vals = cop.stream().flatMapToDouble(x -> Arrays.stream(x)).toArray();
			SimpleMatrix sm1 = new SimpleMatrix(dS.getNumCols(), dS.getNumCols(), false, vals);
			SimpleMatrix s3S1=s3DerivFirstS.elementMult(sm1);

			//equity
			cop = Collections.nCopies(dS.getNumCols(), s4.multipliedBy(-b1).plus(a0).toArray());
			vals = cop.stream().flatMapToDouble(x -> Arrays.stream(x)).toArray();
			sm1 = new SimpleMatrix(dS.getNumCols(), dS.getNumCols(), false, vals);
			SimpleMatrix s4S1=s4DerivFirstS.elementMult(sm1);

			firstOrder.addAll(List.of(s2DerivFirstS,s3S1,s4S1));

			//second order
			SimpleMatrix s3DerivSecS =  ds2S.kron(Taylor.UDFtoMatrixS(prv.get(1).size(),2,3,stepsizes.get(1))).kron(ds4S);
			SimpleMatrix s4DerivSecS =  ds2S.kron(ds3S).kron(Taylor.UDFtoMatrixS(prv.get(2).size(),2,3,stepsizes.get(2)));

			//SimpleMatrix dSDet= SimpleMatrix.identity(g2.length);
			SimpleMatrix s3SS = s3DerivSecS.scale(.5*s0*s0);
			SimpleMatrix s4SS = s4DerivSecS.scale(.5*s1*s1);
			secondOrder.addAll(List.of(s3SS,s4SS));
			//determistic= 

		default:

		}

	}
	// static ReferenceData REF_DATA = ReferenceData.standard(); 
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


	static Surface qxM = addRefDatafromCSV("AG2018_man.csv","AAG-man");
	static Surface qxF = addRefDatafromCSV("AG2018_vrouw.csv","AAG-vrouw");
	static Surface pfM = addRefDatafromCSV("PORT_STERFTE_MAN_2019.csv","portf-man");
	static Surface pfF = addRefDatafromCSV("PORT_STERFTE_VRW_2019.csv","portf-vrouw");
	static Surface tar207 = addRefDatafromCSV("ZL1001SN.csv","207");
	static Surface tar220=	 addRefDatafromCSV("PREMTAB21POSNEG.csv","220");
	static Surface tar221	=addRefDatafromCSV("PREMTAB32NEG.csv","221");
	static Surface lapseM = addRefDatafromCSV("VERVAL_UL_2019M.csv","verv-man");
	static Surface lapseF = addRefDatafromCSV("VERVAL_UL_2019V.csv","verv-vrouw");



	 static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> IC =
			//end condition / initial condition  - not a function of t  
			new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
		@Override
		public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {
			ResolvedPolicy resolvedPolicy = data.getFirst();	
			double mr=resolvedPolicy.getMortalityRestitution();
			RatesProvider provider = data.getSecond();
			double gr = resolvedPolicy.getRateInvestementAccountGuaranteed();
			double dt= 0.25; // a fixed number - does not align with the dt of the ODE
			int exp = Period.between(provider.getValuationDate(),resolvedPolicy.getExpiryDate()).getYears();
			int age = Period.between(resolvedPolicy.getBirthDate(),provider.getValuationDate()).getYears();
			int expM = Period.between(provider.getValuationDate(),resolvedPolicy.getExpiryDate()).getMonths();
			int valYear = provider.getValuationDate().getYear();
			double steps = exp + dt*Math.floor(expM/(12*dt));	            
			double y0= 0;           
			if (gr!=0){
				y0=resolvedPolicy.getInvestementAccountProxy();
				for (double i = 0; i < steps; i=i+dt) {
					double tar = tar220.zValue(age+i, valYear+i);// Obviously hardcoded- must be derived from policy
					y0=(tar*(1-mr/100)+gr*(1+tar))*y0*dt +y0;
				} 
			}
			double GK=y0;    
			SimpleMatrix ic = new SimpleMatrix(blockDim,1,true,DoubleArray.copyOf(vectors.get(0).stream().map(i-> GK + Math.max(0,i-GK)).toArray()).toArray());  
			return ic;

			//return SimpleMatrix.filled(blockDim,1,0d);//.combine(0,0,ic).combine(blockDim,0,ic).combine(blockDim*2,0,ic);
		}
	};



	static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> R =
			//is state dependent. This case three states  -> three top blocks of R is needed

			new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
		@Override
		public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {
			double costF= 16.11;

			RatesProvider provider = data.getSecond();
			ResolvedPolicy resolvedPolicy = data.getFirst();	
			double accVal= resolvedPolicy.getInvestementAccount();
			Curve curve = provider.findData(CurveName.of("EUR-CPI")).get();
			int valYear = provider.getValuationDate().getYear();
			double inflation = curve.yValue(t)/100;
			int age = Period.between(resolvedPolicy.getBirthDate(),provider.getValuationDate()).getYears();
			double lapse = lapseM.zValue(age+t, valYear+t)/100;      	
			//R
//			SimpleMatrix Rm = SimpleMatrix.filled(blockDim, blockDim*3,0d)
//					.combine(0, blockDim*2, SimpleMatrix.diag(vectors.get(0).toArray()).scale(-lapse).scale(accVal)) 
//					.combine(0, 0, SimpleMatrix.identity(blockDim).scale(costF*inflation));
			SimpleMatrix Rm = new SimpleMatrix(blockDim,1,true,vectors.get(0).toArray()).scale(accVal).scale(lapse).plus(costF*inflation);
		    // here it helps collapsing into a vector for speed
			//.combine(blockDim, blockDim, SimpleMatrix.identity(blockDim).scale(costF*inflation))
			//.combine(blockDim*2, blockDim*2, SimpleMatrix.identity(blockDim).scale(costF*inflation));
			//Rm.convertToSparse();
			return Rm;


		}
	};

	 static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> R_F =
			//only active state  
			new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
		@Override
		public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {
			double costF= 16.11;
			RatesProvider provider = data.getSecond();
			ResolvedPolicy resolvedPolicy = data.getFirst();	
			Curve curve = provider.findData(CurveName.of("EUR-CPI")).get();
			int valYear = provider.getValuationDate().getYear();
			double inflation = curve.yValue(t)/100;
			int age = Period.between(resolvedPolicy.getBirthDate(),provider.getValuationDate()).getYears();
			double lapse = lapseM.zValue(age+t, valYear+t)/100;
			return new SimpleMatrix(1,3,true,costF*inflation,0,lapse);


		}
	};			        


	 static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> M =
			//is state dependent. This case three states  
			new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
		@Override
		public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {

			RatesProvider provider = data.getSecond();
			ResolvedPolicy resolvedPolicy = data.getFirst();	
			int valYear = provider.getValuationDate().getYear();			        
			int age = Period.between(resolvedPolicy.getBirthDate(),provider.getValuationDate()).getYears();
			double qx = qxM.zValue(t+age, valYear+t);
			double lapse = lapseM.zValue(age+t, valYear+t)/100;
			double portfCorr = pfM.zValue(age+t, valYear+t)/100; 
			return SimpleMatrix.identity(blockDim).scale(-(qx*portfCorr+lapse));


		}
	};

	 static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> M_F =
			//is state dependent. This case three states  
			new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
		@Override
		public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {

			RatesProvider provider = data.getSecond();
			ResolvedPolicy resolvedPolicy = data.getFirst();	
			int valYear = provider.getValuationDate().getYear();			        
			int age = Period.between(resolvedPolicy.getBirthDate(),provider.getValuationDate()).getYears();
			double qx = qxM.zValue(t+age, valYear+t);
			double lapse = lapseM.zValue(age+t, valYear+t)/100;
			double portfCorr = pfM.zValue(age+t, valYear+t)/100; 
			return SimpleMatrix.diag(-(qx*portfCorr+lapse));


		}
	};

	 static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> IR =
			//only active state  
			new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
		@Override
		public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {
			return SimpleMatrix.diag(vectors.get(1).toArray());
			//			        	 return SimpleMatrix.filled(blockDim*3,blockDim*3,0d)
			//				        		 .combine(0, 0, SimpleMatrix.diag(vectors.get(1).toArray()))
			//				        		.combine(blockDim, blockDim, SimpleMatrix.diag(vectors.get(1).toArray()))
			//				        		 .combine(blockDim*2, blockDim*2, SimpleMatrix.diag(vectors.get(1).toArray()));



		}
	};			        
	 static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> IR_F =
			//only active state  
			new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
		@Override
		public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {
			RatesProvider provider = data.getSecond();
			Curve fwdCurve = provider.findData(CurveName.of("ESG")).get();
			return SimpleMatrix.identity(blockDim).scale(fwdCurve.yValue(t));
		}
	};			        



	 static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> D_F =
			new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
		// all differentiation matrices, deliberately dimension equal to one state, as separate D functions  per state may be required 

		@Override
		public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {
			ResolvedPolicy resolvedPolicy = data.getFirst();
			RatesProvider provider = data.getSecond();
			double terbeh = resolvedPolicy.getExpenseRateinvestementAccount();
			double accVal= resolvedPolicy.getInvestementAccount();
			Curve fwdCurve = provider.findData(CurveName.of("ESG")).get();
			double fwd1= fwdCurve.yValue(t);
			double scaleVal=1d/accVal;
			int age = Period.between(resolvedPolicy.getBirthDate(),provider.getValuationDate()).getYears();
			int valYear = provider.getValuationDate().getYear();
			double qx = qxM.zValue(t+age, valYear+t);
			double tar = tar220.zValue(age+t, valYear+t);
			//fund
			SimpleMatrix s3DerivFirstS = firstOrder.get(0).scale(scaleVal);
			List<double[]> cop = Collections.nCopies(blockDim, vectors.get(0).multipliedBy(accVal).multipliedBy(-terbeh -tar*qx + fwd1).toArray());
			double[] vals = cop.stream().flatMapToDouble(x -> Arrays.stream(x)).toArray();
			SimpleMatrix sm1 = new SimpleMatrix(blockDim, blockDim, false, vals);
			SimpleMatrix fundD=s3DerivFirstS.elementMult(sm1);
			// only fund 
			SimpleMatrix Dt = fundD;
			return Dt;
			//        return SimpleMatrix.filled(blockDim*3, blockDim*3,0d).combine(0, 0, Dt).
			//          		 combine(blockDim, blockDim, Dt).combine(blockDim*2, blockDim*2, Dt);
		}
	};
	 static SimpleMatrix preD= firstOrder.get(1).plus(firstOrder.get(2)).plus(secondOrder.get(0)).plus(secondOrder.get(1));
	 static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> D =
			new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
		// all differentiation matrices, deliberately dimension equal to one state, as separate D functions  per state may be required 

		@Override
		public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {
			ResolvedPolicy resolvedPolicy = data.getFirst();
			RatesProvider provider = data.getSecond();
			double terbeh = resolvedPolicy.getExpenseRateinvestementAccount();
			double accVal= resolvedPolicy.getInvestementAccount();

			double scaleVal=1d/accVal;
			int age = Period.between(resolvedPolicy.getBirthDate(),provider.getValuationDate()).getYears();
			int valYear = provider.getValuationDate().getYear();
			double qx = qxM.zValue(t+age, valYear+t);
			double tar = tar220.zValue(age+t, valYear+t);
			//fund
			SimpleMatrix s3DerivFirstS = firstOrder.get(0).scale(scaleVal);
			List<double[]> cop = Collections.nCopies(blockDim, vectors.get(0).multipliedBy(accVal).multipliedBy(vectors.get(1).plus(vectors.get(2)).dividedBy(2).plus(-terbeh -tar*qx )).toArray());
			double[] vals = cop.stream().flatMapToDouble(x -> Arrays.stream(x)).toArray();
			SimpleMatrix sm1 = new SimpleMatrix(blockDim, blockDim, false, vals);
			SimpleMatrix fundD=s3DerivFirstS.elementMult(sm1);

			SimpleMatrix Dt = fundD.plus(preD);
			return Dt;
			//	          return SimpleMatrix.filled(blockDim*3, blockDim*3,0d).combine(0, 0, Dt).
			//	            		 combine(blockDim, blockDim, Dt).combine(blockDim*2, blockDim*2, Dt);
		}
	};
	 static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> Derivative =
			//only active state  
			new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
		@Override
		public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {
			//derivate with respect to perturbation p
			ResolvedPolicy resolvedPolicy = data.getFirst();
			RatesProvider provider = data.getSecond();
			double terbeh = resolvedPolicy.getExpenseRateinvestementAccount();
			double accVal= resolvedPolicy.getInvestementAccount();

			double scaleVal=1d/accVal;
			int age = Period.between(resolvedPolicy.getBirthDate(),provider.getValuationDate()).getYears();
			int valYear = provider.getValuationDate().getYear();
			double qx = qxM.zValue(t+age, valYear+t);
			double tar = tar220.zValue(age+t, valYear+t);
			//fund
			SimpleMatrix s3DerivFirstS = firstOrder.get(0).scale(scaleVal);
			List<double[]> cop = Collections.nCopies(blockDim, vectors.get(0).toArray());
			double[] vals = cop.stream().flatMapToDouble(x -> Arrays.stream(x)).toArray();
			SimpleMatrix sm1 = new SimpleMatrix(blockDim, blockDim, false, vals);
			SimpleMatrix der=s3DerivFirstS.elementMult(sm1);

			SimpleMatrix Der = der.minus(SimpleMatrix.identity(blockDim));

			return Der;
			//			        	 return SimpleMatrix.filled(blockDim*3,blockDim*3,0d)
			//				        		 .combine(0, 0, SimpleMatrix.diag(vectors.get(1).toArray()))
			//				        		.combine(blockDim, blockDim, SimpleMatrix.diag(vectors.get(1).toArray()))
			//				        		 .combine(blockDim*2, blockDim*2, SimpleMatrix.diag(vectors.get(1).toArray()));

		}
	};		
	 static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> Ind =
			//only active state  
			new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
		@Override
		public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {		
			return ind;
		}
	};
	static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> Zero =
			//only active state  
			new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
		@Override
		public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {		
			return SimpleMatrix.filled(blockDim,blockDim,0d);
		}
	};

}
