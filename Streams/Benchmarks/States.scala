package scalaxy.streams.benchmark.jmh

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

trait BaseState {
  @Param(Array("1000", "1000000"))
  var size: Int = 0
}

@State(Scope.Thread)
class ArrayState extends BaseState {
  var intArray: Array[Int] = _
  var tup2Array: Array[(Int, Int)] = _
  var tup3Array: Array[(Int, Int, Int)] = _

  @Setup
  def init {
    intArray = Array.tabulate(size)(i => i)
    tup2Array = Array.tabulate(size)(i => (i, i * 10))
    tup3Array = Array.tabulate(size)(i => (i, i * 10, i * 100))
  }
}

@State(Scope.Thread)
class ListState extends BaseState {
  var intList: List[Int] = _
  var tup2List: List[(Int, Int)] = _
  var tup3List: List[(Int, Int, Int)] = _

  @Setup
  def init {
    intList = Array.tabulate(size)(i => i).toList
    tup2List = Array.tabulate(size)(i => (i, i * 10)).toList
    tup3List = Array.tabulate(size)(i => (i, i * 10, i * 100)).toList
  }
}

@State(Scope.Thread)
class RangeState extends BaseState
