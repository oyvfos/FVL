package liabilities;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

import org.ejml.simple.SimpleMatrix;
import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.pricer.rate.RatesProvider;

import product.ResolvedPolicy;



@BeanDefinition
public final class DifferentiationMatrix implements ImmutableBean {

    /** The user identifier. */
    @PropertyDefinition(validate = "notNull")
    private final DifferentiationMatrixId differenationMatrixId;

   
    /** The number of logins. */
    @PropertyDefinition
    private final List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>> differenationMatrix;
    
    public static DifferentiationMatrix of(DifferentiationMatrixId differenationMatrixId,
            List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>> differenationMatrix) {
        return new DifferentiationMatrix(differenationMatrixId, differenationMatrix);
      }
    
//    public static DifferentiationMatrix of(range, stepsize ) {
//        return new DifferentiationMatrix(differenationMatrixId, differenationMatrix, calcFunction);
//      }
    
    //------------------------- AUTOGENERATED START -------------------------
    /**
     * The meta-bean for {@code DifferentiationMatrix}.
     * @return the meta-bean, not null
     */
    public static DifferentiationMatrix.Meta meta() {
        return DifferentiationMatrix.Meta.INSTANCE;
    }

    static {
        MetaBean.register(DifferentiationMatrix.Meta.INSTANCE);
    }

    /**
     * Returns a builder used to create an instance of the bean.
     * @return the builder, not null
     */
    public static DifferentiationMatrix.Builder builder() {
        return new DifferentiationMatrix.Builder();
    }

    private DifferentiationMatrix(
            DifferentiationMatrixId differenationMatrixId,
            List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>> differenationMatrix) {
        JodaBeanUtils.notNull(differenationMatrixId, "differenationMatrixId");
        this.differenationMatrixId = differenationMatrixId;
        this.differenationMatrix = (differenationMatrix != null ? ImmutableList.copyOf(differenationMatrix) : null);
    }

    @Override
    public DifferentiationMatrix.Meta metaBean() {
        return DifferentiationMatrix.Meta.INSTANCE;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the user identifier.
     * @return the value of the property, not null
     */
    public DifferentiationMatrixId getDifferenationMatrixId() {
        return differenationMatrixId;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the number of logins.
     * @return the value of the property
     */
    public List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>> getDifferenationMatrix() {
        return differenationMatrix;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a builder that allows this bean to be mutated.
     * @return the mutable builder, not null
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && obj.getClass() == this.getClass()) {
            DifferentiationMatrix other = (DifferentiationMatrix) obj;
            return JodaBeanUtils.equal(differenationMatrixId, other.differenationMatrixId) &&
                    JodaBeanUtils.equal(differenationMatrix, other.differenationMatrix);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = getClass().hashCode();
        hash = hash * 31 + JodaBeanUtils.hashCode(differenationMatrixId);
        hash = hash * 31 + JodaBeanUtils.hashCode(differenationMatrix);
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(96);
        buf.append("DifferentiationMatrix{");
        buf.append("differenationMatrixId").append('=').append(differenationMatrixId).append(',').append(' ');
        buf.append("differenationMatrix").append('=').append(JodaBeanUtils.toString(differenationMatrix));
        buf.append('}');
        return buf.toString();
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-bean for {@code DifferentiationMatrix}.
     */
    public static final class Meta extends DirectMetaBean {
        /**
         * The singleton instance of the meta-bean.
         */
        static final Meta INSTANCE = new Meta();

        /**
         * The meta-property for the {@code differenationMatrixId} property.
         */
        private final MetaProperty<DifferentiationMatrixId> differenationMatrixId = DirectMetaProperty.ofImmutable(
                this, "differenationMatrixId", DifferentiationMatrix.class, DifferentiationMatrixId.class);
        /**
         * The meta-property for the {@code differenationMatrix} property.
         */
        @SuppressWarnings({"unchecked", "rawtypes" })
        private final MetaProperty<List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>>> differenationMatrix = DirectMetaProperty.ofImmutable(
                this, "differenationMatrix", DifferentiationMatrix.class, (Class) List.class);
        /**
         * The meta-properties.
         */
        private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
                this, null,
                "differenationMatrixId",
                "differenationMatrix");

        /**
         * Restricted constructor.
         */
        private Meta() {
        }

        @Override
        protected MetaProperty<?> metaPropertyGet(String propertyName) {
            switch (propertyName.hashCode()) {
                case -1651679466:  // differenationMatrixId
                    return differenationMatrixId;
                case -1525739365:  // differenationMatrix
                    return differenationMatrix;
            }
            return super.metaPropertyGet(propertyName);
        }

        @Override
        public DifferentiationMatrix.Builder builder() {
            return new DifferentiationMatrix.Builder();
        }

        @Override
        public Class<? extends DifferentiationMatrix> beanType() {
            return DifferentiationMatrix.class;
        }

        @Override
        public Map<String, MetaProperty<?>> metaPropertyMap() {
            return metaPropertyMap$;
        }

        //-----------------------------------------------------------------------
        /**
         * The meta-property for the {@code differenationMatrixId} property.
         * @return the meta-property, not null
         */
        public MetaProperty<DifferentiationMatrixId> differenationMatrixId() {
            return differenationMatrixId;
        }

        /**
         * The meta-property for the {@code differenationMatrix} property.
         * @return the meta-property, not null
         */
        public MetaProperty<List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>>> differenationMatrix() {
            return differenationMatrix;
        }

        //-----------------------------------------------------------------------
        @Override
        protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
            switch (propertyName.hashCode()) {
                case -1651679466:  // differenationMatrixId
                    return ((DifferentiationMatrix) bean).getDifferenationMatrixId();
                case -1525739365:  // differenationMatrix
                    return ((DifferentiationMatrix) bean).getDifferenationMatrix();
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
     * The bean-builder for {@code DifferentiationMatrix}.
     */
    public static final class Builder extends DirectFieldsBeanBuilder<DifferentiationMatrix> {

        private DifferentiationMatrixId differenationMatrixId;
        private List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>> differenationMatrix;

        /**
         * Restricted constructor.
         */
        private Builder() {
        }

        /**
         * Restricted copy constructor.
         * @param beanToCopy  the bean to copy from, not null
         */
        private Builder(DifferentiationMatrix beanToCopy) {
            this.differenationMatrixId = beanToCopy.getDifferenationMatrixId();
            this.differenationMatrix = (beanToCopy.getDifferenationMatrix() != null ? ImmutableList.copyOf(beanToCopy.getDifferenationMatrix()) : null);
        }

        //-----------------------------------------------------------------------
        @Override
        public Object get(String propertyName) {
            switch (propertyName.hashCode()) {
                case -1651679466:  // differenationMatrixId
                    return differenationMatrixId;
                case -1525739365:  // differenationMatrix
                    return differenationMatrix;
                default:
                    throw new NoSuchElementException("Unknown property: " + propertyName);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Builder set(String propertyName, Object newValue) {
            switch (propertyName.hashCode()) {
                case -1651679466:  // differenationMatrixId
                    this.differenationMatrixId = (DifferentiationMatrixId) newValue;
                    break;
                case -1525739365:  // differenationMatrix
                    this.differenationMatrix = (List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>>) newValue;
                    break;
                default:
                    throw new NoSuchElementException("Unknown property: " + propertyName);
            }
            return this;
        }

        @Override
        public Builder set(MetaProperty<?> property, Object value) {
            super.set(property, value);
            return this;
        }

        @Override
        public DifferentiationMatrix build() {
            return new DifferentiationMatrix(
                    differenationMatrixId,
                    differenationMatrix);
        }

        //-----------------------------------------------------------------------
        /**
         * Sets the user identifier.
         * @param differenationMatrixId  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder differenationMatrixId(DifferentiationMatrixId differenationMatrixId) {
            JodaBeanUtils.notNull(differenationMatrixId, "differenationMatrixId");
            this.differenationMatrixId = differenationMatrixId;
            return this;
        }

        /**
         * Sets the number of logins.
         * @param differenationMatrix  the new value
         * @return this, for chaining, not null
         */
        public Builder differenationMatrix(List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>> differenationMatrix) {
            this.differenationMatrix = differenationMatrix;
            return this;
        }

        /**
         * Sets the {@code differenationMatrix} property in the builder
         * from an array of objects.
         * @param differenationMatrix  the new value
         * @return this, for chaining, not null
         */
        @SafeVarargs
        public final Builder differenationMatrix(BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>... differenationMatrix) {
            return differenationMatrix(ImmutableList.copyOf(differenationMatrix));
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(96);
            buf.append("DifferentiationMatrix.Builder{");
            buf.append("differenationMatrixId").append('=').append(JodaBeanUtils.toString(differenationMatrixId)).append(',').append(' ');
            buf.append("differenationMatrix").append('=').append(JodaBeanUtils.toString(differenationMatrix));
            buf.append('}');
            return buf.toString();
        }

    }

    //-------------------------- AUTOGENERATED END --------------------------
}