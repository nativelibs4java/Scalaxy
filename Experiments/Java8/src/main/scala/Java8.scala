package scalaxy.java8

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

import java.util.function._

package object java8 {
  implicit def scalaToJavaToIntFunction[A](f: A => Int): ToIntFunction[A] =
    macro impl.javaToFunctionImpl[A, Int, ToIntFunction[Int]]

  implicit def scalaToJavaToLongFunction[A](f: A => Long): ToLongFunction[A] =
    macro impl.javaToFunctionImpl[A, Long, ToLongFunction[Long]]

  implicit def scalaToJavaToDoubleFunction[A](f: A => Double): ToDoubleFunction[A] =
    macro impl.javaToFunctionImpl[A, Double, ToDoubleFunction[Double]]

  implicit def scalaToJavaFunction[A, B <: AnyRef](f: A => B): Function[A, B] =
    macro impl.javaToFunctionImpl[A, B, Function[A, B]]

  implicit def scalaToJavaFunction[A](f: A => A): UnaryOperator[A] =
    macro impl.javaToFunctionImpl[A, Double, UnaryOperator[A]]
}
