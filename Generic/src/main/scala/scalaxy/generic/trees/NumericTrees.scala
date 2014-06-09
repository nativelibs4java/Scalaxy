package scalaxy.generic.trees

import scala.reflect.NameTransformer
import scala.reflect.runtime.universe._

object NumericTrees {

  private object NumericNumberCall {
    lazy val numTpeConstructor = typeOf[Numeric[Any]].typeConstructor
    def isNumeric(tpe: Type): Boolean = tpe.typeConstructor <:< numTpeConstructor
    def unapply(tree: Tree): Option[(Int, Type)] = Option(tree) collect {

      case Select(WithType(numType, _), Name(n @ ("one" | "zero"))) if isNumeric(numType) =>
        (if (n == "one") 1 else 0, tree.tpe)

      case Apply(Select(WithType(numType, _), Name("fromInt")), List(Literal(Constant(n: Int)))) if isNumeric(numType) =>
        (n, tree.tpe)
    }
  }

  private object NumericOpsCreation {
    def unapply(tree: Tree): Option[(Tree, Type)] = Option(tree) collect {
      case Apply(
        Apply(
          TypeApply(
            Select(WithSymbol(implicitsModule), name),
            List(tpt)),
          List(value)),
        implicits) =>
        value -> tpt.tpe
    }
  }

  def simplifier: PartialFunction[(Tree, Tree => Tree), Tree] = {
    case (NumericNumberCall(n, tpe @ ConcreteType()), transformer) => //if typesToNumerics.contains(tpe) =>
      Literal(Constant(typesToNumerics(tpe).fromInt(n).asInstanceOf[AnyRef]))

    case (Apply(Select(NumericOpsCreation(target, tpe @ ConcreteType()), name), args), transformer) =>
      val sel = Select(transformer(target), TermName(NameTransformer.encode(NameTransformer.decode(name.toString))))
      if (args.isEmpty) {
        sel
      } else {
        Apply(sel, args.map(transformer))
      }
  }
}
