package scalaxy.loops

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect.NameTransformer
import scala.reflect.macros.Context

import scala.reflect.runtime.{ universe => ru }

object Loops {
  def optimize[A](a: A): A = macro Macros.optimize[A]
}

/**
  Stream output:
    indexName: Option[TermName],
    sinkSizeName: Option[TermName],
    tupleNames: TupleNames

  Then:
    tuple match + tuple apply + tupleNames => replacedBody + valdefs + newTupleNames

*/
object Macros {
  def optimize[A : c.WeakTypeTag](c: Context)(a: c.Expr[A]): c.Expr[A] = {
    object Optimize extends StreamOps {
      override val global = c.universe
      import global._

      val original = a.tree.asInstanceOf[Tree]//c.typeCheck(a.tree)

      val result = new Transformer {
        override def transform(tree: Tree) = tree match {
          case Stream(stream) =>
            // println(s"source = $source")
            c.info(a.tree.pos, s"stream = $stream", force = true)
            val result = stream.emitStream(n => c.fresh(n): TermName, transform(_))
            println(result)
            tree
            // super.transform(tree)

          case _ =>
            // println("Not matched: " + tree)
            super.transform(tree)
        }
      } transform original

      // println(s"Original: $original")
      // println(s"Result: $result")
    }

    c.Expr[A](Optimize.result.asInstanceOf[c.universe.Tree])
  }
}
