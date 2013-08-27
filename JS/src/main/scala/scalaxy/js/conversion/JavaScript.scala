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

    val body = c.typeCheck(c.enclosingUnit.body, withMacrosDisabled = true)
    val javaScriptCode = new ScalaToJavaScriptConverter(c.universe).convert(body)
    println("CONVERTED TO JavaScript:\n" + javaScriptCode)

    import java.io._
    val javascriptFile = new File(
      "target/javascript/" +
        new File(c.enclosingPosition.source.path.toString).getName.replaceAll("(.*?)\\.scala", "$1") + ".js").getAbsoluteFile
    javascriptFile.getParentFile.mkdirs()
    val out = new PrintWriter(javascriptFile)
    out.println(javaScriptCode)
    out.close()
    println("Wrote " + javascriptFile)

    c.Expr[Any](Block(annottees.map(_.tree).toList, c.literalUnit.tree))
  }
}
