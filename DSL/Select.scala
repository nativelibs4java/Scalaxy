package scalaxy.dsl

import scala.reflect.runtime.universe.{ Symbol, Function }

import scala.collection.GenTraversableOnce
import scala.collection.generic.CanBuildFrom
class Select[A](val from: A) extends ReifiedFilterMonadic[A, Select[A]] {
  def reifiedForeach[U](
      f: ReifiedFunction[A, U],
      filters: List[ReifiedFunction[A, Boolean]]): Unit = {
    println(s"f = $f, filters = $filters")
  }
    
  def reifiedFlatMap[B, That](
      f: ReifiedFunction[A, GenTraversableOnce[B]],
      filters: List[ReifiedFunction[A, Boolean]])(
      implicit bf: CanBuildFrom[Select[A], B, That]): That = {
    println(s"f = $f, filters = $filters")
    val b = bf()
    b.result()
  }
}

object Select {
  def apply[A](from: A) = new Select(from)
}
