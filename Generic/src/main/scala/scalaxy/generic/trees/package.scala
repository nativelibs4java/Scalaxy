package scalaxy.generic

import scala.reflect.runtime.universe._

package trees {
  private[trees] object WithSymbol {
    def unapply(tree: Tree): Option[(Symbol, Tree)] = Some(tree.symbol, tree)
  }
  private[trees] object WithType {
    def unapply(tree: Tree): Option[(Type, Tree)] = Some(tree.tpe, tree)
  }
  private[trees] object ConcreteType {
    def unapply(tpe: Type): Boolean = {
      !tpe.dealias.etaExpand.typeSymbol.asType.isAbstractType
    }
  }
  private[trees] object AsInstanceOf {
    def unapply(tree: Tree): Option[(Tree, Type)] = Option(tree) collect {

      case Apply(TypeApply(Select(target, Name("asInstanceOf")), List(tpt)), Nil) =>
        (target, tpt.tpe)

      case TypeApply(Select(target, name), List(tpt)) if name.toString == "asInstanceOf" =>
        (target, tpt.tpe)
    }
  }
  private[trees] object Name {
    def unapply(n: Name): Option[String] = Some(n.toString)
  }
}

package object trees {
  private[trees] val typesToNumerics: Map[Type, Numeric[_]] = {
    import Numeric._
    Map(
      typeOf[Byte] -> implicitly[Numeric[Byte]],
      typeOf[Short] -> implicitly[Numeric[Short]],
      typeOf[Int] -> implicitly[Numeric[Int]],
      typeOf[Long] -> implicitly[Numeric[Long]],
      typeOf[Float] -> implicitly[Numeric[Float]],
      typeOf[Double] -> implicitly[Numeric[Double]],
      typeOf[Char] -> implicitly[Numeric[Char]],
      typeOf[math.BigInt] -> implicitly[Numeric[math.BigInt]],
      typeOf[math.BigDecimal] -> implicitly[Numeric[math.BigDecimal]]
    )
  }

  def simplifyGenericTree(tree: Tree): Tree = {
    val f = GenericTrees.simplifier orElse NumericTrees.simplifier
    val transformer = new Transformer {
      val self = (tree: Tree) => transform(tree)

      override def transform(tree: Tree): Tree = {
        val sup = (tt: (Tree, Tree => Tree)) => super.transform(tt._1)
        f.applyOrElse((tree, self), sup)
      }
    }
    transformer.transform(tree)
  }
}
