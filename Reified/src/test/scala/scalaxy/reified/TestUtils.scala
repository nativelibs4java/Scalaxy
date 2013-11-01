package scalaxy.reified
package test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

import scala.collection.mutable.ArrayBuffer

import scalaxy.reified.{ reified, Reified }

trait TestUtils {

  implicit class ReifiedExtensions[A](r: Reified[A]) {
    private[test] def capturedValues: Seq[AnyRef] = {
      val b = ArrayBuffer[AnyRef]()
      import universe._
      new Traverser {
        override def traverse(tree: Tree) {
          if (tree.symbol != null && tree.symbol.isFreeTerm) {
            val value = tree.symbol.asFreeTerm.value
            b += value.asInstanceOf[AnyRef]
            value match {
              case hr: HasReified[_] =>
                traverse(hr.reifiedValue.expr.tree)
              case _ =>
            }
          } else {
            super.traverse(tree)
          }
        }
      } traverse r.expr.tree
      b
    }
  }

  def assertSameEvals[A: TypeTag, B: TypeTag](f: Reified[A => B], inputs: A*) {
    //val toolbox = currentMirror.mkToolBox()
    for (input <- inputs) {
      val directEval = f(input)
      val reifiedF = f.compile()()
      val reifiedEval = reifiedF(input)

      assertEquals(directEval, reifiedEval)
    }
  }
}
