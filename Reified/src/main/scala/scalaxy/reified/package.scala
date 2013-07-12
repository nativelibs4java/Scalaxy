package scalaxy

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.TypeTag

import scalaxy.reified.impl
import scalaxy.reified.base.ReifiedValue

package object reified {

  type ReifiedValue[A] = base.ReifiedValue[A]
  type HasReifiedValue[A] = base.HasReifiedValue[A]

  def reify[A](v: A): ReifiedValue[A] = macro impl.reifyValue[A]

  implicit def reifiedValue2ReifiedFunction[A: TypeTag, B: TypeTag](r: ReifiedValue[A => B]): ReifiedFunction1[A, B] = {
    new ReifiedFunction1(r)
  }

  implicit def reifiedFunction2ReifiedValue[A, B](r: ReifiedFunction1[A, B]): ReifiedValue[A => B] = {
    r.value
  }

  implicit def reifiedValue2Value[A](r: ReifiedValue[A]): A = r.value
  implicit def reifiedFunction2Value[A, B](r: ReifiedFunction1[A, B]): A => B = r.value
}

