package scalaxy.fastcaseclasses.benchmark.jmh

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

/**
 * http://java-performance.info/introduction-jmh-profilers/
 * -prof cl,comp
 * -prof HS_GC
 */
@BenchmarkMode(Array(Mode.AverageTime))
// @BenchmarkMode(Array(Mode.All))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class CaseClassesBenchmarks
{
  @Benchmark
  def sumScalar(state: ScalarArray) = {
    var tot = 0L
    val n = state.scalarArray.length
    for (Scalar(x) <- state.scalarArray) {
      tot += x
    }
    tot
  }

  @Benchmark
  def sumScalarNoOpt(state: ScalarArray_NoFastCaseClasses) = {
    var tot = 0L
    val n = state.scalarArray.length
    for (Scalar_NoFastCaseClasses(x) <- state.scalarArray) {
      tot += x
    }
    tot
  }

  @Benchmark
  def sumPoint(state: PointArray) = {
    var tot = 0L
    val n = state.pointsArray.length
    for (Point(x, y, z) <- state.pointsArray) {
      tot += x | y | z
    }
    tot
  }
  @Benchmark
  def sumPointNoOpt(state: PointArray_NoFastCaseClasses) = {
    var tot = 0L
    val n = state.pointsArray.length
    for (Point_NoFastCaseClasses(x, y, z) <- state.pointsArray) {
      tot += x | y | z
    }
    tot
  }

  @Benchmark
  def sumVertex(state: PointArray) = {
    var tot = 0L
    val n = state.vertexArray.length
    for (Vertex(Point(x, y, z), Point(x2, y2, z2)) <- state.vertexArray) {
      tot += x | y | z
      tot += x2 | y2 | z2
    }
    tot
  }
  @Benchmark
  def sumVertexNoOpt(state: PointArray_NoFastCaseClasses) = {
    var tot = 0L
    val n = state.vertexArray.length
    for (Vertex_NoFastCaseClasses(Point_NoFastCaseClasses(x, y, z), Point_NoFastCaseClasses(x2, y2, z2)) <- state.vertexArray) {
      tot += x | y | z
      tot += x2 | y2 | z2
    }
    tot
  }
}
