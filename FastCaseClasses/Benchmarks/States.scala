package scalaxy.fastcaseclasses.benchmark.jmh

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

case class Point(x: Int, y: Int, z: Int)
case class Vertex(a: Point, b: Point)

case class Point_NoFastCaseClasses(x: Int, y: Int, z: Int)
case class Vertex_NoFastCaseClasses(a: Point_NoFastCaseClasses, b: Point_NoFastCaseClasses)

@State(Scope.Thread)
class ArrayState {
  //@Param(Array("10", "15", "20", "25"))
  //@Param(Array("1000", "10000", "100000", "1000000"))
  @Param(Array("1000000"))
  var size: Int = 0

  var pointsArray: Array[Point] = _
  var pointsArrayNoOpt: Array[Point_NoFastCaseClasses] = _

  var vertexArray: Array[Vertex] = _
  var vertexArrayNoOpt: Array[Vertex_NoFastCaseClasses] = _

  @Setup
  def init {
    pointsArray = Array.tabulate(size)(i => Point(i, i + 1, i + 2))
    pointsArrayNoOpt = Array.tabulate(size)(i => Point_NoFastCaseClasses(i, i + 1, i + 2))

    vertexArray = Array.tabulate(size)(i =>
      Vertex(pointsArray(i), pointsArray((i + 1) % size)))
    vertexArrayNoOpt = Array.tabulate(size)(i =>
      Vertex_NoFastCaseClasses(pointsArrayNoOpt(i), pointsArrayNoOpt((i + 1) % size)))
  }
}
