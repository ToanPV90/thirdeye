package com.linkedin.thirdeye.dataframe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math.stat.correlation.Covariance;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;


/**
 * Series container for primitive double.
 */
public final class DoubleSeries extends TypedSeries<DoubleSeries> {
  public static final double NULL = Double.NaN;
  public static final double INFINITY = Double.POSITIVE_INFINITY;
  public static final double POSITIVE_INFINITY = Double.POSITIVE_INFINITY;
  public static final double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;
  public static final double DEFAULT = 0.0d;
  public static final double MIN_VALUE = Double.MIN_VALUE;
  public static final double MAX_VALUE = Double.MAX_VALUE;

  public static final DoubleFunction SUM = new DoubleSum();
  public static final DoubleFunction PRODUCT = new DoubleProduct();
  public static final DoubleFunction FIRST = new DoubleFirst();
  public static final DoubleFunction LAST = new DoubleLast();
  public static final DoubleFunction MIN = new DoubleMin();
  public static final DoubleFunction MAX = new DoubleMax();
  public static final DoubleFunction MEAN = new DoubleMean();
  public static final DoubleFunction STD = new DoubleStandardDeviation();
  public static final DoubleFunction NEGATIVE = new DoubleNegative();

  public static final class DoubleSum implements DoubleFunction {
    @Override
    public double apply(double[] values) {
      if(values.length <= 0)
        return NULL;
      // TODO sort, add low to high for accuracy?
      double result = 0.0d;
      for(double v : values)
        result += v;
      return result;
    }
  }

  public static final class DoubleProduct implements DoubleFunction {
    @Override
    public double apply(double[] values) {
      if(values.length <= 0)
        return NULL;
      // TODO sort for accuracy?
      double result = 1.0d;
      for(double v : values)
        result *= v;
      return result;
    }
  }

  public static final class DoubleMean implements DoubleFunction {
    @Override
    public double apply(double[] values) {
      if(values.length <= 0)
        return NULL;

      // TODO sort, add low to high for accuracy?
      double sum = 0.0d;
      int count = 0;
      for(double v : values) {
        sum += v;
        count++;
      }
      return sum / count;
    }
  }

  public static final class DoubleFirst implements DoubleFunction {
    @Override
    public double apply(double[] values) {
      if(values.length <= 0)
        return NULL;
      return values[0];
    }
  }

  public static final class DoubleLast implements DoubleFunction {
    @Override
    public double apply(double[] values) {
      if(values.length <= 0)
        return NULL;
      return values[values.length - 1];
    }
  }

  public static final class DoubleMin implements DoubleFunction {
    @Override
    public double apply(double[] values) {
      if(values.length <= 0)
        return NULL;
      double min = values[0];
      for(double v : values)
        min = Math.min(min, v);
      return min;
    }
  }

  public static final class DoubleMax implements DoubleFunction {
    @Override
    public double apply(double[] values) {
      if (values.length <= 0)
        return NULL;
      double max = values[0];
      for (double v : values)
        max = Math.max(max, v);
      return max;
    }
  }

  public static final class DoubleNegative implements DoubleFunction {
    @Override
    public double apply(double... values) {
      if(values.length <= 0)
        return NULL;
      return -values[0];
    }
  }

  public static final class DoubleStandardDeviation implements DoubleFunction {
    @Override
    public double apply(double... values) {
      if(values.length <= 1)
        return NULL;
      double mean = MEAN.apply(values);
      double var = 0.0;
      for(double v : values)
        var += (v - mean) * (v - mean);
      return Math.sqrt(var);
    }
  }

  public static final class DoubleMapZScore implements DoubleFunction {
    final double mean;
    final double std;

    public DoubleMapZScore(double mean, double std) {
      if(std <= 0.0d)
        throw new IllegalArgumentException("std must be greater than 0");
      this.mean = mean;
      this.std = std;
    }

    @Override
    public double apply(double... values) {
      return (values[0] - this.mean) / this.std;
    }
  }

  public static final class DoubleMapNormalize implements DoubleFunction {
    final double min;
    final double max;

    public DoubleMapNormalize(double min, double max) {
      if(min == max)
        throw new IllegalArgumentException("min and max must be different");
      this.min = min;
      this.max = max;
    }

    @Override
    public double apply(double... values) {
      return (values[0] - this.min) / (this.max - this.min);
    }
  }

  public static class Builder extends Series.Builder {
    final List<double[]> arrays = new ArrayList<>();

    private Builder() {
      // left blank
    }

    public Builder addValues(double... values) {
      this.arrays.add(values);
      return this;
    }

    public Builder addValues(double value) {
      return this.addValues(new double[] { value });
    }

    public Builder addValues(Collection<Double> values) {
      double[] newValues = new double[values.size()];
      int i = 0;
      for(Double v : values)
        newValues[i++] = valueOf(v);
      return this.addValues(newValues);
    }

    public Builder addValues(Double... values) {
      return this.addValues(Arrays.asList(values));
    }

    public Builder addValues(Double value) {
      return this.addValues(new double[] { valueOf(value) });
    }

    public Builder fillValues(int count, double value) {
      double[] values = new double[count];
      Arrays.fill(values, value);
      return this.addValues(values);
    }

    public Builder fillValues(int count, Double value) {
      return this.fillValues(count, valueOf(value));
    }

    @Override
    public Builder addSeries(Collection<Series> series) {
      for(Series s : series)
        this.addValues(s.getDoubles().values);
      return this;
    }

    @Override
    public DoubleSeries build() {
      int totalSize = 0;
      for(double[] array : this.arrays)
        totalSize += array.length;

      int offset = 0;
      double[] values = new double[totalSize];
      for(double[] array : this.arrays) {
        System.arraycopy(array, 0, values, offset, array.length);
        offset += array.length;
      }

      return new DoubleSeries(values);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static DoubleSeries buildFrom(double... values) {
    return new DoubleSeries(values);
  }

  public static DoubleSeries empty() {
    return new DoubleSeries();
  }

  public static DoubleSeries nulls(int size) {
    return builder().fillValues(size, NULL).build();
  }

  public static DoubleSeries zeros(int size) {
    return builder().fillValues(size, 0.0d).build();
  }

  public static DoubleSeries ones(int size) {
    return builder().fillValues(size, 1.0d).build();
  }

  public static DoubleSeries fillValues(int size, double value) { return builder().fillValues(size, value).build(); }

  // CAUTION: The array is final, but values are inherently modifiable
  final double[] values;

  private DoubleSeries(double... values) {
    this.values = values;
  }

  @Override
  public Builder getBuilder() {
    return new Builder();
  }

  @Override
  public DoubleSeries getDoubles() {
    return this;
  }

  @Override
  public double getDouble(int index) {
    return getDouble(this.values[index]);
  }

  public static double getDouble(double value) {
    return value;
  }

  @Override
  public long getLong(int index) {
    return getLong(this.values[index]);
  }

  public static long getLong(double value) {
    if(DoubleSeries.isNull(value))
      return LongSeries.NULL;
    if(value == NEGATIVE_INFINITY)
      return LongSeries.MIN_VALUE;
    return (long) value;
  }

  @Override
  public byte getBoolean(int index) {
    return getBoolean(this.values[index]);
  }

  public static byte getBoolean(double value) {
    if(DoubleSeries.isNull(value))
      return BooleanSeries.NULL;
    return BooleanSeries.valueOf(value != 0.0d);
  }

  @Override
  public String getString(int index) {
    return getString(this.values[index]);
  }

  public static String getString(double value) {
    if(DoubleSeries.isNull(value))
      return StringSeries.NULL;
    return String.valueOf(value);
  }

  @Override
  public boolean isNull(int index) {
    return isNull(this.values[index]);
  }

  @Override
  public int size() {
    return this.values.length;
  }

  @Override
  public SeriesType type() {
    return SeriesType.DOUBLE;
  }

  public double[] values() {
    return this.values;
  }

  public double value() {
    if(this.size() != 1)
      throw new IllegalStateException("Series must contain exactly one element");
    return this.values[0];
  }

  /**
   * Returns the contents of the series wrapped as list.
   *
   * @return list of series elements
   */
  public List<Double> toList() {
    Double[] values = new Double[this.values.length];
    for(int i=0; i<this.values.length; i++) {
      if(!this.isNull(i))
        values[i] = this.values[i];
    }
    return Arrays.asList(values);
  }

  /**
   * Returns the value of the first element in the series
   *
   * @throws IllegalStateException if the series is empty
   * @return first element in the series
   */
  public double first() {
    assertNotEmpty(this.values);
    return this.values[0];
  }

  /**
   * Returns the value of the last element in the series
   *
   * @throws IllegalStateException if the series is empty
   * @return last element in the series
   */
  public double last() {
    assertNotEmpty(this.values);
    return this.values[this.values.length-1];
  }

  @Override
  public DoubleSeries slice(int from, int to) {
    return buildFrom(Arrays.copyOfRange(this.values, from, to));
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("DoubleSeries{");
    for(double d : this.values) {
      if(isNull(d)) {
        builder.append("null");
      } else {
        builder.append(d);
      }
      builder.append(" ");
    }
    builder.append("}");
    return builder.toString();
  }

  @Override
  public String toString(int index) {
    if(this.isNull(index))
      return TOSTRING_NULL;
    return String.valueOf(this.values[index]);
  }

  public double min() {
    return this.aggregate(MIN).value();
  }

  public double max() {
    return this.aggregate(MAX).value();
  }

  public double sum() {
    return this.aggregate(SUM).value();
  }

  public double product() {
    return this.aggregate(PRODUCT).value();
  }

  public double mean() {
    return this.aggregate(MEAN).value();
  }

  public double std() {
    return this.aggregate(STD).value();
  }

  public double corr(Series other) {
    return corr(this, other);
  }

  public double cov(Series other) {
    return cov(this, other);
  }

  public DoubleSeries normalize() {
    try {
      return this.map(new DoubleMapNormalize(this.min(), this.max()));
    } catch (Exception e) {
      return DoubleSeries.builder().fillValues(this.size(), NULL).build();
    }
  }

  public DoubleSeries zscore() {
    try {
      return this.map(new DoubleMapZScore(this.mean(), this.std()));
    } catch (Exception e) {
      return DoubleSeries.builder().fillValues(this.size(), NULL).build();
    }
  }

  public DoubleSeries add(Series other) {
    return map(new DoubleFunction() {
      @Override
      public double apply(double... values) {
        return values[0] + values[1];
      }
    }, this, other);
  }

  public DoubleSeries add(final double constant) {
    if(isNull(constant))
      return nulls(this.size());
    return this.map(new DoubleFunction() {
      @Override
      public double apply(double... values) {
        return values[0] + constant;
      }
    });
  }

  public DoubleSeries subtract(Series other) {
    return map(new DoubleFunction() {
      @Override
      public double apply(double... values) {
        return values[0] - values[1];
      }
    }, this, other);
  }

  public DoubleSeries subtract(final double constant) {
    if(isNull(constant))
      return nulls(this.size());
    return this.map(new DoubleFunction() {
      @Override
      public double apply(double... values) {
        return values[0] - constant;
      }
    });
  }

  public DoubleSeries multiply(Series other) {
    return map(new DoubleFunction() {
      @Override
      public double apply(double... values) {
        return values[0] * values[1];
      }
    }, this, other);
  }

  public DoubleSeries multiply(final double constant) {
    if(isNull(constant))
      return nulls(this.size());
    return this.map(new DoubleFunction() {
      @Override
      public double apply(double... values) {
        return values[0] * constant;
      }
    });
  }

  public DoubleSeries divide(Series other) {
    DoubleSeries o = other.getDoubles();
    if(o.contains(0.0d))
      throw new ArithmeticException("/ by zero");
    return map(new DoubleFunction() {
      @Override
      public double apply(double... values) {
        return values[0] / values[1];
      }
    }, this, o);
  }

  public DoubleSeries divide(final double constant) {
    if(isNull(constant))
      return nulls(this.size());
    if(constant == 0.0d)
      throw new ArithmeticException("/ by zero");
    return this.map(new DoubleFunction() {
      @Override
      public double apply(double... values) {
        return values[0] / constant;
      }
    });
  }

  public DoubleSeries pow(Series other) {
    return map(new DoubleFunction() {
      @Override
      public double apply(double... values) {
        return Math.pow(values[0], values[1]);
      }
    }, this, other);
  }

  public DoubleSeries pow(final double constant) {
    return this.map(new DoubleFunction() {
      @Override
      public double apply(double... values) {
        return Math.pow(values[0], constant);
      }
    });
  }

  public BooleanSeries eq(Series other) {
    return map(new DoubleConditional() {
      @Override
      public boolean apply(double... values) {
        return values[0] == values[1];
      }
    }, this, other);
  }

  public BooleanSeries eq(final double constant) {
    if(isNull(constant))
      return BooleanSeries.nulls(this.size());
    return this.map(new DoubleConditional() {
      @Override
      public boolean apply(double... values) {
        return values[0] == constant;
      }
    });
  }

  public BooleanSeries eq(final double constant, final double epsilon) {
    return this.eq(fillValues(this.size(), constant), epsilon);
  }

  public BooleanSeries eq(Series other, final double epsilon) {
    return map(new DoubleConditional() {
      @Override
      public boolean apply(double... values) {
        return values[0] - epsilon <= values[1] && values[0] + epsilon >= values[1];
      }
    }, this, other);
  }

  public DoubleSeries set(BooleanSeries where, double value) {
    double[] values = new double[this.values.length];
    for(int i=0; i<where.size(); i++) {
      if(BooleanSeries.isTrue(where.getBoolean(i))) {
        values[i] = value;
      } else {
        values[i] = this.values[i];
      }
    }
    return buildFrom(values);
  }

  public int count(double value) {
    int count = 0;
    for(double v : this.values)
      if(nullSafeDoubleComparator(v, value) == 0)
        count++;
    return count;
  }

  public boolean contains(double value) {
    return this.count(value) > 0;
  }

  public DoubleSeries replace(double find, double by) {
    if(isNull(find))
      return this.fillNull(by);
    return this.set(this.eq(find), by);
  }

  @Override
  public DoubleSeries filter(BooleanSeries filter) {
    return this.set(filter.fillNull().not(), NULL);
  }

  @Override
  public DoubleSeries fillNull() {
    return this.fillNull(DEFAULT);
  }

  /**
   * Return a copy of the series with all <b>null</b> values replaced by
   * {@code value}.
   *
   * @param value replacement value for <b>null</b>
   * @return series copy without nulls
   */
  public DoubleSeries fillNull(double value) {
    double[] values = Arrays.copyOf(this.values, this.values.length);
    for(int i=0; i<values.length; i++) {
      if(isNull(values[i])) {
        values[i] = value;
      }
    }
    return buildFrom(values);
  }

  public DoubleSeries fillInfinite(double value) {
    double[] values = Arrays.copyOf(this.values, this.values.length);
    for(int i=0; i<values.length; i++) {
      if(Double.isInfinite(values[i])) {
        values[i] = value;
      }
    }
    return buildFrom(values);
  }

  @Override
  DoubleSeries project(int[] fromIndex) {
    double[] values = new double[fromIndex.length];
    for(int i=0; i<fromIndex.length; i++) {
      if(fromIndex[i] == -1) {
        values[i] = NULL;
      } else {
        values[i] = this.values[fromIndex[i]];
      }
    }
    return buildFrom(values);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DoubleSeries that = (DoubleSeries) o;

    return Arrays.equals(this.values, that.values);
  }

  @Override
  int compare(Series that, int indexThis, int indexThat) {
    return nullSafeDoubleComparator(this.values[indexThis], that.getDouble(indexThat));
  }

  /**
   * @see DataFrame#map(Series.Function, Series...)
   */
  public static DoubleSeries map(DoubleFunction function, Series... series) {
    if(series.length <= 0)
      return empty();

    DataFrame.assertSameLength(series);

    // Note: code-specialization to help hot-spot vm
    if(series.length == 1)
      return mapUnrolled(function, series[0]);
    if(series.length == 2)
      return mapUnrolled(function, series[0], series[1]);
    if(series.length == 3)
      return mapUnrolled(function, series[0], series[1], series[2]);

    double[] input = new double[series.length];
    double[] output = new double[series[0].size()];
    for(int i=0; i<series[0].size(); i++) {
      output[i] = mapRow(function, series, input, i);
    }

    return buildFrom(output);
  }

  private static double mapRow(DoubleFunction function, Series[] series, double[] input, int row) {
    for(int j=0; j<series.length; j++) {
      double value = series[j].getDouble(row);
      if(isNull(value))
        return NULL;
      input[j] = value;
    }
    return function.apply(input);
  }

  private static DoubleSeries mapUnrolled(DoubleFunction function, Series a) {
    double[] output = new double[a.size()];
    for(int i=0; i<a.size(); i++) {
      if(a.isNull(i)) {
        output[i] = NULL;
      } else {
        output[i] = function.apply(a.getDouble(i));
      }
    }
    return buildFrom(output);
  }

  private static DoubleSeries mapUnrolled(DoubleFunction function, Series a, Series b) {
    double[] output = new double[a.size()];
    for(int i=0; i<a.size(); i++) {
      if(a.isNull(i) || b.isNull(i)) {
        output[i] = NULL;
      } else {
        output[i] = function.apply(a.getDouble(i), b.getDouble(i));
      }
    }
    return buildFrom(output);
  }

  private static DoubleSeries mapUnrolled(DoubleFunction function, Series a, Series b, Series c) {
    double[] output = new double[a.size()];
    for(int i=0; i<a.size(); i++) {
      if(a.isNull(i) || b.isNull(i) || c.isNull(i)) {
        output[i] = NULL;
      } else {
        output[i] = function.apply(a.getDouble(i), b.getDouble(i), c.getDouble(i));
      }
    }
    return buildFrom(output);
  }

  /**
   * @see DataFrame#map(Series.Function, Series...)
   */
  public static BooleanSeries map(DoubleConditional function, Series... series) {
    if(series.length <= 0)
      return BooleanSeries.empty();

    DataFrame.assertSameLength(series);

    double[] input = new double[series.length];
    byte[] output = new byte[series[0].size()];
    for(int i=0; i<series[0].size(); i++) {
      output[i] = mapRow(function, series, input, i);
    }

    return BooleanSeries.buildFrom(output);
  }

  private static byte mapRow(DoubleConditional function, Series[] series, double[] input, int row) {
    for(int j=0; j<series.length; j++) {
      double value = series[j].getDouble(row);
      if(isNull(value))
        return BooleanSeries.NULL;
      input[j] = value;
    }
    return BooleanSeries.valueOf(function.apply(input));
  }

  /**
   * @see Series#aggregate(Function)
   */
  public static DoubleSeries aggregate(DoubleFunction function, Series series) {
    return buildFrom(function.apply(series.dropNull().getDoubles().values));
  }

  /**
   * @see Series#aggregate(Function)
   */
  public static BooleanSeries aggregate(DoubleConditional function, Series series) {
    return BooleanSeries.builder().addBooleanValues(function.apply(series.dropNull().getDoubles().values)).build();
  }

  public static double corr(Series a, Series b) {
    if(a.hasNull() || b.hasNull())
      return NULL;
    return new PearsonsCorrelation().correlation(a.getDoubles().values(), b.getDoubles().values());
  }

  public static double cov(Series a, Series b) {
    if(a.hasNull() || b.hasNull())
      return NULL;
    return new Covariance().covariance(a.getDoubles().values(), b.getDoubles().values());
  }

  private static int nullSafeDoubleComparator(double a, double b) {
    if(isNull(a) && isNull(b))
      return 0;
    if(isNull(a))
      return -1;
    if(isNull(b))
      return 1;
    return Double.compare(a, b);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(this.values);
  }

  public static double valueOf(Double value) {
    if(value == null)
      return NULL;
    return value;
  }

  public static boolean isNull(double value) {
    return Double.isNaN(value);
  }

  private static double[] assertNotEmpty(double[] values) {
    if(values.length <= 0)
      throw new IllegalStateException("Must contain at least one value");
    return values;
  }

  @Override
  public DoubleSeries shift(int offset) {
    double[] values = new double[this.values.length];
    if(offset >= 0) {
      Arrays.fill(values, 0, Math.min(offset, values.length), NULL);
      System.arraycopy(this.values, 0, values, Math.min(offset, values.length), Math.max(values.length - offset, 0));
    } else {
      System.arraycopy(this.values, Math.min(-offset, values.length), values, 0, Math.max(values.length + offset, 0));
      Arrays.fill(values, Math.max(values.length + offset, 0), values.length, NULL);
    }
    return buildFrom(values);
  }

  @Override
  public DoubleSeries sorted() {
    double[] values = Arrays.copyOf(this.values, this.values.length);
    Arrays.sort(values);

    // order NaNs first
    int count = 0;
    while(count < values.length && isNull(values[values.length - count - 1]))
      count++;

    if(count <= 0 || count >= values.length)
      return buildFrom(values);

    double[] newValues = new double[values.length];
    Arrays.fill(newValues, 0, count, Double.NaN);
    System.arraycopy(values, 0, newValues, count, values.length - count);

    return buildFrom(newValues);
  }

  @Override
  int[] sortedIndex() {
    List<DoubleSortTuple> tuples = new ArrayList<>();
    for (int i = 0; i < this.values.length; i++) {
      tuples.add(new DoubleSortTuple(this.values[i], i));
    }

    Collections.sort(tuples, new Comparator<DoubleSortTuple>() {
      @Override
      public int compare(DoubleSortTuple a, DoubleSortTuple b) {
        return nullSafeDoubleComparator(a.value, b.value);
      }
    });

    int[] fromIndex = new int[tuples.size()];
    for (int i = 0; i < tuples.size(); i++) {
      fromIndex[i] = tuples.get(i).index;
    }
    return fromIndex;
  }

  static final class DoubleSortTuple {
    final double value;
    final int index;

    DoubleSortTuple(double value, int index) {
      this.value = value;
      this.index = index;
    }
  }
}
