package product;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

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

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.swap.ResolvedSwap;

/**
 * A forward rate agreement (FRA).
 * <p>
 * A FRA is a financial instrument that represents the one off exchange of a fixed
 * rate of interest for a floating rate at a future date.
 * <p>
 * For example, a FRA might involve an agreement to exchange the difference between
 * the fixed rate of 1% and the 'GBP-LIBOR-3M' rate in 2 months time.
 * <p>
 * The FRA is defined by four dates.
 * <ul>
 * <li>Start date, the date on which the implied deposit starts
 * <li>End date, the date on which the implied deposit ends
 * <li>Fixing date, the date on which the index is to be observed, typically 2 business days before the start date
 * <li>Payment date, the date on which payment is made, typically the same as the start date
 * </ul>
 * <p>
 * The start date, end date and payment date are determined when the trade if created,
 * adjusting to valid business days based on the holiday calendar dates known on the trade trade.
 * The payment date may be further adjusted when the FRA is resolved if an additional holiday has been added.
 * The data model does allow for the start and end dates to be adjusted when the FRA is resolved,
 * but this is typically not used.
 */
@BeanDefinition
public final class Policy
    implements Product, Resolvable<ResolvedPolicy>, ImmutableBean, Serializable {

  /**
   * Whether the FRA is buy or sell.
   * <p>
   * A value of 'Buy' implies that the floating rate is received from the counterparty,
   * with the fixed rate being paid. A value of 'Sell' implies that the floating rate
   * is paid to the counterparty, with the fixed rate being received.
   */
 
  
  @PropertyDefinition(validate = "notNull")
  private final Boolean male;
  /**
   * The primary currency, defaulted to the currency of the index.
   * <p>
   * This is the currency of the FRA and the currency that payment is made in.
   * The data model permits this currency to differ from that of the index,
   * however the two are typically the same.
   * <p>
   * When building, this will default to the currency of the index if not specified.
   */
  
  /**
   * The notional amount.
   * <p>
   * The notional expressed here must be positive.
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate birthDate;
  
  @PropertyDefinition(validate = "notNull")
  private final LocalDate expiryDate;
  /**
   * The start date, which is the effective date of the FRA.
   * <p>
   * This is the first date that interest accrues.
   * <p>
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   */
  /**
   * The end date, which is the termination date of the FRA.
   * <p>
   * This is the last day that interest accrues.
   * This date must be after the start date.
   * <p>
   * This date is typically set to be a valid business day.
   * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   */
  @PropertyDefinition(validate = "notNull")
  private final double investementAccount;
  
  @PropertyDefinition(validate = "notNull")
  private final double investementAccountProxy;
  /**
   * The business day adjustment to apply to the start and end date, optional.
   * <p>
   * The start and end date are typically defined as valid business days and thus
   * do not need to be adjusted. If this optional property is present, then the
   * start and end date will be adjusted as defined here.
   */
  
  @PropertyDefinition(validate = "notNull")
  private final double expenseRateinvestementAccount;
  
  
  @PropertyDefinition(validate = "notNull")
  private final double rateInvestementAccountGuaranteed;
  
  @PropertyDefinition(validate = "notNull")
  private final double mortalityRestitution;
  
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  
  @PropertyDefinition(validate = "notNull")
  private final String tarifId;
  
  @PropertyDefinition(validate = "notNull")
  private final PolicyComputation calcMethod;
  
  @Override
	public ResolvedPolicy resolve(ReferenceData refData) {
		// TODO Auto-generated method stub
		return ResolvedPolicy.builder()
				 .male(male)
				    .birthDate(birthDate)
				    .expiryDate(expiryDate)
				    
				    .investementAccount(investementAccount)
				    .investementAccountProxy(investementAccountProxy)
				    .expenseRateinvestementAccount(expenseRateinvestementAccount)
				    
				    .rateInvestementAccountGuaranteed(rateInvestementAccountGuaranteed)
				    .mortalityRestitution(mortalityRestitution)
				    .currency(currency)
				    .tarifId(tarifId)
				    .calcMethod(calcMethod)
				    .build();
	}

  /**
   * The payment date.
   * <p>
   * The payment date is typically the same as the start date.
   * The date may be subject to adjustment to ensure it is a business day.
   * <p>
   * When building, this will default to the start date with no adjustments if not specified.
   */
    
    //------------------------- AUTOGENERATED START -------------------------
    /**
     * The meta-bean for {@code Policy}.
     * @return the meta-bean, not null
     */
    public static Policy.Meta meta() {
        return Policy.Meta.INSTANCE;
    }

    static {
        MetaBean.register(Policy.Meta.INSTANCE);
    }

    /**
     * The serialization version id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Returns a builder used to create an instance of the bean.
     * @return the builder, not null
     */
    public static Policy.Builder builder() {
        return new Policy.Builder();
    }

    private Policy(
            Boolean male,
            LocalDate birthDate,
            LocalDate expiryDate,
            double investementAccount,
            double investementAccountProxy,
            double expenseRateinvestementAccount,
            double rateInvestementAccountGuaranteed,
            double mortalityRestitution,
            Currency currency,
            String tarifId,
            PolicyComputation calcMethod) {
        JodaBeanUtils.notNull(male, "male");
        JodaBeanUtils.notNull(birthDate, "birthDate");
        JodaBeanUtils.notNull(expiryDate, "expiryDate");
        JodaBeanUtils.notNull(investementAccount, "investementAccount");
        JodaBeanUtils.notNull(investementAccountProxy, "investementAccountProxy");
        JodaBeanUtils.notNull(expenseRateinvestementAccount, "expenseRateinvestementAccount");
        JodaBeanUtils.notNull(rateInvestementAccountGuaranteed, "rateInvestementAccountGuaranteed");
        JodaBeanUtils.notNull(mortalityRestitution, "mortalityRestitution");
        JodaBeanUtils.notNull(currency, "currency");
        JodaBeanUtils.notNull(tarifId, "tarifId");
        JodaBeanUtils.notNull(calcMethod, "calcMethod");
        this.male = male;
        this.birthDate = birthDate;
        this.expiryDate = expiryDate;
        this.investementAccount = investementAccount;
        this.investementAccountProxy = investementAccountProxy;
        this.expenseRateinvestementAccount = expenseRateinvestementAccount;
        this.rateInvestementAccountGuaranteed = rateInvestementAccountGuaranteed;
        this.mortalityRestitution = mortalityRestitution;
        this.currency = currency;
        this.tarifId = tarifId;
        this.calcMethod = calcMethod;
    }

    @Override
    public Policy.Meta metaBean() {
        return Policy.Meta.INSTANCE;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the male.
     * @return the value of the property, not null
     */
    public Boolean getMale() {
        return male;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the notional amount.
     * <p>
     * The notional expressed here must be positive.
     * The currency of the notional is specified by {@code currency}.
     * @return the value of the property, not null
     */
    public LocalDate getBirthDate() {
        return birthDate;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the expiryDate.
     * @return the value of the property, not null
     */
    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the end date, which is the termination date of the FRA.
     * <p>
     * This is the last day that interest accrues.
     * This date must be after the start date.
     * <p>
     * This date is typically set to be a valid business day.
     * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
     * @return the value of the property, not null
     */
    public double getInvestementAccount() {
        return investementAccount;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the investementAccountProxy.
     * @return the value of the property, not null
     */
    public double getInvestementAccountProxy() {
        return investementAccountProxy;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the expenseRateinvestementAccount.
     * @return the value of the property, not null
     */
    public double getExpenseRateinvestementAccount() {
        return expenseRateinvestementAccount;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the rateInvestementAccountGuaranteed.
     * @return the value of the property, not null
     */
    public double getRateInvestementAccountGuaranteed() {
        return rateInvestementAccountGuaranteed;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the mortalityRestitution.
     * @return the value of the property, not null
     */
    public double getMortalityRestitution() {
        return mortalityRestitution;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the currency.
     * @return the value of the property, not null
     */
    public Currency getCurrency() {
        return currency;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the tarifId.
     * @return the value of the property, not null
     */
    public String getTarifId() {
        return tarifId;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the calcMethod.
     * @return the value of the property, not null
     */
    public PolicyComputation getCalcMethod() {
        return calcMethod;
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
            Policy other = (Policy) obj;
            return JodaBeanUtils.equal(male, other.male) &&
                    JodaBeanUtils.equal(birthDate, other.birthDate) &&
                    JodaBeanUtils.equal(expiryDate, other.expiryDate) &&
                    JodaBeanUtils.equal(investementAccount, other.investementAccount) &&
                    JodaBeanUtils.equal(investementAccountProxy, other.investementAccountProxy) &&
                    JodaBeanUtils.equal(expenseRateinvestementAccount, other.expenseRateinvestementAccount) &&
                    JodaBeanUtils.equal(rateInvestementAccountGuaranteed, other.rateInvestementAccountGuaranteed) &&
                    JodaBeanUtils.equal(mortalityRestitution, other.mortalityRestitution) &&
                    JodaBeanUtils.equal(currency, other.currency) &&
                    JodaBeanUtils.equal(tarifId, other.tarifId) &&
                    JodaBeanUtils.equal(calcMethod, other.calcMethod);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = getClass().hashCode();
        hash = hash * 31 + JodaBeanUtils.hashCode(male);
        hash = hash * 31 + JodaBeanUtils.hashCode(birthDate);
        hash = hash * 31 + JodaBeanUtils.hashCode(expiryDate);
        hash = hash * 31 + JodaBeanUtils.hashCode(investementAccount);
        hash = hash * 31 + JodaBeanUtils.hashCode(investementAccountProxy);
        hash = hash * 31 + JodaBeanUtils.hashCode(expenseRateinvestementAccount);
        hash = hash * 31 + JodaBeanUtils.hashCode(rateInvestementAccountGuaranteed);
        hash = hash * 31 + JodaBeanUtils.hashCode(mortalityRestitution);
        hash = hash * 31 + JodaBeanUtils.hashCode(currency);
        hash = hash * 31 + JodaBeanUtils.hashCode(tarifId);
        hash = hash * 31 + JodaBeanUtils.hashCode(calcMethod);
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(384);
        buf.append("Policy{");
        buf.append("male").append('=').append(male).append(',').append(' ');
        buf.append("birthDate").append('=').append(birthDate).append(',').append(' ');
        buf.append("expiryDate").append('=').append(expiryDate).append(',').append(' ');
        buf.append("investementAccount").append('=').append(investementAccount).append(',').append(' ');
        buf.append("investementAccountProxy").append('=').append(investementAccountProxy).append(',').append(' ');
        buf.append("expenseRateinvestementAccount").append('=').append(expenseRateinvestementAccount).append(',').append(' ');
        buf.append("rateInvestementAccountGuaranteed").append('=').append(rateInvestementAccountGuaranteed).append(',').append(' ');
        buf.append("mortalityRestitution").append('=').append(mortalityRestitution).append(',').append(' ');
        buf.append("currency").append('=').append(currency).append(',').append(' ');
        buf.append("tarifId").append('=').append(tarifId).append(',').append(' ');
        buf.append("calcMethod").append('=').append(JodaBeanUtils.toString(calcMethod));
        buf.append('}');
        return buf.toString();
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-bean for {@code Policy}.
     */
    public static final class Meta extends DirectMetaBean {
        /**
         * The singleton instance of the meta-bean.
         */
        static final Meta INSTANCE = new Meta();

        /**
         * The meta-property for the {@code male} property.
         */
        private final MetaProperty<Boolean> male = DirectMetaProperty.ofImmutable(
                this, "male", Policy.class, Boolean.class);
        /**
         * The meta-property for the {@code birthDate} property.
         */
        private final MetaProperty<LocalDate> birthDate = DirectMetaProperty.ofImmutable(
                this, "birthDate", Policy.class, LocalDate.class);
        /**
         * The meta-property for the {@code expiryDate} property.
         */
        private final MetaProperty<LocalDate> expiryDate = DirectMetaProperty.ofImmutable(
                this, "expiryDate", Policy.class, LocalDate.class);
        /**
         * The meta-property for the {@code investementAccount} property.
         */
        private final MetaProperty<Double> investementAccount = DirectMetaProperty.ofImmutable(
                this, "investementAccount", Policy.class, Double.TYPE);
        /**
         * The meta-property for the {@code investementAccountProxy} property.
         */
        private final MetaProperty<Double> investementAccountProxy = DirectMetaProperty.ofImmutable(
                this, "investementAccountProxy", Policy.class, Double.TYPE);
        /**
         * The meta-property for the {@code expenseRateinvestementAccount} property.
         */
        private final MetaProperty<Double> expenseRateinvestementAccount = DirectMetaProperty.ofImmutable(
                this, "expenseRateinvestementAccount", Policy.class, Double.TYPE);
        /**
         * The meta-property for the {@code rateInvestementAccountGuaranteed} property.
         */
        private final MetaProperty<Double> rateInvestementAccountGuaranteed = DirectMetaProperty.ofImmutable(
                this, "rateInvestementAccountGuaranteed", Policy.class, Double.TYPE);
        /**
         * The meta-property for the {@code mortalityRestitution} property.
         */
        private final MetaProperty<Double> mortalityRestitution = DirectMetaProperty.ofImmutable(
                this, "mortalityRestitution", Policy.class, Double.TYPE);
        /**
         * The meta-property for the {@code currency} property.
         */
        private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
                this, "currency", Policy.class, Currency.class);
        /**
         * The meta-property for the {@code tarifId} property.
         */
        private final MetaProperty<String> tarifId = DirectMetaProperty.ofImmutable(
                this, "tarifId", Policy.class, String.class);
        /**
         * The meta-property for the {@code calcMethod} property.
         */
        private final MetaProperty<PolicyComputation> calcMethod = DirectMetaProperty.ofImmutable(
                this, "calcMethod", Policy.class, PolicyComputation.class);
        /**
         * The meta-properties.
         */
        private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
                this, null,
                "male",
                "birthDate",
                "expiryDate",
                "investementAccount",
                "investementAccountProxy",
                "expenseRateinvestementAccount",
                "rateInvestementAccountGuaranteed",
                "mortalityRestitution",
                "currency",
                "tarifId",
                "calcMethod");

        /**
         * Restricted constructor.
         */
        private Meta() {
        }

        @Override
        protected MetaProperty<?> metaPropertyGet(String propertyName) {
            switch (propertyName.hashCode()) {
                case 3343885:  // male
                    return male;
                case -1210031859:  // birthDate
                    return birthDate;
                case -816738431:  // expiryDate
                    return expiryDate;
                case -208863777:  // investementAccount
                    return investementAccount;
                case -1624330289:  // investementAccountProxy
                    return investementAccountProxy;
                case 765523255:  // expenseRateinvestementAccount
                    return expenseRateinvestementAccount;
                case -653872357:  // rateInvestementAccountGuaranteed
                    return rateInvestementAccountGuaranteed;
                case -605797589:  // mortalityRestitution
                    return mortalityRestitution;
                case 575402001:  // currency
                    return currency;
                case -1538217923:  // tarifId
                    return tarifId;
                case 2005361558:  // calcMethod
                    return calcMethod;
            }
            return super.metaPropertyGet(propertyName);
        }

        @Override
        public Policy.Builder builder() {
            return new Policy.Builder();
        }

        @Override
        public Class<? extends Policy> beanType() {
            return Policy.class;
        }

        @Override
        public Map<String, MetaProperty<?>> metaPropertyMap() {
            return metaPropertyMap$;
        }

        //-----------------------------------------------------------------------
        /**
         * The meta-property for the {@code male} property.
         * @return the meta-property, not null
         */
        public MetaProperty<Boolean> male() {
            return male;
        }

        /**
         * The meta-property for the {@code birthDate} property.
         * @return the meta-property, not null
         */
        public MetaProperty<LocalDate> birthDate() {
            return birthDate;
        }

        /**
         * The meta-property for the {@code expiryDate} property.
         * @return the meta-property, not null
         */
        public MetaProperty<LocalDate> expiryDate() {
            return expiryDate;
        }

        /**
         * The meta-property for the {@code investementAccount} property.
         * @return the meta-property, not null
         */
        public MetaProperty<Double> investementAccount() {
            return investementAccount;
        }

        /**
         * The meta-property for the {@code investementAccountProxy} property.
         * @return the meta-property, not null
         */
        public MetaProperty<Double> investementAccountProxy() {
            return investementAccountProxy;
        }

        /**
         * The meta-property for the {@code expenseRateinvestementAccount} property.
         * @return the meta-property, not null
         */
        public MetaProperty<Double> expenseRateinvestementAccount() {
            return expenseRateinvestementAccount;
        }

        /**
         * The meta-property for the {@code rateInvestementAccountGuaranteed} property.
         * @return the meta-property, not null
         */
        public MetaProperty<Double> rateInvestementAccountGuaranteed() {
            return rateInvestementAccountGuaranteed;
        }

        /**
         * The meta-property for the {@code mortalityRestitution} property.
         * @return the meta-property, not null
         */
        public MetaProperty<Double> mortalityRestitution() {
            return mortalityRestitution;
        }

        /**
         * The meta-property for the {@code currency} property.
         * @return the meta-property, not null
         */
        public MetaProperty<Currency> currency() {
            return currency;
        }

        /**
         * The meta-property for the {@code tarifId} property.
         * @return the meta-property, not null
         */
        public MetaProperty<String> tarifId() {
            return tarifId;
        }

        /**
         * The meta-property for the {@code calcMethod} property.
         * @return the meta-property, not null
         */
        public MetaProperty<PolicyComputation> calcMethod() {
            return calcMethod;
        }

        //-----------------------------------------------------------------------
        @Override
        protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
            switch (propertyName.hashCode()) {
                case 3343885:  // male
                    return ((Policy) bean).getMale();
                case -1210031859:  // birthDate
                    return ((Policy) bean).getBirthDate();
                case -816738431:  // expiryDate
                    return ((Policy) bean).getExpiryDate();
                case -208863777:  // investementAccount
                    return ((Policy) bean).getInvestementAccount();
                case -1624330289:  // investementAccountProxy
                    return ((Policy) bean).getInvestementAccountProxy();
                case 765523255:  // expenseRateinvestementAccount
                    return ((Policy) bean).getExpenseRateinvestementAccount();
                case -653872357:  // rateInvestementAccountGuaranteed
                    return ((Policy) bean).getRateInvestementAccountGuaranteed();
                case -605797589:  // mortalityRestitution
                    return ((Policy) bean).getMortalityRestitution();
                case 575402001:  // currency
                    return ((Policy) bean).getCurrency();
                case -1538217923:  // tarifId
                    return ((Policy) bean).getTarifId();
                case 2005361558:  // calcMethod
                    return ((Policy) bean).getCalcMethod();
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
     * The bean-builder for {@code Policy}.
     */
    public static final class Builder extends DirectFieldsBeanBuilder<Policy> {

        private Boolean male;
        private LocalDate birthDate;
        private LocalDate expiryDate;
        private double investementAccount;
        private double investementAccountProxy;
        private double expenseRateinvestementAccount;
        private double rateInvestementAccountGuaranteed;
        private double mortalityRestitution;
        private Currency currency;
        private String tarifId;
        private PolicyComputation calcMethod;

        /**
         * Restricted constructor.
         */
        private Builder() {
        }

        /**
         * Restricted copy constructor.
         * @param beanToCopy  the bean to copy from, not null
         */
        private Builder(Policy beanToCopy) {
            this.male = beanToCopy.getMale();
            this.birthDate = beanToCopy.getBirthDate();
            this.expiryDate = beanToCopy.getExpiryDate();
            this.investementAccount = beanToCopy.getInvestementAccount();
            this.investementAccountProxy = beanToCopy.getInvestementAccountProxy();
            this.expenseRateinvestementAccount = beanToCopy.getExpenseRateinvestementAccount();
            this.rateInvestementAccountGuaranteed = beanToCopy.getRateInvestementAccountGuaranteed();
            this.mortalityRestitution = beanToCopy.getMortalityRestitution();
            this.currency = beanToCopy.getCurrency();
            this.tarifId = beanToCopy.getTarifId();
            this.calcMethod = beanToCopy.getCalcMethod();
        }

        //-----------------------------------------------------------------------
        @Override
        public Object get(String propertyName) {
            switch (propertyName.hashCode()) {
                case 3343885:  // male
                    return male;
                case -1210031859:  // birthDate
                    return birthDate;
                case -816738431:  // expiryDate
                    return expiryDate;
                case -208863777:  // investementAccount
                    return investementAccount;
                case -1624330289:  // investementAccountProxy
                    return investementAccountProxy;
                case 765523255:  // expenseRateinvestementAccount
                    return expenseRateinvestementAccount;
                case -653872357:  // rateInvestementAccountGuaranteed
                    return rateInvestementAccountGuaranteed;
                case -605797589:  // mortalityRestitution
                    return mortalityRestitution;
                case 575402001:  // currency
                    return currency;
                case -1538217923:  // tarifId
                    return tarifId;
                case 2005361558:  // calcMethod
                    return calcMethod;
                default:
                    throw new NoSuchElementException("Unknown property: " + propertyName);
            }
        }

        @Override
        public Builder set(String propertyName, Object newValue) {
            switch (propertyName.hashCode()) {
                case 3343885:  // male
                    this.male = (Boolean) newValue;
                    break;
                case -1210031859:  // birthDate
                    this.birthDate = (LocalDate) newValue;
                    break;
                case -816738431:  // expiryDate
                    this.expiryDate = (LocalDate) newValue;
                    break;
                case -208863777:  // investementAccount
                    this.investementAccount = (Double) newValue;
                    break;
                case -1624330289:  // investementAccountProxy
                    this.investementAccountProxy = (Double) newValue;
                    break;
                case 765523255:  // expenseRateinvestementAccount
                    this.expenseRateinvestementAccount = (Double) newValue;
                    break;
                case -653872357:  // rateInvestementAccountGuaranteed
                    this.rateInvestementAccountGuaranteed = (Double) newValue;
                    break;
                case -605797589:  // mortalityRestitution
                    this.mortalityRestitution = (Double) newValue;
                    break;
                case 575402001:  // currency
                    this.currency = (Currency) newValue;
                    break;
                case -1538217923:  // tarifId
                    this.tarifId = (String) newValue;
                    break;
                case 2005361558:  // calcMethod
                    this.calcMethod = (PolicyComputation) newValue;
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
        public Policy build() {
            return new Policy(
                    male,
                    birthDate,
                    expiryDate,
                    investementAccount,
                    investementAccountProxy,
                    expenseRateinvestementAccount,
                    rateInvestementAccountGuaranteed,
                    mortalityRestitution,
                    currency,
                    tarifId,
                    calcMethod);
        }

        //-----------------------------------------------------------------------
        /**
         * Sets the male.
         * @param male  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder male(Boolean male) {
            JodaBeanUtils.notNull(male, "male");
            this.male = male;
            return this;
        }

        /**
         * Sets the notional amount.
         * <p>
         * The notional expressed here must be positive.
         * The currency of the notional is specified by {@code currency}.
         * @param birthDate  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder birthDate(LocalDate birthDate) {
            JodaBeanUtils.notNull(birthDate, "birthDate");
            this.birthDate = birthDate;
            return this;
        }

        /**
         * Sets the expiryDate.
         * @param expiryDate  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder expiryDate(LocalDate expiryDate) {
            JodaBeanUtils.notNull(expiryDate, "expiryDate");
            this.expiryDate = expiryDate;
            return this;
        }

        /**
         * Sets the end date, which is the termination date of the FRA.
         * <p>
         * This is the last day that interest accrues.
         * This date must be after the start date.
         * <p>
         * This date is typically set to be a valid business day.
         * Optionally, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
         * @param investementAccount  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder investementAccount(double investementAccount) {
            JodaBeanUtils.notNull(investementAccount, "investementAccount");
            this.investementAccount = investementAccount;
            return this;
        }

        /**
         * Sets the investementAccountProxy.
         * @param investementAccountProxy  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder investementAccountProxy(double investementAccountProxy) {
            JodaBeanUtils.notNull(investementAccountProxy, "investementAccountProxy");
            this.investementAccountProxy = investementAccountProxy;
            return this;
        }

        /**
         * Sets the expenseRateinvestementAccount.
         * @param expenseRateinvestementAccount  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder expenseRateinvestementAccount(double expenseRateinvestementAccount) {
            JodaBeanUtils.notNull(expenseRateinvestementAccount, "expenseRateinvestementAccount");
            this.expenseRateinvestementAccount = expenseRateinvestementAccount;
            return this;
        }

        /**
         * Sets the rateInvestementAccountGuaranteed.
         * @param rateInvestementAccountGuaranteed  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder rateInvestementAccountGuaranteed(double rateInvestementAccountGuaranteed) {
            JodaBeanUtils.notNull(rateInvestementAccountGuaranteed, "rateInvestementAccountGuaranteed");
            this.rateInvestementAccountGuaranteed = rateInvestementAccountGuaranteed;
            return this;
        }

        /**
         * Sets the mortalityRestitution.
         * @param mortalityRestitution  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder mortalityRestitution(double mortalityRestitution) {
            JodaBeanUtils.notNull(mortalityRestitution, "mortalityRestitution");
            this.mortalityRestitution = mortalityRestitution;
            return this;
        }

        /**
         * Sets the currency.
         * @param currency  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder currency(Currency currency) {
            JodaBeanUtils.notNull(currency, "currency");
            this.currency = currency;
            return this;
        }

        /**
         * Sets the tarifId.
         * @param tarifId  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder tarifId(String tarifId) {
            JodaBeanUtils.notNull(tarifId, "tarifId");
            this.tarifId = tarifId;
            return this;
        }

        /**
         * Sets the calcMethod.
         * @param calcMethod  the new value, not null
         * @return this, for chaining, not null
         */
        public Builder calcMethod(PolicyComputation calcMethod) {
            JodaBeanUtils.notNull(calcMethod, "calcMethod");
            this.calcMethod = calcMethod;
            return this;
        }

        //-----------------------------------------------------------------------
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(384);
            buf.append("Policy.Builder{");
            buf.append("male").append('=').append(JodaBeanUtils.toString(male)).append(',').append(' ');
            buf.append("birthDate").append('=').append(JodaBeanUtils.toString(birthDate)).append(',').append(' ');
            buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(expiryDate)).append(',').append(' ');
            buf.append("investementAccount").append('=').append(JodaBeanUtils.toString(investementAccount)).append(',').append(' ');
            buf.append("investementAccountProxy").append('=').append(JodaBeanUtils.toString(investementAccountProxy)).append(',').append(' ');
            buf.append("expenseRateinvestementAccount").append('=').append(JodaBeanUtils.toString(expenseRateinvestementAccount)).append(',').append(' ');
            buf.append("rateInvestementAccountGuaranteed").append('=').append(JodaBeanUtils.toString(rateInvestementAccountGuaranteed)).append(',').append(' ');
            buf.append("mortalityRestitution").append('=').append(JodaBeanUtils.toString(mortalityRestitution)).append(',').append(' ');
            buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
            buf.append("tarifId").append('=').append(JodaBeanUtils.toString(tarifId)).append(',').append(' ');
            buf.append("calcMethod").append('=').append(JodaBeanUtils.toString(calcMethod));
            buf.append('}');
            return buf.toString();
        }

    }

	
	@Override
	public ImmutableSet<Currency> allCurrencies() {
		// TODO Auto-generated method stub
		return null;
	}

    //-------------------------- AUTOGENERATED END --------------------------
}
