package mavenBlue;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.io.CsvRow;

import com.opengamma.strata.loader.csv.TradeCsvInfoResolver;

import product.PolicyTrade;

/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

public interface TradeCsvInfoResolverPolicy extends TradeCsvInfoResolver {

	 public default PolicyTrade completeTrade(CsvRow row, PolicyTrade trade) {
		    // do nothing
		    return completeTradeCommon(row, trade);
		  }

	public static TradeCsvInfoResolverPolicy standard() {
		// TODO Auto-generated method stub
		return null;
	}

	public static TradeCsvInfoResolverPolicy of(ReferenceData refData) {
		// TODO Auto-generated method stub
		return StandardCsvInfoImpl.of(refData);
	}

 
}
