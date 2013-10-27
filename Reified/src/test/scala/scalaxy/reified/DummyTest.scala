package scalaxy.reified
package test

import org.junit._
import org.junit.Assert._

import scala.reflect.runtime.universe
import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe.TypeTag

import scalaxy.reified._
import scalaxy.reified.internal.Optimizer

class DummyTest extends TestUtils {
  @Test
  def test {
    val x = 10
    val r1 = reified { (y: Int) => x * y }
    val r2 = reified { (y: Int) => x * y + r1(10) }
    println(r1)
    println(r2)

    {
      import universe._

      println("ORIGINAL:")
      println(r2.expr.tree)

      println("FLAT:")
      println(r2.flatExpr.tree)
      // new Traverser {
      //   override def traverse(tree: Tree) {
      //     if (tree.symbol != null) {
      //       println(s"tree $tree: ${tree.symbol}")
      //       if (tree.symbol.isFreeTerm) {
      //         println("\tvalue = " + tree.symbol.asFreeTerm.value + ": " + tree.symbol.typeSignature)
      //       }
      //     }
      //     super.traverse(tree)
      //   }
      // } traverse r2.taggedExpr.tree
    }
  }
}
