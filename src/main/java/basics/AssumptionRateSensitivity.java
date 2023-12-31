package basics;

/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.DoubleUnaryOperator;

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

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Point sensitivity to a rate from a price index curve.
 * <p>
 * Holds the sensitivity to the {@link PriceIndex} curve at a reference month.
 */
@BeanDefinition(builderScope = "private")
public final class AssumptionRateSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The Price index observation.
   * <p>
   * This includes the index and fixing month.
   */
  @PropertyDefinition(validate = "notNull")
  private final AssumptionIndexObservation observation;
  /**
   * The currency of the sensitivity.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The value of the sensitivity.
   */
  @PropertyDefinition(overrideGet = true)
  private final double sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the observation and sensitivity value.
   * <p>
   * The currency is defaulted from the index.
   * 
   * @param observation  the rate observation, including the fixing date
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static AssumptionRateSensitivity of(AssumptionIndexObservation observation, double sensitivity) {
    return new AssumptionRateSensitivity(observation, observation.getCurrency(), sensitivity);
  }

  /**
   * Obtains an instance from the observation and sensitivity value,
   * specifying the currency of the value.
   * 
   * @param observation  the rate observation, including the fixing date
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static AssumptionRateSensitivity of(
      AssumptionIndexObservation observation,
      Currency sensitivityCurrency,
      double sensitivity) {

    return new AssumptionRateSensitivity(observation, sensitivityCurrency, sensitivity);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the Ibor index that the sensitivity refers to.
   * 
   * @return the Ibor index
   */
  public AssumptionIndex getIndex() {
    return observation.getIndex();
  }

  //-------------------------------------------------------------------------
  @Override
  public AssumptionRateSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new AssumptionRateSensitivity(observation, currency, sensitivity);
  }

  @Override
  public AssumptionRateSensitivity withSensitivity(double sensitivity) {
    return new AssumptionRateSensitivity(observation, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof AssumptionRateSensitivity) {
      AssumptionRateSensitivity otherInflation = (AssumptionRateSensitivity) other;
      return ComparisonChain.start()
          .compare(getIndex().toString(), otherInflation.getIndex().toString())
          .compare(currency, otherInflation.currency)
          .compare(observation.getFixingMonth(), otherInflation.observation.getFixingMonth())
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public AssumptionRateSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (AssumptionRateSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public AssumptionRateSensitivity multipliedBy(double factor) {
    return new AssumptionRateSensitivity(observation, currency, sensitivity * factor);
  }

  @Override
  public AssumptionRateSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new AssumptionRateSensitivity(observation, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public AssumptionRateSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public AssumptionRateSensitivity cloned() {
    return this;
  }


  //-------------------------- AUTOGENERATED END --------------------------
    //------------------------- AUTOGENERATED START -------------------------
    /**
     * The meta-bean for {@code AssumptionRateSensitivity}.
     * @return the meta-bean, not null
     */
    public static AssumptionRateSensitivity.Meta meta() {
        return AssumptionRateSensitivity.Meta.INSTANCE;
    }

    static {
        MetaBean.register(AssumptionRateSensitivity.Meta.INSTANCE);
    }

    /**
     * The serialization version id.
     */
    private static final long serialVersionUID = 1L;

    private AssumptionRateSensitivity(
            AssumptionIndexObservation observation2,
            Currency currency,
            double sensitivity) {
        JodaBeanUtils.notNull(observation2, "observation");
        JodaBeanUtils.notNull(currency, "currency");
        this.observation = observation2;
        this.currency = currency;
        this.sensitivity = sensitivity;
    }

    @Override
    public AssumptionRateSensitivity.Meta metaBean() {
        return AssumptionRateSensitivity.Meta.INSTANCE;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the Price index observation.
     * <p>
     * This includes the index and fixing month.
     * @return the value of the property, not null
     */
    public AssumptionIndexObservation getObservation() {
        return observation;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the currency of the sensitivity.
     * @return the value of the property, not null
     */
    @Override
    public Currency getCurrency() {
        return currency;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the value of the sensitivity.
     * @return the value of the property
     */
    @Override
    public double getSensitivity() {
        return sensitivity;
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && obj.getClass() == this.getClass()) {
            AssumptionRateSensitivity other = (AssumptionRateSensitivity) obj;
            return JodaBeanUtils.equal(observation, other.observation) &&
                    JodaBeanUtils.equal(currency, other.currency) &&
                    JodaBeanUtils.equal(sensitivity, other.sensitivity);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = getClass().hashCode();
        hash = hash * 31 + JodaBeanUtils.hashCode(observation);
        hash = hash * 31 + JodaBeanUtils.hashCode(currency);
        hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(128);
        buf.append("AssumptionRateSensitivity{");
        buf.append("observation").append('=').append(observation).append(',').append(' ');
        buf.append("currency").append('=').append(currency).append(',').append(' ');
        buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
        buf.append('}');
        return buf.toString();
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-bean for {@code AssumptionRateSensitivity}.
     */
    public static final class Meta extends DirectMetaBean {
        /**
         * The singleton instance of the meta-bean.
         */
        static final Meta INSTANCE = new Meta();

        /**
         * The meta-property for the {@code observation} property.
         */
        private final MetaProperty<PriceIndexObservation> observation = DirectMetaProperty.ofImmutable(
                this, "observation", AssumptionRateSensitivity.class, PriceIndexObservation.class);
        /**
         * The meta-property for the {@code currency} property.
         */
        private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
                this, "currency", AssumptionRateSensitivity.class, Currency.class);
        /**
         * The meta-property for the {@code sensitivity} property.
         */
        private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
                this, "sensitivity", AssumptionRateSensitivity.class, Double.TYPE);
        /**
         * The meta-properties.
         */
        private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
                this, null,
                "observation",
                "currency",
                "sensitivity");

        /**
         * Restricted constructor.
         */
        private Meta() {
        }

        @Override
        protected MetaProperty<?> metaPropertyGet(String propertyName) {
            switch (propertyName.hashCode()) {
                case 122345516:  // observation
                    return observation;
                case 575402001:  // currency
                    return currency;
                case 564403871:  // sensitivity
                    return sensitivity;
            }
            return super.metaPropertyGet(propertyName);
        }

        @Override
        public BeanBuilder<? extends AssumptionRateSensitivity> builder() {
            return new AssumptionRateSensitivity.Builder();
        }

        @Override
        public Class<? extends AssumptionRateSensitivity> beanType() {
            return AssumptionRateSensitivity.class;
        }

        @Override
        public Map<String, MetaProperty<?>> metaPropertyMap() {
            return metaPropertyMap$;
        }

        //-----------------------------------------------------------------------
        /**
         * The meta-property for the {@code observation} property.
         * @return the meta-property, not null
         */
        public MetaProperty<PriceIndexObservation> observation() {
            return observation;
        }

        /**
         * The meta-property for the {@code currency} property.
         * @return the meta-property, not null
         */
        public MetaProperty<Currency> currency() {
            return currency;
        }

        /**
         * The meta-property for the {@code sensitivity} property.
         * @return the meta-property, not null
         */
        public MetaProperty<Double> sensitivity() {
            return sensitivity;
        }

        //-----------------------------------------------------------------------
        @Override
        protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
            switch (propertyName.hashCode()) {
                case 122345516:  // observation
                    return ((AssumptionRateSensitivity) bean).getObservation();
                case 575402001:  // currency
                    return ((AssumptionRateSensitivity) bean).getCurrency();
                case 564403871:  // sensitivity
                    return ((AssumptionRateSensitivity) bean).getSensitivity();
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
     * The bean-builder for {@code AssumptionRateSensitivity}.
     */
    private static final class Builder extends DirectPrivateBeanBuilder<AssumptionRateSensitivity> {

        private AssumptionIndexObservation observation;
        private Currency currency;
        private double sensitivity;

        /**
         * Restricted constructor.
         */
        private Builder() {
        }

        //-----------------------------------------------------------------------
        @Override
        public Object get(String propertyName) {
            switch (propertyName.hashCode()) {
                case 122345516:  // observation
                    return observation;
                case 575402001:  // currency
                    return currency;
                case 564403871:  // sensitivity
                    return sensitivity;
                default:
                    throw new NoSuchElementException("Unknown property: " + propertyName);
            }
        }

        @Override
        public Builder set(String propertyName, Object newValue) {
            switch (propertyName.hashCode()) {
                case 122345516:  // observation
                    this.observation = (AssumptionIndexObservation) newValue;
                    break;
                case 575402001:  // currency
                    this.currency = (Currency) newValue;
                    break;
                case 564403871:  // sensitivity
                    this.sensitivity = (Double) newValue;
                    break;
                default:
                    throw new NoSuchElementException("Unknown property: " + propertyName);
            }
            return this;
        }

        @Override
        public AssumptionRateSensitivity build() {
            return new AssumptionRateSensitivity(
                    observation,
                    currency,
                    sensitivity);
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(128);
            buf.append("AssumptionRateSensitivity.Builder{");
            buf.append("observation").append('=').append(JodaBeanUtils.toString(observation)).append(',').append(' ');
            buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
            buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
            buf.append('}');
            return buf.toString();
        }

    }

    //-------------------------- AUTOGENERATED END --------------------------
}
