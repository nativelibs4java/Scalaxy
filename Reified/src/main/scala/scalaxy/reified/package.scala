package scalaxy

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime
import scala.reflect.runtime.universe

import scalaxy.reified.impl.Reification

package object reified
{
  def reify[A, B](f: A => B): A => B = macro reified.impl.reifyFunction[A, B]
  
  def reify[A](v: A): ReifiedValue[A] = macro reified.impl.reifyValue[A]
}

