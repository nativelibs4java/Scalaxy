package scalaxy

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime
import scala.reflect.runtime.universe

package object reified {
  def reify[A](v: A): ReifiedValue[A] = macro reified.impl.reifyValue[A]

  implicit def reifiedValue2ReifiedFunction[A, B](r: ReifiedValue[A => B]): ReifiedFunction1[A, B] = {
    new ReifiedFunction1(r)
  }

  implicit def reifiedFunction2ReifiedValue[A, B](r: ReifiedFunction1[A, B]): ReifiedValue[A => B] = {
    r.value
  }

  implicit def reifiedValue2Value[A](r: ReifiedValue[A]): A = r.value
}

