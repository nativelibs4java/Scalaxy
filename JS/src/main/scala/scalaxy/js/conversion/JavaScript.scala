package scalaxy.js

import scala.reflect.macros.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation

class JavaScript extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro JavaScript.implementation
}

object JavaScript {
  def implementation(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val trees = annottees.map(_.tree).toList

    // val isGlobal = c.prefix.tree match {
    //   case q"new JavaScript(global = true)" | q"new JavaScript(true)" => true
    //   case _ => false
    // }

    // val inputs = trees
    // val inputs = List(c.enclosingUnit.body)
    val inputs = List(c.typeCheck(c.enclosingUnit.body, withMacrosDisabled = true))

    def collectGlobals(tree: Tree): List[Tree] = tree match {
      case ModuleDef(mods, name, Template(parents, self, body)) 
          if global.hasAnnotation(c.universe)(tree.symbol) =>
        body
      case PackageDef(pid, stats) =>
        List(PackageDef(pid, stats.flatMap(stat => collectGlobals(stat))))
      case input =>
        List(input)
    }
    val conv: List[Tree] = 
      inputs.flatMap(collectGlobals(_))
      .map(tree => ApiMappings.replaceScalaApisByCallsToExterns(c.universe)(tree))

    val convJs = conv.map(JsPrettyPrinter.prettyPrintJs(c)(_)).mkString("\n")
    println("CONVERTED TO JavaScript:\n" + convJs)

    import java.io._
    val javascriptFile = new File(
      "target/javascript/" +
        new File(c.enclosingPosition.source.path.toString).getName.replaceAll("(.*?)\\.scala", "$1") + ".js").getAbsoluteFile
    javascriptFile.getParentFile.mkdirs()
    val out = new PrintWriter(javascriptFile)
    out.println(convJs)
    out.close()
    println("Wrote " + javascriptFile)

    c.Expr[Any](Block(trees, c.literalUnit.tree))
  }
}
