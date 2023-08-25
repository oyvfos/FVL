package liabilities;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiFunction;

import org.ejml.simple.SimpleMatrix;
import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceInfoType;

import product.ResolvedPolicy;


@BeanDefinition(builderScope = "private")
public final class PolicyFunctions
    implements TransitionRates, ImmutableBean, Serializable {

  /**
   * The legal entity identifier.
   * <p>
   * This identifier is used for the reference legal entity of a credit derivative.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final TransitionRatesId transitionRatesId;
  
  /**
   * The valuation date.
   */
  /**
   * The recovery rate.
   * <p>
   * The recovery rate is represented in decimal form, and must be between 0 and 1 inclusive.
   */
  @PropertyDefinition
  private final List<BiFunction<ResolvedPolicy, Double, SimpleMatrix>> funcs;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance.
   * 
   * @param transitionRatesId  the legal entity identifier
   * @param valuationDate  the valuation date
   * @param recoveryRate  the recovery rate
   * @return the instance
   */
  public static PolicyFunctions of(TransitionRatesId transitionRatesId,  List<BiFunction<ResolvedPolicy, Double, SimpleMatrix>> funcs) {
    return new PolicyFunctions(transitionRatesId, funcs);
  }

  @ImmutableValidator
  private void validate() {
    //ArgChecker.inRangeInclusive(mortalityRates, 0d, 1d, "recoveryRate");
  }
  private PolicyFunctions(TransitionRatesId transitionRatesId,   List<BiFunction<ResolvedPolicy, Double, SimpleMatrix>> funcs) {

	    //ArgChecker.notNull(convention, "convention");
	    //ArgChecker.notNull(valuationDateTime, "valuationDateTime");
	    //ArgChecker.notNull(surface, "surface");
//	    surface.getMetadata().getXValueType().checkEquals(
//	        ValueType.YEAR_FRACTION, "Incorrect x-value type for Normal volatilities");
//	    surface.getMetadata().getYValueType().checkEquals(
//	        ValueType.YEAR_FRACTION, "Incorrect y-value type for Normal volatilities");
//	    surface.getMetadata().getZValueType().checkEquals(
//	        ValueType.NORMAL_VOLATILITY, "Incorrect z-value type for Normal volatilities");
//	    DayCount dayCount = surface.getMetadata().findInfo(SurfaceInfoType.DAY_COUNT)
//	        .orElseThrow(() -> new IllegalArgumentException("Incorrect surface metadata, missing DayCount"));
//
//	    this.valuationDate = valuationDate;
	    this.funcs = funcs;
	    this.transitionRatesId = transitionRatesId;
	    //this.dayCount = dayCount;
	  }
  //-------------------------------------------------------------------------
  //@Override
//  public SwaptionVolatilitiesName getName() {
//    return SwaptionVolatilitiesName.of(mortalityRates.getName().getName());
//  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    if (funcs.getName().equals(name)) {
      return Optional.of(name.getMarketDataType().cast(funcs));
    }
    return Optional.empty();
  }

//  @Override
//  public int getParameterCount() {
//    return mortalityRates.getParameterCount();
//  }
//
//  @Override
//  public double getParameter(int parameterIndex) {
//    return mortalityRates.getParameter(parameterIndex);
//  }

//  @Override
//  public ParameterMetadata getParameterMetadata(int parameterIndex) {
//    return mortalityRates.getParameterMetadata(parameterIndex);
//  }

//  @Override
//  public OptionalInt findParameterIndex(ParameterMetadata metadata) {
//    return mortalityRates.findParameterIndex(metadata);
//  }

//  @Override
//  public PolicyFunctions withParameter(int parameterIndex, double newValue) {
//    ArgChecker.isTrue(parameterIndex == 0, "Only one parameter for ConstantRecoveryRates");
//    return new PolicyFunctions(transitionRatesId, mortalityRates.withParameter(parameterIndex,newValue));
//  }

//  @Override
//  public PolicyFunctions withPerturbation(ParameterPerturbation perturbation) {
//	    return new PolicyFunctions(
//	    		transitionRatesId, mortalityRates.withPerturbation(perturbation));
//	  }
//
  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ConstantRecoveryRates}.
   * @return the meta-bean, not null
   */
  public static PolicyFunctions.Meta meta() {
    return PolicyFunctions.Meta.INSTANCE;
  }

  static {
    MetaBean.register(PolicyFunctions.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

//  private MortalityRates(
//      TransitionRatesId transitionRatesId,
//      LocalDate valuationDate,
//      Surface mortalityRates) {
//    JodaBeanUtils.notNull(transitionRatesId, "transitionRatesId");
//    JodaBeanUtils.notNull(valuationDate, "valuationDate");
//    this.transitionRatesId = transitionRatesId;
//    this.valuationDate = valuationDate;
//    this.mortalityRates = mortalityRates;
//    validate();
//  }

  @Override
  public PolicyFunctions.Meta metaBean() {
    return PolicyFunctions.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legal entity identifier.
   * <p>
   * This identifier is used for the reference legal entity of a credit derivative.
   * @return the value of the property, not null
   */
  @Override
  public TransitionRatesId getTransitionRatesId() {
    return transitionRatesId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the recovery rate.
   * <p>
   * The recovery rate is represented in decimal form, and must be between 0 and 1 inclusive.
   * @return the value of the property
   */
  public Surface getSurface() {
    return mortalityRates;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      PolicyFunctions other = (PolicyFunctions) obj;
      return JodaBeanUtils.equal(transitionRatesId, other.transitionRatesId) &&
          JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(mortalityRates, other.mortalityRates);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(transitionRatesId);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(mortalityRates);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("MortalityRates{");
    buf.append("transitionRatesId").append('=').append(JodaBeanUtils.toString(transitionRatesId)).append(',').append(' ');
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
    buf.append("mortalityRates").append('=').append(JodaBeanUtils.toString(mortalityRates));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ConstantRecoveryRates}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code transitionRatesId} property.
     */
    private final MetaProperty<TransitionRatesId> transitionRatesId = DirectMetaProperty.ofImmutable(
        this, "transitionRatesId", PolicyFunctions.class, TransitionRatesId.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", PolicyFunctions.class, LocalDate.class);
    /**
     * The meta-property for the {@code recoveryRate} property.
     */
    private final MetaProperty<Double> recoveryRate = DirectMetaProperty.ofImmutable(
        this, "mortalityRates", PolicyFunctions.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "transitionRatesId",
        "valuationDate",
        "mortalityRates");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 866287159:  // transitionRatesId
          return transitionRatesId;
        case 113107279:  // valuationDate
          return valuationDate;
        case 2002873877:  // recoveryRate
          //return mortalityRates;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends PolicyFunctions> builder() {
      return new PolicyFunctions.Builder();
    }

    @Override
    public Class<? extends PolicyFunctions> beanType() {
      return PolicyFunctions.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code transitionRatesId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<TransitionRatesId> transitionRatesId() {
      return transitionRatesId;
    }

    /**
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
    }

    /**
     * The meta-property for the {@code recoveryRate} property.
     * @return the meta-property, not null
     */
//    public MetaProperty<Surface> mortalityRates() {
//      return mortalityRates;
//    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 866287159:  // transitionRatesId
          return ((PolicyFunctions) bean).getTransitionRatesId();
        case 113107279:  // valuationDate
          return ((PolicyFunctions) bean).getValuationDate();
        case 2002873877:  // recoveryRate
          return ((PolicyFunctions) bean).getSurface();
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
   * The bean-builder for {@code ConstantRecoveryRates}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<PolicyFunctions> {

    private TransitionRatesId transitionRatesId;
    private LocalDate valuationDate;
    private Surface mortalityRates;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 866287159:  // transitionRatesId
          return transitionRatesId;
        case 113107279:  // valuationDate
          return valuationDate;
        case 2002873877:  // recoveryRate
          return mortalityRates;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 866287159:  // transitionRatesId
          this.transitionRatesId = (TransitionRatesId) newValue;
          break;
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
          break;
        case 2002873877:  // recoveryRate
          this.mortalityRates = (Surface) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public PolicyFunctions build() {
      return new PolicyFunctions(
          transitionRatesId,
          valuationDate,
          mortalityRates);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("ConstantRecoveryRates.Builder{");
      buf.append("transitionRatesId").append('=').append(JodaBeanUtils.toString(transitionRatesId)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("mortalityRates").append('=').append(JodaBeanUtils.toString(mortalityRates));
      buf.append('}');
      return buf.toString();
    }

  }

@Override
public int getParameterCount() {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public double getParameter(int parameterIndex) {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public ParameterMetadata getParameterMetadata(int parameterIndex) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public MortalityRates withParameter(int parameterIndex, double newValue) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public MortalityRates withPerturbation(ParameterPerturbation perturbation) {
	// TODO Auto-generated method stub
	return null;
}




//@Override
//public double recoveryRate(LocalDate date) {
//	// TODO Auto-generated method stub
//	return 0;
//}



  //-------------------------- AUTOGENERATED END --------------------------
}