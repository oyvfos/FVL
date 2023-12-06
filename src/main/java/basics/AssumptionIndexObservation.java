package basics;

/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

import java.io.Serializable;
import java.time.YearMonth;
import java.util.Map;
import java.util.NoSuchElementException;

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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IndexObservation;

/**
 * Information about a single observation of a Price index.
 * <p>
 * Observing a Price index requires knowledge of the index and fixing date.
 */
@BeanDefinition(builderScope = "private")
public final class AssumptionIndexObservation
    implements IndexObservation, ImmutableBean, Serializable {

  /**
   * The FX index.
   * <p>
   * The rate will be queried from this index.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final AssumptionIndex index;
  /**
   * The fixing month.
   * <p>
   * The index will be observed for this month.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth fixingMonth;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance from an index and fixing date.
   * <p>
   * The reference data is used to find the maturity date from the fixing date.
   * 
   * @param index  the index
   * @param fixingMonth  the fixing month
   * @return the rate observation
   */
  public static AssumptionIndexObservation of(AssumptionIndex index, YearMonth fixingMonth) {
    return new AssumptionIndexObservation(index, fixingMonth);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the Ibor index.
   * 
   * @return the currency of the index
   */
  public Currency getCurrency() {
    return index.getCurrency();
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this observation to another based on the index and fixing date.
   * <p>
   * The maturity date is ignored.
   * 
   * @param obj  the other observation
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      AssumptionIndexObservation other = (AssumptionIndexObservation) obj;
      return index.equals(other.index) && fixingMonth.equals(other.fixingMonth);
    }
    return false;
  }

  /**
   * Returns a hash code based on the index and fixing date.
   * <p>
   * The maturity date is ignored.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + index.hashCode();
    return hash * 31 + fixingMonth.hashCode();
  }

  @Override
  public String toString() {
    return new StringBuilder(64)
        .append("AssumptionIndexObservation[")
        .append(index)
        .append(" on ")
        .append(fixingMonth)
        .append(']')
        .toString();
  }

  
  //-------------------------- AUTOGENERATED END --------------------------
    //------------------------- AUTOGENERATED START -------------------------
    /**
     * The meta-bean for {@code AssumptionIndexObservation}.
     * @return the meta-bean, not null
     */
    public static AssumptionIndexObservation.Meta meta() {
        return AssumptionIndexObservation.Meta.INSTANCE;
    }

    static {
        MetaBean.register(AssumptionIndexObservation.Meta.INSTANCE);
    }

    /**
     * The serialization version id.
     */
    private static final long serialVersionUID = 1L;

    private AssumptionIndexObservation(
            AssumptionIndex index,
            YearMonth fixingMonth) {
        JodaBeanUtils.notNull(index, "index");
        JodaBeanUtils.notNull(fixingMonth, "fixingMonth");
        this.index = index;
        this.fixingMonth = fixingMonth;
    }

    @Override
    public AssumptionIndexObservation.Meta metaBean() {
        return AssumptionIndexObservation.Meta.INSTANCE;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the FX index.
     * <p>
     * The rate will be queried from this index.
     * @return the value of the property, not null
     */
    @Override
    public AssumptionIndex getIndex() {
        return index;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the fixing month.
     * <p>
     * The index will be observed for this month.
     * @return the value of the property, not null
     */
    public YearMonth getFixingMonth() {
        return fixingMonth;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-bean for {@code AssumptionIndexObservation}.
     */
    public static final class Meta extends DirectMetaBean {
        /**
         * The singleton instance of the meta-bean.
         */
        static final Meta INSTANCE = new Meta();

        /**
         * The meta-property for the {@code index} property.
         */
        private final MetaProperty<AssumptionIndex> index = DirectMetaProperty.ofImmutable(
                this, "index", AssumptionIndexObservation.class, AssumptionIndex.class);
        /**
         * The meta-property for the {@code fixingMonth} property.
         */
        private final MetaProperty<YearMonth> fixingMonth = DirectMetaProperty.ofImmutable(
                this, "fixingMonth", AssumptionIndexObservation.class, YearMonth.class);
        /**
         * The meta-properties.
         */
        private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
                this, null,
                "index",
                "fixingMonth");

        /**
         * Restricted constructor.
         */
        private Meta() {
        }

        @Override
        protected MetaProperty<?> metaPropertyGet(String propertyName) {
            switch (propertyName.hashCode()) {
                case 100346066:  // index
                    return index;
                case 265281235:  // fixingMonth
                    return fixingMonth;
            }
            return super.metaPropertyGet(propertyName);
        }

        @Override
        public BeanBuilder<? extends AssumptionIndexObservation> builder() {
            return new AssumptionIndexObservation.Builder();
        }

        @Override
        public Class<? extends AssumptionIndexObservation> beanType() {
            return AssumptionIndexObservation.class;
        }

        @Override
        public Map<String, MetaProperty<?>> metaPropertyMap() {
            return metaPropertyMap$;
        }

        //-----------------------------------------------------------------------
        /**
         * The meta-property for the {@code index} property.
         * @return the meta-property, not null
         */
        public MetaProperty<AssumptionIndex> index() {
            return index;
        }

        /**
         * The meta-property for the {@code fixingMonth} property.
         * @return the meta-property, not null
         */
        public MetaProperty<YearMonth> fixingMonth() {
            return fixingMonth;
        }

        //-----------------------------------------------------------------------
        @Override
        protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
            switch (propertyName.hashCode()) {
                case 100346066:  // index
                    return ((AssumptionIndexObservation) bean).getIndex();
                case 265281235:  // fixingMonth
                    return ((AssumptionIndexObservation) bean).getFixingMonth();
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
     * The bean-builder for {@code AssumptionIndexObservation}.
     */
    private static final class Builder extends DirectPrivateBeanBuilder<AssumptionIndexObservation> {

        private AssumptionIndex index;
        private YearMonth fixingMonth;

        /**
         * Restricted constructor.
         */
        private Builder() {
        }

        //-----------------------------------------------------------------------
        @Override
        public Object get(String propertyName) {
            switch (propertyName.hashCode()) {
                case 100346066:  // index
                    return index;
                case 265281235:  // fixingMonth
                    return fixingMonth;
                default:
                    throw new NoSuchElementException("Unknown property: " + propertyName);
            }
        }

        @Override
        public Builder set(String propertyName, Object newValue) {
            switch (propertyName.hashCode()) {
                case 100346066:  // index
                    this.index = (AssumptionIndex) newValue;
                    break;
                case 265281235:  // fixingMonth
                    this.fixingMonth = (YearMonth) newValue;
                    break;
                default:
                    throw new NoSuchElementException("Unknown property: " + propertyName);
            }
            return this;
        }

        @Override
        public AssumptionIndexObservation build() {
            return new AssumptionIndexObservation(
                    index,
                    fixingMonth);
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(96);
            buf.append("AssumptionIndexObservation.Builder{");
            buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
            buf.append("fixingMonth").append('=').append(JodaBeanUtils.toString(fixingMonth));
            buf.append('}');
            return buf.toString();
        }

    }

    //-------------------------- AUTOGENERATED END --------------------------
}
