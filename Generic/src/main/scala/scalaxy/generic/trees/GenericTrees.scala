package scalaxy.generic
package trees

import scala.language.implicitConversions

import scala.reflect.NameTransformer
import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror

import scalaxy.debug._

object GenericTrees {

  private lazy val genericPackageSymbol = currentMirror.staticModule("scalaxy.generic.package")

  private object GenericPackage {
    def unapply(tree: Tree): Boolean = tree.symbol == genericPackageSymbol
  }
  private object GenericOpsCreation {
    def unapply(tree: Tree): Option[(Tree, Type)] = {
      if (tree != null && tree.tpe != null && tree.tpe <:< typeOf[GenericOps[_]]) {
        // println("GenericOpsCreation: " + tree)
        Option(tree) collect {
          //case q"scalaxy.generic.mkGenericOps[$tpe]($value)" =>
          case Apply(
            Apply(
              TypeApply(
                Select(GenericPackage(), name),
                List(tpt)),
              List(value)),
            implicits) =>
            value -> tpt.tpe
        }
      } else {
        // println("NOT A GenericOpsCreation: " + tree.tpe)
        None
      }
    }
  }
  private object GenericOpsCall {
    def unapply(tree: Tree): Option[(Tree, Type, String, String)] = Option(tree) collect {

      case Apply(Select(GenericOpsCreation(target, tpe), callName), List(Literal(Constant(methodName: String)))) =>
        (target, tpe, callName.toString, methodName)
    }
  }
  private object GenericNumberCall {
    def unapply(tree: Tree): Option[(Int, Type)] = Option(tree) collect {

      case Apply(TypeApply(Select(GenericPackage(), Name(n @ ("one" | "zero"))), List(tpt)), List(ev)) =>
        (if (n == "one") 1 else 0, tpt.tpe)

      case Apply(Apply(TypeApply(Select(GenericPackage(), Name("number")), List(tpt)), List(Literal(Constant(n: Int)))), List(ev)) =>
        (n, tpt.tpe)
    }
  }

  private def normalizeName(name: String): TermName =
    TermName(NameTransformer.encode(NameTransformer.decode(name)))

  def simplifier: PartialFunction[(Tree, Tree => Tree), Tree] = {
    case (AsInstanceOf(target, tpe @ ConcreteType()), transformer) if target.tpe =:= tpe =>
      transformer(target)

    case (GenericNumberCall(n, tpe), transformer) if typesToNumerics.contains(tpe) =>
      Literal(Constant(typesToNumerics(tpe).fromInt(n)))

    case (Apply(GenericOpsCall(target, ConcreteType(), "applyDynamic", methodName), args), transformer) =>
      Apply(Select(transformer(target), normalizeName(methodName)), args.map(transformer))

    case (Apply(GenericOpsCall(target, ConcreteType(), "updateDynamic", methodName), List(value)), transformer) =>
      Apply(Select(transformer(target), TermName("update")), List(transformer(value)))

    case (GenericOpsCall(target, ConcreteType(), "selectDynamic", methodName), transformer) =>
      Select(transformer(target), normalizeName(methodName))
  }
}
