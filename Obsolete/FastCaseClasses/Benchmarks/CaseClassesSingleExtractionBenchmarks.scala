package scalaxy.fastcaseclasses.benchmark.jmh

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

/**
 * http://java-performance.info/jmh/
 * http://java-performance.info/introduction-jmh-profilers/
 * -prof cl,comp
 * -prof HS_GC
 */
@BenchmarkMode(Array(Mode.AverageTime, Mode.SingleShotTime))
// @BenchmarkMode(Array(Mode.All))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class CaseClassesSingleExtractionBenchmarks
{
  @Benchmark
  @Warmup(iterations = 5, batchSize = 5000)
  @Measurement(iterations = 5, batchSize = 5000)
  def singleScalar(state: SimpleState) = {
    import state._
    val Scalar(x) = scalar
    x
  }

  @Benchmark
  @Warmup(iterations = 5, batchSize = 5000)
  @Measurement(iterations = 5, batchSize = 5000)
  def singleScalarNoOpt(state: SimpleState) = {
    import state._
    val Scalar_NoFastCaseClasses(x) = scalar_NoFastCaseClasses
    x
  }

  @Benchmark
  @Warmup(iterations = 5, batchSize = 5000)
  @Measurement(iterations = 5, batchSize = 5000)
  def singleValScalar(state: SimpleState) = {
    import state._
    val ScalarVal(x) = scalarVal
    x
  }

  @Benchmark
  @Warmup(iterations = 5, batchSize = 5000)
  @Measurement(iterations = 5, batchSize = 5000)
  def singleValScalarNoOpt(state: SimpleState) = {
    import state._
    val ScalarVal_NoFastCaseClasses(x) = scalarVal_NoFastCaseClasses
    x
  }

  @Benchmark
  @Warmup(iterations = 5, batchSize = 5000)
  @Measurement(iterations = 5, batchSize = 5000)
  def singlePoint(state: SimpleState) = {
    import state._
    val Point(x, y, z) = point
    x | y | z
  }
  @Benchmark
  @Warmup(iterations = 5, batchSize = 5000)
  @Measurement(iterations = 5, batchSize = 5000)
  def singlePointNoOpt(state: SimpleState) = {
    import state._
    val Point_NoFastCaseClasses(x, y, z) = point_NoFastCaseClasses
    x | y | z
  }

  @Benchmark
  @Warmup(iterations = 5, batchSize = 5000)
  @Measurement(iterations = 5, batchSize = 5000)
  def singleVertex(state: SimpleState) = {
    import state._
    val Vertex(Point(x, y, z), Point(x2, y2, z2)) = vertex
    x | y | z | x2 | y2 | z2
  }
  @Benchmark
  @Warmup(iterations = 5, batchSize = 5000)
  @Measurement(iterations = 5, batchSize = 5000)
  def singleVertexNoOpt(state: SimpleState) = {
    import state._
    val Vertex_NoFastCaseClasses(Point_NoFastCaseClasses(x, y, z), Point_NoFastCaseClasses(x2, y2, z2)) = vertex_NoFastCaseClasses
    x | y | z | x2 | y2 | z2
  }

  @Benchmark
  @Warmup(iterations = 5, batchSize = 5000)
  @Measurement(iterations = 5, batchSize = 5000)
  def singleMatchVertex(state: SimpleState) = {
    import state._
    vertex match {
      case Vertex(Point(_, _, _), null) =>
        0
      case Vertex(null, Point(_, _, _)) =>
        0
      case Vertex(Point(_, _, _), Point(_, _, _)) =>
        1
      case _ =>
        0
    }
  }
  @Benchmark
  @Warmup(iterations = 5, batchSize = 5000)
  @Measurement(iterations = 5, batchSize = 5000)
  def singleMatchVertexNoOpt(state: SimpleState) = {
    import state._
    vertex_NoFastCaseClasses match {
      case Vertex_NoFastCaseClasses(Point_NoFastCaseClasses(_, _, _), null) =>
        0
      case Vertex_NoFastCaseClasses(null, Point_NoFastCaseClasses(_, _, _)) =>
        0
      case Vertex_NoFastCaseClasses(Point_NoFastCaseClasses(_, _, _), Point_NoFastCaseClasses(_, _, _)) =>
        1
      case _ =>
        0
    }
  }
}
