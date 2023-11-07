package product;

import java.io.Serializable;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.ejml.simple.SimpleMatrix;
import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;
import com.opengamma.strata.pricer.rate.RatesProvider;

import liabilities.DifferentiationMatrixId;
import liabilities.NonObservableId;
import liabilities.TransitionRatesId;
import utilities.HWAnalytical;
import utilities.Taylor;
@BeanDefinition(builderScope = "private")
public final class StochasticPIDEComputation
implements PolicyComputation, ImmutableBean, Serializable {

  // must be a bean 

  /**
   * The fixed rate to be paid.
   * A 5% rate will be expressed as 0.05.
   */
  @PropertyDefinition
  private final double rate;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param rate  the fixed rate
   * @return the fixed rate computation
   */
  public static StochasticPIDEComputation of(double rate) {
    return new StochasticPIDEComputation(rate);
  }

  //-------------------------------------------------------------------------
  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    // no indices to add
  }
  public CurrencyAmount presentValue(ResolvedPolicy fra, RatesProvider provider, ReferenceData refData, Pair<SimpleMatrix, SimpleMatrix> solutions) {    
		  int col = solutions.getFirst().getNumCols();
		  List<BiFunction<Pair<ResolvedPolicy, RatesProvider>, Double, SimpleMatrix>> funcs =  refData
				  .getValue(DifferentiationMatrixId.of("OG-Ticker", fra.getConvention().getName())).getDifferenationMatrix() ;  
	      BiFunction<Pair<ResolvedPolicy, RatesProvider>, Double, SimpleMatrix> Indf = funcs.get(6);// fixed place
	      int ind = (int)Indf.apply(Pair.of(fra,provider),0d).get(0, 0);
		  if (col==1) ind=0;
		  return CurrencyAmount.of(Currency.EUR, solutions.getFirst().cols(col-1,col).get(ind)*-1);		
	  }
  public PointSensitivityBuilder sensBuilder(ResolvedPolicy resolvedPolicy, RatesProvider provider,ReferenceData refData, Pair<SimpleMatrix, SimpleMatrix> diffMat) {
      double dt = provider.data(NonObservableId.of("TimeStep"));
      if (diffMat.getSecond().getNumRows()==1) return PointSensitivityBuilder.none();
      List<BiFunction<Pair<ResolvedPolicy, RatesProvider>, Double, SimpleMatrix>> funcs =  refData
    		  .getValue(DifferentiationMatrixId.of("OG-Ticker", resolvedPolicy.getConvention().getName())).getDifferenationMatrix() ;  
      BiFunction<Pair<ResolvedPolicy, RatesProvider>, Double, SimpleMatrix> IRDerf = funcs.get(5);// fixed place
      List<Double> l= new ArrayList<Double>();
      List<IborRateSensitivity> ps=Lists.newArrayList();
      double prevTmp=0;
      double tmp=0;
      int totalSteps= diffMat.getSecond().getNumCols() ;
      SimpleMatrix fwdDer= IRDerf.apply(Pair.of(resolvedPolicy,provider),0d);
      for (int i = 0; i < (totalSteps-1); i++) {
          prevTmp=tmp;
          tmp = diffMat.getSecond().cols(totalSteps-i-2, totalSteps-i-1).transpose().mult(fwdDer).mult(diffMat.getFirst().cols(i, i+1)).get(0,0);//*(-i/(Math.log(discountFactors.discountFactor(i+1))));
          l.add(tmp);
        IborIndexObservation obs = IborIndexObservation.of(IborIndices.EUR_EURIBOR_3M, provider.getValuationDate().plusMonths((long) (i*12*dt)).minusDays(4), refData);
        tmp=(i==0|i==totalSteps-2)?-tmp:tmp;
        ps.add(IborRateSensitivity.of(obs, Currency.EUR,  -tmp));
      }
      
    //double sum = l.stream().mapToDouble(f -> f.doubleValue()/10000).sum()*dt;
    //System.out.println(sum);
    return PointSensitivityBuilder.of(ps);
  }
 // Solves the PIDE
  public Pair<SimpleMatrix, SimpleMatrix> diffMat(ResolvedPolicy resolvedPolicy, RatesProvider provider, ReferenceData refData ) {
	  //long start = System.currentTimeMillis();
		    List<BiFunction<Pair<ResolvedPolicy, RatesProvider>, Double, SimpleMatrix>> funcs =  refData.getValue(DifferentiationMatrixId.of("OG-Ticker", resolvedPolicy.getConvention().getName())).getDifferenationMatrix() ;
          //List.of(IC,D,M, R,IR, IRDerivative, ind)
	       BiFunction<Pair<ResolvedPolicy, RatesProvider>, Double, SimpleMatrix> IC = funcs.get(0);//.apply(resolvedPolicy,1.0);
	       BiFunction<Pair<ResolvedPolicy, RatesProvider>, Double, SimpleMatrix> Df = funcs.get(1);//.apply(resolvedPolicy,1.0);
	       BiFunction<Pair<ResolvedPolicy, RatesProvider>, Double, SimpleMatrix> Mf = funcs.get(2);//.apply(resolvedPolicy,1.0);
	       BiFunction<Pair<ResolvedPolicy, RatesProvider>, Double, SimpleMatrix> Rf = funcs.get(3);//.apply(resolvedPolicy,1.0);
	       BiFunction<Pair<ResolvedPolicy, RatesProvider>, Double, SimpleMatrix> IRf = funcs.get(4);//.apply(resolvedPolicy,1.0);
	       BiFunction<Pair<ResolvedPolicy, RatesProvider>, Double, SimpleMatrix> Indf = funcs.get(6);//.apply(resolvedPolicy,1.0);    
	          int exp = Period.between(provider.getValuationDate(),resolvedPolicy.getExpiryDate()).getYears();
	          int expM = Period.between(provider.getValuationDate(),resolvedPolicy.getExpiryDate()).getMonths(); 
	          double dt = provider.data(NonObservableId.of("TimeStep"));
	          double steps = exp + dt*Math.floor(expM/(12*dt));
	          if (steps==0) return Pair.of(SimpleMatrix.diag(0),SimpleMatrix.diag(0));
	          SimpleMatrix endCond=IC.apply(Pair.of(resolvedPolicy,provider),0.0);
	          int blockDim = endCond.getNumRows();
         SimpleMatrix ySAdj = new SimpleMatrix(blockDim,1);
         ySAdj.set((int)Indf.apply(Pair.of(resolvedPolicy,provider),0d).get(0, 0), 0, 1);
         SimpleMatrix yS = endCond;
         SimpleMatrix fullY=yS;//yS         
         SimpleMatrix fullYadj=ySAdj;        
         SimpleMatrix IR = IRf.apply(Pair.of(resolvedPolicy,provider),0d);       
         for (double i = (steps-dt); i > -dt; i=i-dt) {
             double t=i;
             double t1=steps-t; 
             long start1 = System.currentTimeMillis();
             SimpleMatrix D = Df.apply(Pair.of(resolvedPolicy,provider),i);
             long end = System.currentTimeMillis();
             SimpleMatrix R = Rf.apply(Pair.of(resolvedPolicy,provider),i);
             SimpleMatrix M = Mf.apply(Pair.of(resolvedPolicy,provider),i);
             SimpleMatrix fR = Rf.apply(Pair.of(resolvedPolicy,provider),t1);
             SimpleMatrix fM = Mf.apply(Pair.of(resolvedPolicy,provider),t1);
             //long end = System.currentTimeMillis();
             //System.out.println((end-start1) + " msec");
             
             SimpleMatrix Id = SimpleMatrix.identity(blockDim);
             SimpleMatrix Ones =SimpleMatrix.ones(R.getNumCols(),1);
             
             SimpleMatrix y1=IR.minus(M).minus(D).scale(dt).plus(Id).mult(yS).minus(R.mult(Ones).scale(dt));
             SimpleMatrix y1Adj = IR.minus(fM).minus(D).scale(dt).plus(Id).mult(ySAdj).minus(fR.mult(Ones).scale(dt));
             yS=y1.scale(-1);
             ySAdj=y1Adj.scale(-1);
             fullYadj=fullYadj.concatColumns(y1Adj);
             fullY=fullY.concatColumns(y1)    ;
        
         }
         return Pair.of(fullY,fullYadj);
         
  }
  
    //------------------------- AUTOGENERATED START -------------------------
    /**
     * The meta-bean for {@code StochasticPIDEComputation}.
     * @return the meta-bean, not null
     */
    public static StochasticPIDEComputation.Meta meta() {
        return StochasticPIDEComputation.Meta.INSTANCE;
    }

    static {
        MetaBean.register(StochasticPIDEComputation.Meta.INSTANCE);
    }

    /**
     * The serialization version id.
     */
    private static final long serialVersionUID = 1L;

    private StochasticPIDEComputation(
            double rate) {
        this.rate = rate;
    }

    @Override
    public StochasticPIDEComputation.Meta metaBean() {
        return StochasticPIDEComputation.Meta.INSTANCE;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the fixed rate to be paid.
     * A 5% rate will be expressed as 0.05.
     * @return the value of the property
     */
    public double getRate() {
        return rate;
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && obj.getClass() == this.getClass()) {
            StochasticPIDEComputation other = (StochasticPIDEComputation) obj;
            return JodaBeanUtils.equal(rate, other.rate);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = getClass().hashCode();
        hash = hash * 31 + JodaBeanUtils.hashCode(rate);
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(64);
        buf.append("StochasticPIDEComputation{");
        buf.append("rate").append('=').append(JodaBeanUtils.toString(rate));
        buf.append('}');
        return buf.toString();
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-bean for {@code StochasticPIDEComputation}.
     */
    public static final class Meta extends DirectMetaBean {
        /**
         * The singleton instance of the meta-bean.
         */
        static final Meta INSTANCE = new Meta();

        /**
         * The meta-property for the {@code rate} property.
         */
        private final MetaProperty<Double> rate = DirectMetaProperty.ofImmutable(
                this, "rate", StochasticPIDEComputation.class, Double.TYPE);
        /**
         * The meta-properties.
         */
        private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
                this, null,
                "rate");

        /**
         * Restricted constructor.
         */
        private Meta() {
        }

        @Override
        protected MetaProperty<?> metaPropertyGet(String propertyName) {
            switch (propertyName.hashCode()) {
                case 3493088:  // rate
                    return rate;
            }
            return super.metaPropertyGet(propertyName);
        }

        @Override
        public BeanBuilder<? extends StochasticPIDEComputation> builder() {
            return new StochasticPIDEComputation.Builder();
        }

        @Override
        public Class<? extends StochasticPIDEComputation> beanType() {
            return StochasticPIDEComputation.class;
        }

        @Override
        public Map<String, MetaProperty<?>> metaPropertyMap() {
            return metaPropertyMap$;
        }

        //-----------------------------------------------------------------------
        /**
         * The meta-property for the {@code rate} property.
         * @return the meta-property, not null
         */
        public MetaProperty<Double> rate() {
            return rate;
        }

        //-----------------------------------------------------------------------
        @Override
        protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
            switch (propertyName.hashCode()) {
                case 3493088:  // rate
                    return ((StochasticPIDEComputation) bean).getRate();
            }
            return super.propertyGet(bean, propertyName, quiet);
        }

        @Override
        protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
            metaProperty(propertyName);
            if (quiet) {
                return;
            }
            throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
        }

    }

    //-----------------------------------------------------------------------
    /**
     * The bean-builder for {@code StochasticPIDEComputation}.
     */
    private static final class Builder extends DirectPrivateBeanBuilder<StochasticPIDEComputation> {

        private double rate;

        /**
         * Restricted constructor.
         */
        private Builder() {
        }

        //-----------------------------------------------------------------------
        @Override
        public Object get(String propertyName) {
            switch (propertyName.hashCode()) {
                case 3493088:  // rate
                    return rate;
                default:
                    throw new NoSuchElementException("Unknown property: " + propertyName);
            }
        }

        @Override
        public Builder set(String propertyName, Object newValue) {
            switch (propertyName.hashCode()) {
                case 3493088:  // rate
                    this.rate = (Double) newValue;
                    break;
                default:
                    throw new NoSuchElementException("Unknown property: " + propertyName);
            }
            return this;
        }

        @Override
        public StochasticPIDEComputation build() {
            return new StochasticPIDEComputation(
                    rate);
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(64);
            buf.append("StochasticPIDEComputation.Builder{");
            buf.append("rate").append('=').append(JodaBeanUtils.toString(rate));
            buf.append('}');
            return buf.toString();
        }

    }

    //-------------------------- AUTOGENERATED END --------------------------
      }

     
