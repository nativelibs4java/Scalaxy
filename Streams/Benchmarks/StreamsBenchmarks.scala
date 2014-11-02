package scalaxy.streams.benchmark.jmh

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

import scalaxy.streams.optimize

/**
 * http://java-performance.info/introduction-jmh-profilers/
 * -prof cl,comp
 * -prof HS_GC
 */
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class ArrayBenchmarks
{
  @Benchmark
  def intArrayForeach(state: ArrayState) = {
    var tot = 0L; { for (v <- state.intArray) tot += v };
    tot
  }
  @Benchmark
  def intArrayForeach_optimized(state: ArrayState) = {
    var tot = 0L; optimize { for (v <- state.intArray) tot += v };
    tot
  }

  @Benchmark
  def intArrayZippedWithIndexForeach(state: ArrayState) = {
    var tot = 0L; { for ((v, i) <- state.intArray.zipWithIndex) tot += v + i };
    tot
  }
  @Benchmark
  def intArrayZippedWithIndexForeach_optimized(state: ArrayState) = {
    var tot = 0L; optimize { for ((v, i) <- state.intArray.zipWithIndex) tot += v + i };
    tot
  }

  // @Benchmark
  // def tup2ArrayForeach(state: ArrayState) = {
  //   var tot = 0L; { for ((a, b) <- state.tup2Array) tot += a + b };
  //   tot
  // }
  // @Benchmark
  // def tup2ArrayForeach_optimized(state: ArrayState) = {
  //   var tot = 0L; optimize { for ((a, b) <- state.tup2Array) tot += a + b };
  //   tot
  // }
}
