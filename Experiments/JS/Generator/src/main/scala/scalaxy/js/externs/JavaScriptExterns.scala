package scalaxy.js

import com.google.javascript.jscomp.ScalaxyClosureUtils
import com.google.javascript.jscomp._

import scala.collection.JavaConversions._

import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation

class JavaScriptExterns(paths: String*) extends StaticAnnotation {
  // def macroTransform(annottees: Any*) = macro JavaScriptExterns.implementation
}

object JavaScriptExterns {
  def implementation(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val q"new JavaScriptExterns(..$pathTrees)" = c.prefix.tree
    val paths = pathTrees.map {
      case Literal(Constant(path: String)) => path
    }
    println("PATHS:\n\t" + paths.mkString(",\n\t"))


    val generator = new JavaScriptToScalaSignaturesGenerator(c.universe)

    val trees = annottees.map(_.tree).toList match {
      case List(q"object $name extends scala.AnyRef { ..$existingDecls }") =>

        // println("PATHS:\n\t" + paths.mkString(",\n\t"))
        val allExterns = SourceFile.fromCode("predefs.js", """
            /** @constructor */ // var DatabaseCallback = function() {};
            /** @constructor */ // var DedicatedWorkerGlobalScope = function() {};
            /** @constructor */ // var EventListener = function() {};
            /** @constructor */ // var EventTarget = function() {};
            /** @constructor */ // var LinkStyle = function() {};
            /** @constructor */ // var Range = function() {};
            /** @constructor */ // var Screen = function() {};
            /** @constructor */ // var SharedWorkerGlobalScope = function() {};
            /** @constructor */ // var Storage = function() {};
            /** @constructor */ // var ViewCSS = function() {};
            /** @constructor */ // var WindowLocalStorage = function() {};
            /** @constructor */ // var WindowSessionStorage = function() {};
            /** @constructor */ // var WorkerGlobalScope = function() {};
            /** @constructor */ // var WorkerLocation = function() {};
            /** @constructor */ // var XMLHttpRequest = function() {};

            /** @constructor */
            var MyClass = function() {};
            /** @this {MyClass} */
            MyClass.prototype.f = function() {};
          """) :: ScalaxyClosureUtils.defaultExterns

        val decls =
          existingDecls ++
          generator.generateSignatures[Tree](allExterns, name.toString)

        List(
          q"""
            object $name extends scala.AnyRef {
              ..$decls
            }
          """)
      case _ =>
        c.error(c.enclosingPosition, "This annotation can only be set on an object, found on: " + annottees.map(_.tree))
        Nil
    }

    write(trees.mkString("\n"), new java.io.File("out.scala"))

    // println("OUT =\n" + trees.mkString("\n"))
    c.Expr[Any](Block(trees, c.literalUnit.tree))
  }
}
