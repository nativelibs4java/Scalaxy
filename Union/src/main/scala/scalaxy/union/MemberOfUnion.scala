package scalaxy.union

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.annotation.implicitNotFound

/**
 * (A =|= B) means that either A =:= B, or if B is an union, there is one member C of B for which A =:= C.
 */
@implicitNotFound(msg = "Cannot prove that ${A} =|= ${B}.")
trait =|=[A, B]

object =|= {
  implicit def selfProof[A, B <: A]: A =|= B = null
  implicit def =|=[A, B]: A =|= B = macro scalaxy.union.internal.=|=[A, B, A =|= B]
}
