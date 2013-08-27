package scalaxy.js

import com.google.javascript.jscomp.ScalaxyClosureUtils
import com.google.javascript.jscomp._

import scala.collection.JavaConversions._

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
        val externSources = CommandLineRunner.getDefaultExterns().toList//: List[SourceFile]
        val allExterns = JSSourceFile.fromCode("predefs.js", """
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
          """) :: externSources
        val externs = ScalaxyClosureUtils.scanExterns(allExterns)
        // val externs = ScalaxyClosureUtils.scanExterns(JSSourceFile.fromCode("externs.js", """
        //     /** @constructor */
        //     var MyClass = function() {};
        //     /** @this {MyClass} */
        //     MyClass.prototype.f = function() {};
        //   """) :: Nil)
        val globalVars = ExternsAnalyzer.analyze(externs)
        val generatedDecls = globalVars.classes.flatMap(classVars => {
          TreeGenerator.generateClass(c.universe)(classVars, externs, name)
        })


        // for (tree <- generatedDecls) {
        //   try {
        //     c.typeCheck(tree)
        //   } catch { case ex: Throwable =>
        //     // ex.printStackTrace()
        //     println(tree)
        //     throw ex
        //   }
        // }

        val decls: List[Tree] = existingDecls ++ generatedDecls//.take(350)
        List(q"object $name extends scala.AnyRef { ..$decls }")
      case _ =>
        c.error(c.enclosingPosition, "This annotation can only be set on an object, found on: " + annottees.map(_.tree))
        Nil
    }
    println("OUT =\n" + trees.mkString("\n"))
    c.Expr[Any](Block(trees, c.literalUnit.tree))
  }
}
