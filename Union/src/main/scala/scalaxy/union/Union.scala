package scalaxy.union

import scala.language.experimental.macros
import scala.language.dynamics

import scala.reflect._
import scala.reflect.macros.blackbox.Context

/**
 * Union type.
 */
trait |[A, B] extends Dynamic {
  /**
   * Type-class definition, handy to ask for a proof that `T` matches this union type.
   */
  type Union[T] = T =|= (A | B)

  def value: Any

  def applyDynamic(name: String)(args: Any*): Any = macro |.applyDynamic[A | B]

  // def `match`[R](f: PartialFunction[Any, R]): R = macro |.`match`[A | B, R]
}

object | {

  def doApplyDynamic(target: Any, name: String, args: Any*): Any = {
    // TODO: implement Generic-style dynamic methods and check with macro they belong to one of the types.
    ???
  }

  def applyDynamic[T: c.WeakTypeTag](c: Context)(name: c.Expr[String])(args: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    // val t = weakTypeTag[T].tpe
    val target = c.prefix.tree match {
      case tree @ Apply(target, List(value)) if target.symbol.isMethod && target.symbol.isImplicit =>
        value
      case tree =>
        Select(tree, TermName("value"))
    }

    // val types = internal.getUnionTypes(c)(t)
    // println(s"openImplicits = ${c.openImplicits}")
    // println(s"openImplicits = ${c.openImplicits}")
    // println(s"types = $types")
    // for (tpe <- types) {
    //   val structTpe = refinedType(Nil, NoSymbol, decls, NoPosition)
    //   val conversionType = MethodType(List(tpe), structTpe)
    // }
    //val typedArgs = args.map(c.typeCheck(_.tree))

    c.Expr[Any](
      Apply(
        reify(|).tree,
        target :: name.tree :: args.map(_.tree).toList
      )
    )
  }

  // def `match`[T: c.WeakTypeTag, R: c.WeakTypeTag](c: Context)(f: c.Expr[PartialFunction[Any, R]]): c.Expr[R] = {
  //   import c.universe._

  //   c.literalNull.asInstanceOf[c.Expr[R]]
  // }
}
