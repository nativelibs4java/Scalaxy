package scalaxy.fastcaseclasses.benchmark.jmh

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

case class Scalar(x: Int)
case class Scalar_NoFastCaseClasses(x: Int)

case class ScalarVal(x: Int) extends AnyVal
case class ScalarVal_NoFastCaseClasses(x: Int) extends AnyVal

case class Point(x: Int, y: Int, z: Int)
case class Vertex(a: Point, b: Point)

case class Point_NoFastCaseClasses(x: Int, y: Int, z: Int)
case class Vertex_NoFastCaseClasses(a: Point_NoFastCaseClasses, b: Point_NoFastCaseClasses)


@State(Scope.Thread)
class SimpleState {
  val scalar = Scalar(1)
  val scalarVal = ScalarVal(1)
  val point = Point(1, 2, 3)
  val vertex = Vertex(Point(1, 2, 3), Point(1, 2, 3))

  val scalar_NoFastCaseClasses = Scalar_NoFastCaseClasses(1)
  val scalarVal_NoFastCaseClasses = ScalarVal_NoFastCaseClasses(1)
  val point_NoFastCaseClasses = Point_NoFastCaseClasses(1, 2, 3)
  val vertex_NoFastCaseClasses = Vertex_NoFastCaseClasses(Point_NoFastCaseClasses(1, 2, 3), Point_NoFastCaseClasses(1, 2, 3))
  
}

// @State(Scope.Thread)
trait BaseState {
  //@Param(Array("10", "15", "20", "25"))
  //@Param(Array("1000", "10000", "100000", "1000000"))
  @Param(Array("1000000"))
  var size: Int = 0
}

@State(Scope.Thread)
class ScalarArray extends BaseState {
  var scalarArray: Array[Scalar] = _

  @Setup
  def init {
    scalarArray = Array.tabulate(size)(i => Scalar(i))
  }
}

@State(Scope.Thread)
class ScalarArray_NoFastCaseClasses extends BaseState {
  var scalarArray: Array[Scalar_NoFastCaseClasses] = _

  @Setup
  def init {
    scalarArray = Array.tabulate(size)(i => Scalar_NoFastCaseClasses(i))
  }
}

@State(Scope.Thread)
class ScalarValArray extends BaseState {
  var scalarValArray: Array[ScalarVal] = _

  @Setup
  def init {
    scalarValArray = Array.tabulate(size)(i => ScalarVal(i))
  }
}

@State(Scope.Thread)
class ScalarValArray_NoFastCaseClasses extends BaseState {
  var scalarValArray: Array[ScalarVal_NoFastCaseClasses] = _

  @Setup
  def init {
    scalarValArray = Array.tabulate(size)(i => ScalarVal_NoFastCaseClasses(i))
  }
}

@State(Scope.Thread)
class PointArray extends BaseState {
  var pointsArray: Array[Point] = _
  var vertexArray: Array[Vertex] = _

  @Setup
  def init {
    pointsArray = Array.tabulate(size)(i => Point(i, i + 1, i + 2))
    vertexArray = Array.tabulate(size)(i =>
      Vertex(pointsArray(i), pointsArray((i + 1) % size)))
  }
}

@State(Scope.Thread)
class PointArray_NoFastCaseClasses extends BaseState {
  var pointsArray: Array[Point_NoFastCaseClasses] = _
  var vertexArray: Array[Vertex_NoFastCaseClasses] = _

  @Setup
  def init {
    pointsArray = Array.tabulate(size)(i => Point_NoFastCaseClasses(i, i + 1, i + 2))
    vertexArray = Array.tabulate(size)(i =>
      Vertex_NoFastCaseClasses(pointsArray(i), pointsArray((i + 1) % size)))
  }
}
