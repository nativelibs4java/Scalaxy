package scalaxy.js

import scala.reflect.macros.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation

class JavaScriptExterns(paths: String*) extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro JavaScriptExterns.implementation
}

object JavaScriptExterns {
  def implementation(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val q"new JavaScriptExterns(..$pathTrees)" = c.prefix.tree
    val paths = pathTrees.map {
      case Literal(Constant(path: String)) => path
    }
    println("PATHS:\n\t" + paths.mkString(",\n\t"))

    val trees = annottees.map(_.tree).toList match {
      case List(q"object $name extends scala.AnyRef { ..$existingDecls }") => //if existingDecls.isEmpty =>
        // val paths = pathTrees.map {
        //   case Literal(Constant(path: String)) => path
        // }
        // println("PATHS:\n\t" + paths.mkString(",\n\t"))

        val decls: List[Tree] = existingDecls ++ Nil
        List(q"object $name { ..$decls }")
      case _ =>
        c.error(c.enclosingPosition, "This annotation can only be set on an object, found on: " + annottees.map(_.tree))
        Nil
    }
    c.Expr[Any](Block(trees, c.literalUnit.tree))
  }
}
