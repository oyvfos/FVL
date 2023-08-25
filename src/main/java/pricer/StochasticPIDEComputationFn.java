package pricer;

import java.time.LocalDate;

import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;

import product.StochasticPIDEComputation;

public class StochasticPIDEComputationFn
implements PolicyComputationFn<StochasticPIDEComputation> {

/**
* Default implementation.
*/
public static final StochasticPIDEComputationFn DEFAULT = new StochasticPIDEComputationFn();

/**
* Creates an instance.
*/
public StochasticPIDEComputationFn() {
}

//-------------------------------------------------------------------------

@Override
public double rate(StochasticPIDEComputation computation, LocalDate startDate, LocalDate endDate,
		RatesProvider provider) {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public PointSensitivityBuilder rateSensitivity(StochasticPIDEComputation computation, LocalDate startDate,
		LocalDate endDate, RatesProvider provider) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public double explainRate(StochasticPIDEComputation computation, LocalDate startDate, LocalDate endDate,
		RatesProvider provider, ExplainMapBuilder builder) {
	// TODO Auto-generated method stub
	return 0;
}

}
