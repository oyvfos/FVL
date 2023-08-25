package mavenBlue;

/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */


import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.loader.csv.PositionCsvInfoResolver;
import com.opengamma.strata.loader.csv.SensitivityCsvInfoResolver;
import com.opengamma.strata.loader.csv.SensitivityCsvInfoSupplier;
import com.opengamma.strata.loader.csv.TradeCsvInfoResolver;
import com.opengamma.strata.loader.csv.TradeCsvInfoSupplier;

/**
 * Standard CSV information resolver.
 */
final class StandardCsvInfoImplPolicy
    implements
    TradeCsvInfoResolver,
    TradeCsvInfoSupplier,
    PositionCsvInfoResolver,
    SensitivityCsvInfoResolver,
    SensitivityCsvInfoSupplier {

  /**
   * Standard instance.
   */
  static final StandardCsvInfoImplPolicy INSTANCE = new StandardCsvInfoImplPolicy(ReferenceData.standard());

  /**
   * The reference data.
   */
  private final ReferenceData refData;

  /**
   * Obtains an instance that uses the specified set of reference data.
   * 
   * @param refData  the reference data
   * @return the loader
   */
  public static StandardCsvInfoImplPolicy of(ReferenceData refData) {
    return new StandardCsvInfoImplPolicy(refData);
  }

  // restricted constructor
  public StandardCsvInfoImplPolicy(ReferenceData refData) {
    this.refData = ArgChecker.notNull(refData, "refData");
  }

  //-------------------------------------------------------------------------
  @Override
  public ReferenceData getReferenceData() {
    return refData;
  }

}

