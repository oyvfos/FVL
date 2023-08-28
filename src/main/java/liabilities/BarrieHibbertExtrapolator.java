/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package liabilities;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.interpolator.BoundCurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.BoundCurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;

/**
 * Extrapolator implementation that returns a value linearly from the gradient at the first or last node.
 */
final class BarrieHibbertExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The extrapolator name.
   */
  public static final String NAME = "BH";
  /**
   * The extrapolator instance.
   */
  public static final CurveExtrapolator INSTANCE = new BarrieHibbertExtrapolator();
  /**
   * The epsilon value.
   */
  private static final double EPS = 1e-8;

  /**
   * Restricted constructor.
   */
  private BarrieHibbertExtrapolator() {
  }

  // resolve instance
  private Object readResolve() {
    return INSTANCE;
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public BoundCurveExtrapolator bind(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
    return new Bound(xValues, yValues, interpolator);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return NAME;
  }

  //-------------------------------------------------------------------------
  /**
   * Bound extrapolator. DIscount factors !
   */
  static class Bound implements BoundCurveExtrapolator {
	private final BoundCurveInterpolator interpolator;
    private final int nodeCount;
    private final double firstXValue;
    private final double firstYValue;
    private final double lastXValue;
    private final double lastYValue;
    private final double eps;
    private final double ufr;
    private final double b1;
    private final double b2;
    private final double b3;
    private final double lambda;
    private final double leftGradient;
    private final DoubleArray leftSens;
    private final DoubleArray xVals;
    private final double rightGradient;
    private final DoubleArray rightSens;

    Bound(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
      
    this.nodeCount = xValues.size();
    this.xVals= xValues;
      this.interpolator=interpolator;
      this.firstXValue = xValues.get(0);
      this.firstYValue = yValues.get(0);
      this.lastXValue = xValues.get(nodeCount - 1);
      this.lastYValue = yValues.get(nodeCount - 1);
      this.eps = EPS * (lastXValue - firstXValue);
      this.ufr= 0.042;
      this.b1 = Math.log(1+ufr);
      this.lambda = 0.06;
      this.b2= Math.log(interpolator.interpolate(lastXValue - eps)- lastYValue) / eps - this.b1;
      this.b3=this.b2-(2*Math.log(interpolator.interpolate(lastXValue - 2*eps)/interpolator.interpolate(lastXValue - eps))-this.b1)*Math.exp(-this.lambda);
      
      
      
      // left
      this.leftGradient = (interpolator.interpolate(firstXValue + eps) - firstYValue) / eps;
      this.leftSens = interpolator.parameterSensitivity(firstXValue + eps);
      // right
      this.rightGradient = (lastYValue - interpolator.interpolate(lastXValue - eps)) / eps;
      this.rightSens = interpolator.parameterSensitivity(lastXValue - eps);
    }

    //-------------------------------------------------------------------------
   // @Override
//    public double leftExtrapolate(double xValue) {
//      return firstYValue + (xValue - firstXValue) * leftGradient;
//    }
//
//    @Override
//    public double leftExtrapolateFirstDerivative(double xValue) {
//      return leftGradient;
//    }
//
//    @Override
//    public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
//      double[] result = leftSens.toArray();
//      int n = result.length;
//      for (int i = 1; i < n; i++) {
//        result[i] = result[i] * (xValue - firstXValue) / eps;
//        
//      }
//      result[0] = 1 + (result[0] - 1) * (xValue - firstXValue) / eps;
//      return DoubleArray.ofUnsafe(result);
//    }

    //adapt this for BarrieHibbert------------------------------------------------------------------------- 
    @Override
    public double rightExtrapolate(double xValue) {
    	double fwdrate = this.b1+(this.b2+this.b3* (xValue- lastXValue))*Math.exp(-this.lambda*(xValue- lastXValue));
    	
        return lastYValue + (xValue - lastXValue) * rightGradient;
    }

    @Override
    public double rightExtrapolateFirstDerivative(double xValue) {
      return rightGradient;
    }

    @Override
    public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
      double[] result = rightSens.toArray();
      int n = result.length;
      for (int i = 0; i < n - 1; i++) {
        result[i] = -result[i] * (xValue - lastXValue) / eps;
        System.out.println((xValue - lastXValue) / eps);
        System.out.println(rightGradient/interpolator.firstDerivative(xVals.get(i)));
        
      }
      result[n - 1] = 1 + (1 - result[n - 1]) * (xValue - lastXValue) / eps;
      return DoubleArray.ofUnsafe(result);
    }

	@Override
	public double leftExtrapolate(double xValue) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double leftExtrapolateFirstDerivative(double xValue) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
		// TODO Auto-generated method stub
		return null;
	}
  }

}
