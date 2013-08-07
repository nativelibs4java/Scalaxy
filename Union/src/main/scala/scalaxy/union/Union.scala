package scalaxy.union

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.language.dynamics

import scala.reflect._
import scala.reflect.macros.Context

import scala.annotation.implicitNotFound

/**
 * Union type.
 */
abstract class |[+A, +B] extends Dynamic {
  /**
   * Type-class definition, handy to ask for a proof that `T` matches this union type.
   */
  type Union[T] = T =|= (A | B)

  def value: Any

  def applyDynamic(name: String)(args: Any*): Any = macro |.applyDynamic[A | B]
}

object | {

  //implicit def wrap[T <: (_ | _): runtime.universe.TypeTag](value: Any): T = macro wrap[T]
  // implicit def |[A, B, C <: A | B](value: Any): C = macro internal.wrap3[A, B, C]

  def doApplyDynamic(target: Any, name: String, args: Any*): Any = {
    // TODO: implement Generic-style dynamic methods and check with macro they belong to one of the types.
    ???
  }

  def applyDynamic[T: c.WeakTypeTag](c: Context)(name: c.Expr[String])(args: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val target = c.prefix.tree match {
      case tree @ Apply(target, List(value)) if target.symbol.isMethod && target.symbol.isImplicit =>
        value
      case tree =>
        Select(tree, "value": TermName)
    }
    //val prefix = c.prefix.asInstanceOf[c.Expr[_ <: (_ | _)]]
    // val value = 
    val unionModule = rootMirror.staticModule("scalaxy.union.|")

    c.Expr[Any](
      Apply(
        Ident(unionModule), //reify(|).tree,
        target :: name.tree :: args.map(_.tree).toList
      )
    )
    // reify(doApplyDynamic(prefix.splice)(name.splice)(args))
  }
}
