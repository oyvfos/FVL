package product;

import org.ejml.simple.SimpleMatrix;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;

/**
 * The accrual calculation part of an interest rate swap leg.
 * <p>
 * An interest rate swap leg is defined by {@link RateCalculationSwapLeg}.
 * The rate to be paid is defined by the implementations of this interface.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface PolicyComputation {

	  /**
	   * Collects all the indices referred to by this computation.
	   * <p>
	   * A computation will typically refer to one index, such as 'GBP-LIBOR-3M'.
	   * Each index that is referred to must be added to the specified builder.
	   * 
	   * @param builder  the builder to use
	   */
	  public abstract void collectIndices(ImmutableSet.Builder<Index> builder);
	  //public  Pair<SimpleMatrix, SimpleMatrix> ODEAdjointSolutions(ResolvedPolicy resolvedPolicy, RatesProvider provider, ReferenceData refData );
	  //public  PointSensitivityBuilder sensBuilder(ResolvedPolicy resolvedPolicy, RatesProvider provider, ReferenceData refData, Pair<SimpleMatrix, SimpleMatrix> diffMat); 

	}
