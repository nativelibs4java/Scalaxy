package scalaxy

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime

package object reified
{
  def reify[A, B](f: A => B): ReifiedFunction[A, B] = macro reified.impl.reifyFunction[A, B]
  
  def reify[A](v: A): ReifiedValue[A] = macro reified.impl.reifyValue[A]
}

