package scalaxy.union

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.language.dynamics

import scala.reflect._
import scala.reflect.macros.Context

import scala.annotation.implicitNotFound

/**
 * (A <|< B) means that either A <:< B, or if B is an union, there is one member C of B for which A <:< C.
 */
@implicitNotFound(msg = "Cannot prove that ${A} <|< ${B}.")
trait <|<[A, B]

object <|< {
  implicit def <|<[A, B]: A <|< B = macro internal.<|<[A, B, A <|< B]
  // implicit def <|<[A, B]: A <|< B = macro prove[A, A <|< B]
  // implicit def derived_<|<[A, B, T <: (A <|< B)]: T = macro internal.<|<[A, B, T]
  // implicit def prove_<|<[T <: (_ <|< _)]: T = macro internal.prove_<|<[T]
  // implicit def prove_<|<[A, B <: (A <|< _)]: B = macro prove[A, B]
}
