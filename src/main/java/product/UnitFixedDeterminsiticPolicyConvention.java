package product;

import java.time.Period;
import java.util.function.BiFunction;

import org.ejml.simple.SimpleMatrix;

import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.pricer.rate.RatesProvider;



public class UnitFixedDeterminsiticPolicyConvention {
	private static final UnitLInkedPolicyConvention ul = new UnitLInkedPolicyConvention();
	static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> R =
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
			//double duration = exp + dt*Math.floor(expM/(12*dt));
			Curve fwdCurve = provider.findData(CurveName.of("ESG")).get();
			Curve eqCurve = provider.findData(CurveName.of("Equity")).get();
			double terbeh = resolvedPolicy.getExpenseRateinvestementAccount();
			// develop funds up to including t. for t= 0
			double y0 = resolvedPolicy.getInvestementAccount();
			double[] fv = new double[(int) (t/dt)]; 
			for (double i = 0; i < t+dt; i=i+dt) {
				double qx = ul.qxM.zValue(age+i, valYear+i);
				double fwd1= fwdCurve.yValue(i);
				double eq= eqCurve.yValue(i);
				double tar = ul.tar220.zValue(age+i, valYear+i);
				//qx=0.007;tar=0.8;terbeh=0.015;fwd1=0.005;
				fv[(int) (i/dt)]=y0;
				y0=(-terbeh -tar*qx + (fwd1 + eq)/2)*y0*dt +y0;
			}
			double costF= 16.11;
			Curve curve = provider.findData(CurveName.of("EUR-CPI")).get();
			double inflation = curve.yValue(t)/100;
			double lapse = ul.lapseM.zValue(age+t, valYear+t)/100;  
			
			return SimpleMatrix.diag(fv[(int) (t/dt)]).scale(lapse).plus(costF*inflation);	

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
			double qx = ul.qxM.zValue(t+age, valYear+t);
			double lapse = ul.lapseM.zValue(age+t, valYear+t)/100;
			double portfCorr = ul.pfM.zValue(age+t, valYear+t)/100; 
			return SimpleMatrix.diag(-(qx*portfCorr+lapse));


		}
	};
	static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> D =
			//is state dependent. This case three states  
			new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
		@Override
		public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {

						return SimpleMatrix.diag(0d);


		}
	};
	 static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> IR =
				//only active state  
				new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
			@Override
			public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {
				RatesProvider provider = data.getSecond();
				Curve fwdCurve = provider.findData(CurveName.of("ESG")).get();
				return SimpleMatrix.diag(fwdCurve.yValue(t));
			}
		};			
		static final BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix> Ind =
				//only active state  
				new BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>() {
			@Override
			public SimpleMatrix apply(Pair<ResolvedPolicy,RatesProvider> data, Double t) {
				
				return SimpleMatrix.diag(0d);
			}
		};	
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
				//Find guarantee
				if (gr!=0){
					y0=resolvedPolicy.getInvestementAccountProxy();
					for (double i = 0; i < steps; i=i+dt) {
						double tar = ul.tar220.zValue(age+i, valYear+i);// Tariff Obviously hardcoded- must be derived from policy
						y0=(tar*(1-mr/100)+gr*(1+tar))*y0*dt +y0;
					} 
				}
				double GK=y0;
				Curve fwdCurve = provider.findData(CurveName.of("ESG")).get();
				Curve eqCurve = provider.findData(CurveName.of("Equity")).get();
				double terbeh = resolvedPolicy.getExpenseRateinvestementAccount();
				//if (steps==0) return Pair.of(SimpleMatrix.diag(0),SimpleMatrix.diag(0));
				y0=resolvedPolicy.getInvestementAccount();
				double[] fv = new double[(int) (steps/dt)]; 
				for (double i = 0; i < steps; i=i+dt) {
					double qx = ul.qxM.zValue(age+i, valYear+i);
					double fwd= fwdCurve.yValue(i);
					double eq= eqCurve.yValue(i);
					double tar = ul.tar220.zValue(age+i, valYear+i);
					//qx=0.007;tar=0.8;terbeh=0.015;fwd1=0.005;
					fv[(int) (i/dt)]=y0;
					y0=(-terbeh -tar*qx + (fwd + eq)/2)*y0*dt +y0;

				}
				y0=GK + Math.max(0,fv[(int) (steps/dt)-1]-GK);

				return SimpleMatrix.diag(y0);  

				//return SimpleMatrix.filled(blockDim,1,0d);//.combine(0,0,ic).combine(blockDim,0,ic).combine(blockDim*2,0,ic);
			}
		};
			
}
