package liabilities;

/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */


import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.function.DoubleUnaryOperator;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Dummy point sensitivity implementation.
 * Based on zero-rate sensitivity.
 */
@BeanDefinition(style = "light")
public final class DummyPointSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The currency of the curve for which the sensitivity is computed.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency curveCurrency;
  /**
   * The date that was looked up on the curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate date;
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
   * Obtains an instance from the curve currency, date and value.
   * <p>
   * The currency representing the curve is used also for the sensitivity currency.
   * 
   * @param currency  the currency of the curve and sensitivity
   * @param date  the date that was looked up on the curve
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static DummyPointSensitivity of(Currency currency, LocalDate date, double sensitivity) {
    return new DummyPointSensitivity(currency, date, currency, sensitivity);
  }

  /**
   * Obtains an instance from the curve currency, date, sensitivity currency and value.
   * <p>
   * The currency representing the curve is used also for the sensitivity currency.
   * 
   * @param curveCurrency  the currency of the curve
   * @param date  the date that was looked up on the curve
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static DummyPointSensitivity of(
      Currency curveCurrency,
      LocalDate date,
      Currency sensitivityCurrency,
      double sensitivity) {
    return new DummyPointSensitivity(curveCurrency, date, sensitivityCurrency, sensitivity);
  }

  //-------------------------------------------------------------------------
  @Override
  public DummyPointSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new DummyPointSensitivity(curveCurrency, date, currency, sensitivity);
  }

  @Override
  public DummyPointSensitivity withSensitivity(double sensitivity) {
    return new DummyPointSensitivity(curveCurrency, date, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof DummyPointSensitivity) {
      DummyPointSensitivity otherZero = (DummyPointSensitivity) other;
      return ComparisonChain.start()
          .compare(curveCurrency, otherZero.curveCurrency)
          .compare(currency, otherZero.currency)
          .compare(date, otherZero.date)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public DummyPointSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (DummyPointSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public DummyPointSensitivity multipliedBy(double factor) {
    return new DummyPointSensitivity(curveCurrency, date, currency, sensitivity * factor);
  }

  @Override
  public DummyPointSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new DummyPointSensitivity(curveCurrency, date, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public DummyPointSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public DummyPointSensitivity cloned() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code DummyPointSensitivity}.
   */
  private static final TypedMetaBean<DummyPointSensitivity> META_BEAN =
      LightMetaBean.of(
          DummyPointSensitivity.class,
          MethodHandles.lookup(),
          new String[] {
              "curveCurrency",
              "date",
              "currency",
              "sensitivity"},
          new Object[0]);

  /**
   * The meta-bean for {@code DummyPointSensitivity}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<DummyPointSensitivity> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private DummyPointSensitivity(
      Currency curveCurrency,
      LocalDate date,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(curveCurrency, "curveCurrency");
    JodaBeanUtils.notNull(date, "date");
    JodaBeanUtils.notNull(currency, "currency");
    this.curveCurrency = curveCurrency;
    this.date = date;
    this.currency = currency;
    this.sensitivity = sensitivity;
  }

  @Override
  public TypedMetaBean<DummyPointSensitivity> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the curve for which the sensitivity is computed.
   * @return the value of the property, not null
   */
  public Currency getCurveCurrency() {
    return curveCurrency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that was looked up on the curve.
   * @return the value of the property, not null
   */
  public LocalDate getDate() {
    return date;
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
      DummyPointSensitivity other = (DummyPointSensitivity) obj;
      return JodaBeanUtils.equal(curveCurrency, other.curveCurrency) &&
          JodaBeanUtils.equal(date, other.date) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(curveCurrency);
    hash = hash * 31 + JodaBeanUtils.hashCode(date);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("DummyPointSensitivity{");
    buf.append("curveCurrency").append('=').append(JodaBeanUtils.toString(curveCurrency)).append(',').append(' ');
    buf.append("date").append('=').append(JodaBeanUtils.toString(date)).append(',').append(' ');
    buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-------------------------- AUTOGENERATED END --------------------------
}
