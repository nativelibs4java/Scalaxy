package scalaxy.reified

import scalaxy.reified.internal.Utils._

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.reflect.runtime.universe.definitions._
import scala.reflect.runtime.currentMirror
import scala.collection.immutable
import scala.reflect.{ ClassTag, Manifest, ClassManifestFactory }

trait Lifter {
  def lift(value: Any, valueType: Type, force: Boolean): Option[Tree]
}

object Lifter {
  /**
   * Default lifter that handles constants, arrays, immutable collections,
   * tuples and options.
   */
  final lazy val DEFAULT: Lifter = new DefaultLifter()
}
