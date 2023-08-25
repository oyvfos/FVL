package liabilities;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioPerturbation;

public class AbsoluteDoubleShift implements ScenarioPerturbation<Double> {

    private final double[] shiftAmount;

    public AbsoluteDoubleShift(double... shiftAmount) {
      this.shiftAmount = shiftAmount;
    }

    @Override
    public MarketDataBox<Double> applyTo(MarketDataBox<Double> marketData, ReferenceData refData) {
      return marketData.mapWithIndex(getScenarioCount(), (value, scenarioIndex) -> value + shiftAmount[scenarioIndex]);
    }

    @Override
    public int getScenarioCount() {
      return shiftAmount.length;
    }

    @Override
    public Class<Double> getMarketDataType() {
      return Double.class;
    }
  }

