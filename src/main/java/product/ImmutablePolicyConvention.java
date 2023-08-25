package product;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

import javax.script.ScriptException;

import org.ejml.simple.SimpleMatrix;
import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataId;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;

import liabilities.DifferentiationMatrix;
import liabilities.DifferentiationMatrixId;
@BeanDefinition
public final class ImmutablePolicyConvention 
    implements PolicyConvention, ImmutableBean, Serializable {
    
    @PropertyDefinition(validate = "notNull", overrideGet = true)
    private final String name;
    
    //-------------------------------------------------------------------------
    /**
    * Obtains a convention based on the specified name and leg conventions.
    * <p>
    * The two leg conventions must be in the same currency.
    * The spot date offset is set to be the effective date offset of the index.
    * 
    * @param name  the unique name of the convention 
    * @param fixedLeg  the market convention for the fixed leg
    * @param floatingLeg  the market convention for the floating leg
    * @return the convention
    */
    public static void main(String[] args) throws IOException, ParseException, ScriptException, URISyntaxException {
        //test();
        long start = System.currentTimeMillis();
        List<Pair<Integer, Integer>> ranges= List.of(Pair.of(-2, 3),Pair.of(-2, 3),Pair.of(-2, 3));
        //List<Pair<Integer,List<Integer>>> statesDef= List.of(Pair.of(0, List.of(0,1,2)),Pair.of(1, List.of(0)),Pair.of(2, List.of(0)));
        List<Double> stepsizes= List.of(0.01,.05,.25);
        //ReferenceData ss = addRefData(ReferenceData.standard(),  ranges, stepsizes);
        long end = System.currentTimeMillis();
        //testCalibration();
        
    }
    
    
    
    
    @PropertyDefinition(validate = "notNull", overrideGet = false)
    private final List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>> funcs;
    
    
    
    public static ImmutablePolicyConvention of(
      String name,
      List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>> funcs) {
        return new ImmutablePolicyConvention(name, funcs);
    }
    public  ReferenceData addRefdata(
              ReferenceData rd) {
        Map<ReferenceDataId<?>, Object> map = new HashMap<>();
      
        ImmutableMap.Builder<ReferenceDataId<?>, Object> builderRefData = ImmutableMap.builder();
        builderRefData.put(DifferentiationMatrixId.of("OG-Ticker", "funcs"), DifferentiationMatrix.of(DifferentiationMatrixId.of("OG-Ticker", "dS"), this.funcs)).build();
            return rd.combinedWith((ImmutableReferenceData) ImmutableReferenceData.of(builderRefData.build()));
                //return rd.combinedWith(this.funcs);
            }
    
    /**
    * Obtains a convention based on the specified name and leg conventions.
    * <p>
    * The two leg conventions must be in the same currency.
    * 
    * @param name  the unique name of the convention 
    * @param fixedLeg  the market convention for the fixed leg
    * @param floatingLeg  the market convention for the floating leg
    * @param spotDateOffset  the offset of the spot value date from the trade date
    * @return the convention
    */
    
    @Override
    public PolicyTrade toTrade(TradeInfo info, Policy policy, ReferenceData referenceData) {
        
        // override for Javadoc
        // return  PolicyConvention.createTrade(info, policy.toBuilder().calcMethod(StochasticPIDEComputation.of(0)).build(), referenceData);
        return PolicyTrade.of(info, policy.toBuilder().calcMethod(StochasticPIDEComputation.of(0)).build());
      }
    
    //-------------------------------------------------------------------------
    @ImmutableValidator
    private void validate() {
    //ArgChecker.isTrue(fixedLeg.getCurrency().equals(floatingLeg.getCurrency()), "Conventions must have same currency");
    }
    
    //-------------------------------------------------------------------------
    
    //-------------------------------------------------------------------------
    @Override
    public String toString() {
    return getName();
}
    //------------------------- AUTOGENERATED START -------------------------
    /**
     * The meta-bean for {@code ImmutablePolicyConvention}.
     * @return the meta-bean, not null
     */
    public static ImmutablePolicyConvention.Meta meta() {
        return ImmutablePolicyConvention.Meta.INSTANCE;
    }

    static {
        MetaBean.register(ImmutablePolicyConvention.Meta.INSTANCE);
    }

    /**
     * The serialization version id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Returns a builder used to create an instance of the bean.
     * @return the builder, not null
     */
    public static ImmutablePolicyConvention.Builder builder() {
        return new ImmutablePolicyConvention.Builder();
    }

    private ImmutablePolicyConvention(
            String name,
            List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>> funcs) {
        JodaBeanUtils.notNull(name, "name");
        JodaBeanUtils.notNull(funcs, "funcs");
        this.name = name;
        this.funcs = ImmutableList.copyOf(funcs);
        validate();
    }

    @Override
    public ImmutablePolicyConvention.Meta metaBean() {
        return ImmutablePolicyConvention.Meta.INSTANCE;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the name.
     * @return the value of the property, not null
     */
    @Override
    public String getName() {
        return name;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the funcs.
     * @return the value of the property, not null
     */
    public List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>> getFuncs() {
        return funcs;
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
            ImmutablePolicyConvention other = (ImmutablePolicyConvention) obj;
            return JodaBeanUtils.equal(name, other.name) &&
                    JodaBeanUtils.equal(funcs, other.funcs);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = getClass().hashCode();
        hash = hash * 31 + JodaBeanUtils.hashCode(name);
        hash = hash * 31 + JodaBeanUtils.hashCode(funcs);
        return hash;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-bean for {@code ImmutablePolicyConvention}.
     */
    public static final class Meta extends DirectMetaBean {
        /**
         * The singleton instance of the meta-bean.
         */
        static final Meta INSTANCE = new Meta();

        /**
         * The meta-property for the {@code name} property.
         */
        private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
                this, "name", ImmutablePolicyConvention.class, String.class);
        /**
         * The meta-property for the {@code funcs} property.
         */
        @SuppressWarnings({"unchecked", "rawtypes" })
        private final MetaProperty<List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>>> funcs = DirectMetaProperty.ofImmutable(
                this, "funcs", ImmutablePolicyConvention.class, (Class) List.class);
        /**
         * The meta-properties.
         */
        private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
                this, null,
                "name",
                "funcs");

        /**
         * Restricted constructor.
         */
        private Meta() {
        }

        @Override
        protected MetaProperty<?> metaPropertyGet(String propertyName) {
            switch (propertyName.hashCode()) {
                case 3373707:  // name
                    return name;
                case 97793583:  // funcs
                    return funcs;
            }
            return super.metaPropertyGet(propertyName);
        }

        @Override
        public ImmutablePolicyConvention.Builder builder() {
            return new ImmutablePolicyConvention.Builder();
        }

        @Override
        public Class<? extends ImmutablePolicyConvention> beanType() {
            return ImmutablePolicyConvention.class;
        }

        @Override
        public Map<String, MetaProperty<?>> metaPropertyMap() {
            return metaPropertyMap$;
        }

        //-----------------------------------------------------------------------
        /**
         * The meta-property for the {@code name} property.
         * @return the meta-property, not null
         */
        public MetaProperty<String> name() {
            return name;
        }

        /**
         * The meta-property for the {@code funcs} property.
         * @return the meta-property, not null
         */
        public MetaProperty<List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>>> funcs() {
            return funcs;
        }

        //-----------------------------------------------------------------------
        @Override
        protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
            switch (propertyName.hashCode()) {
                case 3373707:  // name
                    return ((ImmutablePolicyConvention) bean).getName();
                case 97793583:  // funcs
                    return ((ImmutablePolicyConvention) bean).getFuncs();
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
     * The bean-builder for {@code ImmutablePolicyConvention}.
     */
    public static final class Builder extends DirectFieldsBeanBuilder<ImmutablePolicyConvention> {

        private String name;
        private List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>> funcs = ImmutableList.of();

        /**
         * Restricted constructor.
         */
        private Builder() {
        }

        /**
         * Restricted copy constructor.
         * @param beanToCopy  the bean to copy from, not null
         */
        private Builder(ImmutablePolicyConvention beanToCopy) {
            this.name = beanToCopy.getName();
            this.funcs = ImmutableList.copyOf(beanToCopy.getFuncs());
        }

        //-----------------------------------------------------------------------
        @Override
        public Object get(String propertyName) {
            switch (propertyName.hashCode()) {
                case 3373707:  // name
                    return name;
                case 97793583:  // funcs
                    return funcs;
                default:
                    throw new NoSuchElementException("Unknown property: " + propertyName);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Builder set(String propertyName, Object newValue) {
            switch (propertyName.hashCode()) {
                case 3373707:  // name
                    this.name = (String) newValue;
                    break;
                case 97793583:  // funcs
                    this.funcs = (List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>>) newValue;
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
        public ImmutablePolicyConvention build() {
            return new ImmutablePolicyConvention(
                    name,
                    funcs);
        }

        //-----------------------------------------------------------------------
        /**
         * Sets the name.
         * @param name  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder name(String name) {
            JodaBeanUtils.notNull(name, "name");
            this.name = name;
            return this;
        }

        /**
         * Sets the funcs.
         * @param funcs  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder funcs(List<BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>> funcs) {
            JodaBeanUtils.notNull(funcs, "funcs");
            this.funcs = funcs;
            return this;
        }

        /**
         * Sets the {@code funcs} property in the builder
         * from an array of objects.
         * @param funcs  the new value, not null
         * @return this, for chaining, not null
         */
        @SafeVarargs
        public final Builder funcs(BiFunction<Pair<ResolvedPolicy,RatesProvider>, Double, SimpleMatrix>... funcs) {
            return funcs(ImmutableList.copyOf(funcs));
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(96);
            buf.append("ImmutablePolicyConvention.Builder{");
            buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
            buf.append("funcs").append('=').append(JodaBeanUtils.toString(funcs));
            buf.append('}');
            return buf.toString();
        }

    }

    //-------------------------- AUTOGENERATED END --------------------------
}
