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
  def sumPoint(state: ArrayState) = {
    var tot = 0L
    val n = state.pointsArray.length
    for (Point(x, y, z) <- state.pointsArray) {
      tot += x + y + z
    }
    tot
  }

  @Benchmark
  def sumVertex(state: ArrayState) = {
    var tot = 0L
    val n = state.vertexArray.length
    for (Vertex(Point(x, y, z), Point(x2, y2, z2)) <- state.vertexArray) {
      tot += x + y + z
      tot += x2 + y2 + z2
    }
    tot
  }

  @Benchmark
  def sumPointNoOpt(state: ArrayState) = {
    var tot = 0L
    val n = state.pointsArrayNoOpt.length
    for (Point_NoFastCaseClasses(x, y, z) <- state.pointsArrayNoOpt) {
      tot += x + y + z
    }
    tot
  }

  @Benchmark
  def sumVertexNoOpt(state: ArrayState) = {
    var tot = 0L
    val n = state.vertexArrayNoOpt.length
    for (Vertex_NoFastCaseClasses(Point_NoFastCaseClasses(x, y, z), Point_NoFastCaseClasses(x2, y2, z2)) <- state.vertexArrayNoOpt) {
      tot += x + y + z
      tot += x2 + y2 + z2
    }
    tot
  }

  // @Benchmark
  // def addIntsDirect(state:IntState): Int = {
  //   val data = state.values
  //   var total = 0
  //   var i = 0
  //   val len = data.length
  //   while (i < len) { total += data(i); i += 1 }
  //   total
  // }
}
