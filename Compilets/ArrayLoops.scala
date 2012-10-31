package scalaxy; package compilets

import scala.reflect.runtime.universe._

import macros._
import matchers._

object ArrayLoops
{
  def simpleArrayForeach[A, B](array: Array[A], body: A => B) = replace(
    for (v <- array)
      body(v),
    {
      var i = 0
      val n = array.length
      while (i < n) {
        val v = array(i)
        body(v)
        i += 1
      }
    }
  )
  
  def simpleArrayMap[A, B : scala.reflect.ClassTag](array: Array[A], body: A => B) = replace(
    for (v <- array) yield body(v),
    {
      var i = 0
      val n = array.length
      val res = scala.collection.mutable.ArrayBuilder.make[B]()
      while (i < n) {
        val v = array(i)
        res += body(v)
        i += 1
      }
      res.result()
    }
  )
}
