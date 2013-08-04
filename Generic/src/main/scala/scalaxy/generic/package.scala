package scalaxy

import scala.language.experimental.macros
import scala.reflect.macros.Context

import scala.language.implicitConversions
import scala.language.dynamics

import scala.reflect.ClassTag
import scala.reflect.NameTransformer
import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror

import scalaxy.debug._

// import scalaxy.union._ //`|`
/**
 * Type used to model constraint alternatives in Generic
 */
// trait |[A, B]

package object generic {
  //type TypeTag[T] = scala.reflect.runtime.universe.TypeTag[T]

  private val typeTagsToNumerics: Map[scala.reflect.runtime.universe.TypeTag[_], Numeric[_]] = {
    import Numeric._
    Map(
      TypeTag.Byte -> implicitly[Numeric[Byte]],
      TypeTag.Short -> implicitly[Numeric[Short]],
      TypeTag.Int -> implicitly[Numeric[Int]],
      TypeTag.Long -> implicitly[Numeric[Long]],
      TypeTag.Float -> implicitly[Numeric[Float]],
      TypeTag.Double -> implicitly[Numeric[Double]],
      TypeTag.Char -> implicitly[Numeric[Char]],
      typeTag[math.BigInt] -> implicitly[Numeric[math.BigInt]],
      typeTag[math.BigDecimal] -> implicitly[Numeric[math.BigDecimal]]
    )
  }
  private val typesToNumerics: Map[scala.reflect.runtime.universe.Type, Numeric[_]] = {
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

  def genericTypeTag[A: Generic]: TypeTag[A] = implicitly[Generic[A]].typeTag

  implicit def mkNumeric[A: TypeTag]: Option[Numeric[A]] = {
    // println(typeTagsToNumerics)
    val numOpt = typeTagsToNumerics.get(implicitly[TypeTag[A]])
    numOpt.map(_.asInstanceOf[Numeric[A]])
  }

  implicit def numeric[A: Generic] = implicitly[Generic[A]].numeric.getOrElse(sys.error("This generic has no associated numeric"))

  def zero[A: Generic]: A = numeric[A].zero
  def one[A: Generic]: A = numeric[A].one
  def number[A: Generic](value: Int): A = numeric[A].fromInt(value)

  // implicit def mkGenericOps[A: Generic](value: GenericOps[_]): GenericOps[A] = new GenericOps[A](value.value.asInstanceOf[A])
  implicit def mkGenericOps[A: Generic](value: A): GenericOps[A] = new GenericOps[A](value)

  def simplifyGenerics(tree: Tree): Tree = {
    // println("SIMPLIFYING " + tree)
    val genericPackageSymbol = currentMirror.staticModule("scalaxy.generic.package")

    object GenericPackage {
      def unapply(tree: Tree): Boolean = tree.symbol == genericPackageSymbol
    }
    object GenericOpsCreation {
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
    object Name {
      def unapply(n: Name): Option[String] = Some(n.toString)
    }
    object GenericOpsCall {
      def unapply(tree: Tree): Option[(Tree, Type, String, String)] = Option(tree) collect {

        case Apply(Select(GenericOpsCreation(target, tpe), callName), List(Literal(Constant(methodName: String)))) =>
          (target, tpe, callName.toString, methodName)
      }
    }
    object NumberCall {
      def unapply(tree: Tree): Option[(Int, Type)] = Option(tree) collect {

        case Apply(TypeApply(Select(GenericPackage(), Name(n @ ("one" | "zero"))), List(tpt)), List(ev)) =>
          (if (n == "one") 1 else 0, tpt.tpe)

        case Apply(Apply(TypeApply(Select(GenericPackage(), Name("number")), List(tpt)), List(Literal(Constant(n: Int)))), List(ev)) =>
          (n, tpt.tpe)
      }
    }
    object AsInstanceOf {
      def unapply(tree: Tree): Option[(Tree, Type)] = Option(tree) collect {

        case Apply(TypeApply(Select(target, Name("asInstanceOf")), List(tpt)), Nil) =>
          (target, tpt.tpe)

        case TypeApply(Select(target, name), List(tpt)) if name.toString == "asInstanceOf" =>
          (target, tpt.tpe)
      }
    }
    object ConcreteType {
      def unapply(tpe: Type): Boolean = {
        !tpe.typeSymbol.asType.isAbstractType
      }
    }
    val transformer = new Transformer {
      override def transform(tree: Tree): Tree = tree match {
        case AsInstanceOf(target, tpe) if target.tpe =:= tpe =>
          //println(s"target.tpe = ${target.tpe}, tpe = ${tpe}\n\ttarget.tpe =:= tpe = ${target.tpe =:= tpe}")
          transform(target)

        case NumberCall(n, tpe) if typesToNumerics.contains(tpe) =>
          Literal(Constant(typesToNumerics(tpe).fromInt(n)))

        case Apply(GenericOpsCall(target, ConcreteType(), "applyDynamic", methodName), args) =>
          Apply(Select(transform(target), NameTransformer.decode(methodName)), args.map(transform(_)))

        case Apply(GenericOpsCall(target, ConcreteType(), "updateDynamic", methodName), List(value)) =>
          Apply(Select(transform(target), "update": TermName), List(transform(value)))

        case GenericOpsCall(target, ConcreteType(), "selectDynamic", methodName) =>
          Select(transform(target), NameTransformer.decode(methodName))

        case _ =>
          super.transform(tree)
      }
    }
    transformer.transform(tree)
  }
  //def generic[A: Generic](value: A, implicitConversions: Any*) = new GenericOps[A](value, implicitConversions)
}
