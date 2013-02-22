// Author: Olivier Chafik (http://ochafik.com)
// Feel free to modify and reuse this for any purpose ("public domain / I don't care").
package scalaxy.extensions

import scala.collection.mutable

import scala.reflect.internal._
import scala.reflect.ClassTag

import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.transform.TypingTransformers

/**
 *  This compiler plugin demonstrates how to do "useful" stuff before the typer phase.
 *
 *  It defines a toy syntax that uses annotations to define implicit classes:
 *
 *    @scalaxy.extend(Any) def quoted(quote: String): String = quote + self + quote
 *
 *  Which gets desugared to:
 *
 *    import scala.language.experimental.macros
 *    implicit class scalaxy$extensions$quoted$1(self: Any) {
 *      def quoted(quote: String) = macro scalaxy$extensions$quoted$1.quoted
 *    }
 *    object scalaxy$extensions$quoted$1 {
 *      def quoted(c: scala.reflect.macros.Context)
 *                (quote: c.Expr[String]): c.Expr[String] = {
 *        import c.universe._
 *        val Apply(_, List(selfTree)) = c.prefix.tree
 *        val self = c.Expr[String](selfTree)
 *        reify(quote.splice + self.splice + quote.splice)
 *      }
 *    }
 *
 *  This plugin is only partially hygienic: it assumes @scalaxy.extend is not locally redefined to something else.
 *
 *  To see the AST before and after the rewrite, run the compiler with -Xprint:parser -Xprint:scalaxy-extensions.
 */
object MacroExtensionsCompiler {
  def jarOf(c: Class[_]) =
    Option(c.getProtectionDomain.getCodeSource).map(_.getLocation.getFile)
  val scalaLibraryJar = jarOf(classOf[List[_]])

  def main(args: Array[String]) {
    try {
      val settings = new Settings
      val command =
        new CompilerCommand(
          scalaLibraryJar.map(jar => List("-bootclasspath", jar)).getOrElse(Nil) ++ args, settings)

      if (!command.ok)
        System.exit(1)

      val global = new Global(settings, new ConsoleReporter(settings)) {
        override protected def computeInternalPhases() {
          super.computeInternalPhases
          phasesSet += new MacroExtensionsComponent(this)
        }
      }
      new global.Run().compile(command.files)
    } catch {
      case ex: Throwable =>
        ex.printStackTrace
        System.exit(2)
    }
  }
}
