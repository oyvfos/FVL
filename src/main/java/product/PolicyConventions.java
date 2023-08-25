package product;


import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Standardized credit default swap conventions.
 */
public final class PolicyConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<PolicyConvention> ENUM_LOOKUP = ExtendedEnum.of(PolicyConvention.class);

  /**
   * USD-dominated standardized credit default swap.
   */
  public static final PolicyConvention UNIT_LINKED = PolicyConvention.of(StandardPolicyConventions.UNIT_LINKED.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private PolicyConventions() {
  }

}
