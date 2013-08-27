package scalaxy.js

import scala.collection.JavaConversions._
import scala.reflect.api.Universe

object ApiMappings {

  def replaceScalaApisByCallsToExterns(u: Universe)(tree: u.Tree): u.Tree = {
    import u._
    def getConstantName(tree: Tree): TermName = {
      val Literal(Constant(n: String)) = tree
      n: TermName
    }

    val transformer = new Transformer {
      override def transform(tree: Tree) = tree match {
        case q"scala.this.Predef.println($text)" =>
          q"window.console.log(${transform(text)})"

        // DynamicValue replacements:
        case q"$target.asDynamic" =>//if tree.tpe <:< typeOf[DynamicValue] =>
          q"${transform(target)}"
        case q"$ext($target)" if tree.tpe <:< typeOf[DynamicExtension] =>
        // case q"js.`package`.DynamicExtension($target)" =>//if tree.tpe <:< typeOf[DynamicValue] =>
          q"${transform(target)}"
        case q"$target.applyDynamic($nameValue)(..$args)" if target.tpe <:< typeOf[DynamicValue] =>
          val name = getConstantName(nameValue)
          q"${transform(target)}.$name(..$args)"
        case q"$target.selectDynamic($nameValue)" if target.tpe <:< typeOf[DynamicValue] =>
          val name = getConstantName(nameValue)
          q"${transform(target)}.$name"
        case q"$target.updateDynamic($nameValue)($value)" if target.tpe <:< typeOf[DynamicValue] =>
          val name = getConstantName(nameValue)
          q"${transform(target)}.$name = ${transform(value)}"

        case Select(root, name) if global.hasAnnotation(u)(tree.symbol) =>
          Ident(name)

        case tree =>
          super.transform(tree)
      }
    }
    transformer.transform(tree)
  }
}
