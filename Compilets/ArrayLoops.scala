package scalaxy; package compilets

import macros._
import matchers._

import scala.collection.mutable.ArrayBuilder

object ArrayLoops
{
  def genericArrayForeach[A, B](array: Array[A], body: A => B) = replace(
    for (v <- array) body(v),
    {
      var i = 0; val a = array; val n = a.length;
      while (i < n) { val v = a(i); body(v); i += 1 }
    }
  )
  def intArrayForeach[B](array: Array[Int], body: Int => B) = replace(
    for (v <- array) body(v),
    {
      var i = 0; val a = array; val n = a.length;
      while (i < n) { val v = a(i); body(v); i += 1 }
    }
  )
  def genericArrayMap[A, B : scala.reflect.ClassTag](array: Array[A], body: A => B) = replace(
    for (v <- array) yield body(v),
    {
      var i = 0; val a = array; val n = a.length; val res = ArrayBuilder.make[B]()
      while (i < n) { val v = a(i); res += body(v); i += 1 }
      res.result()
    }
  )
  def intArrayMap[B : scala.reflect.ClassTag](array: Array[Int], body: Int => B) = replace(
    for (v <- array) yield body(v),
    {
      var i = 0; val a = array; val n = a.length; val res = ArrayBuilder.make[B]()
      while (i < n) { val v = a(i); res += body(v); i += 1 }
      res.result()
    }
  )
}
