package scalaxy

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.language.dynamics

import scala.reflect._
import scala.reflect.macros.Context

import scala.annotation.implicitNotFound

package union {
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

  /**
   * (A <|< B) means that either A <:< B, or if B is an union, there is one member C of B for which A <:< C.
   */
  @implicitNotFound(msg = "Cannot prove that ${A} <|< ${B}.")
  trait <|<[A, B]

  object <|< {
    implicit def <|<[A, B]: A <|< B = macro internal.<|<[A, B, A <|< B]
    // implicit def <|<[A, B]: A <|< B = macro prove[A, A <|< B]
    // implicit def derived_<|<[A, B, T <: (A <|< B)]: T = macro internal.<|<[A, B, T]
    // implicit def prove_<|<[T <: (_ <|< _)]: T = macro internal.prove_<|<[T]
    // implicit def prove_<|<[A, B <: (A <|< _)]: B = macro prove[A, B]
  }

  /**
   * (A =|= B) means that either A =:= B, or if B is an union, there is one member C of B for which A =:= C.
   */
  @implicitNotFound(msg = "Cannot prove that ${A} =|= ${B}.")
  trait =|=[A, B] {
    /**
     * Implicit cast to another union type, if it makes sense.
     */
    implicit def cast[C <: _ | _]: C = macro internal.cast[A, B, C]
  }

  object =|= {
    implicit def selfProof[A, B <: A]: A =|= B = null
    implicit def =|=[A, B]: A =|= B = macro internal.=|=[A, B, A =|= B]
    // implicit def =|=[A, B]: A =|= B = macro prove[A, A =|= B]
    //implicit def derived_=|=[A, B, T <: (A =|= B)]: T = macro internal.=|=[A, B, T]
    // implicit def prove_=|=[T <: (_ =|= _)]: T = macro internal.prove_=|=[T]
    // implicit def prove_=|=[A, B, T <: (A =|= B)]: B = macro prove[A, T]
  }

  private[union] trait Trait
}

package object union {

  def prove[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context): c.Expr[B] = {
    import c.universe._

    val a = c.universe.weakTypeTag[A].tpe
    val b = c.universe.weakTypeTag[B].tpe
    // println("### proving: " + a + " as " + b)

    // b.normalize match {
    //   case TypeRef(_, _, List(tparam)) =>
    if (b <:< typeOf[_ <|< _]) {
      val TypeRef(_, _, List(aa, constraint)) = b.normalize.baseType(typeOf[<|<[_, _]].typeSymbol)
      println("### constraint: " + constraint)
      internal.checkType(c)(a, constraint, _ <:< _, "match or derive from")
    } else if (b <:< typeOf[_ =|= _]) {
      val TypeRef(_, _, List(aa, constraint)) = b.normalize.baseType(typeOf[=|=[_, _]].typeSymbol)
      println("### constraint: " + constraint)
      internal.checkType(c)(a, constraint, _ =:= _, "match")
    } else {
      c.error(c.prefix.tree.pos, "Cannot prove " + a + " as " + b + " (unknown type)")
    }
    //   case _ =>
    //     c.error(c.prefix.tree.pos, "Cannot prove " + a + " as " + b + " (not a type-class, expecting a type with exactly one type parameter)")
    // }
    c.literalNull.asInstanceOf[c.Expr[B]]
  }

  def wrap[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(value: c.Expr[A]): c.Expr[B] = {
    import c.universe._

    val a = weakTypeTag[A].tpe
    val b = weakTypeTag[B].tpe

    internal.checkType(c)(a, b, _ =:= _, "match")

    val valueExpr = value
    val expr = reify {
      new Trait {
        val value: Any = valueExpr.splice
      }
    }
    // println("expr = " + expr)
    val replaced = expr.tree.substituteSymbols(
      List(typeOf[Trait].typeSymbol),
      List(b.typeSymbol)
    )
    // println(replaced)
    c.Expr[B](replaced)
    //c.literalNull.asInstanceOf[c.Expr[B]]
  }

  // def wrap[T](value: Any) = macro wrap[T]
  implicit class AnyOps[A](value: A) {
    def as[B]: B = macro internal.as[A, B]
  }
}
