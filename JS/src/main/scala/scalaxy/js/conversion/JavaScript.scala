package scalaxy.js

import scala.reflect.macros.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation

import java.io.File

class JavaScript extends StaticAnnotation {
  // def macroTransform(annottees: Any*) = macro JavaScript.implementation
}

object JavaScript {
  def implementation(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val body = c.typeCheck(c.enclosingUnit.body, withMacrosDisabled = true)
    val converter = new ScalaToJavaScriptConverter(c.universe)
    val javaScriptCode = converter.convert(body.asInstanceOf[converter.global.Tree])
    println("CONVERTED TO JavaScript:\n" + javaScriptCode)
    write(
      javaScriptCode,
      new File(
        "target/javascript/" +
        new File(c.enclosingPosition.source.path.toString)
          .getName.replaceAll("(.*?)\\.scala", "$1") + ".js"))


    c.Expr[Any](Block(annottees.map(_.tree).toList, c.literalUnit.tree))
  }
}
