package scalaxy.union

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.language.dynamics

import scala.reflect._
import scala.reflect.macros.Context

import scala.annotation.implicitNotFound

package object internal {

  def <|<[A: c.WeakTypeTag, B: c.WeakTypeTag, T <: (A <|< B): c.WeakTypeTag](c: Context): c.Expr[T] = {
    check[A, B](c)(_ <:< _, "match or derive from")
    c.literalNull.asInstanceOf[c.Expr[T]]
  }

  def =|=[A: c.WeakTypeTag, B: c.WeakTypeTag, T <: (A =|= B): c.WeakTypeTag](c: Context): c.Expr[T] = {
    check[A, B](c)(_ =:= _, "match")
    c.literalNull.asInstanceOf[c.Expr[T]]
  }

  def as[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context): c.Expr[B] = {
    // TODO !!!
    import c.universe._

    // println("TODO as implementation !!!")
    val Apply(TypeApply(constr, List(tpt)), List(value)) = c.prefix.tree
    wrap[A, B](c)(c.Expr[A](value))
  }

  private def debugType(c: Context)(t: c.universe.Type) {
    import c.universe._
    println(s"""
      WRAP t = $t
          typeConstructor = ${t.typeConstructor}
          baseClasses = ${t.baseClasses.mkString(", ")}
          (t <:< typeOf[_ | _]) = ${(t <:< typeOf[_ | _])}
          (t =:= typeOf[_ | _]) = ${(t =:= typeOf[_ | _])}
    """)
  }

  // def wrap2[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(value: c.Expr[Any]): c.Expr[A | B] = {
  //   import c.universe._
  //   wrap[A | B](c)(value)
  // }

  // def wrap3[A: c.WeakTypeTag, B: c.WeakTypeTag, C: c.WeakTypeTag](c: Context)(value: c.Expr[Any]): c.Expr[C] = {
  //   import c.universe._
  //   wrap[C](c)(value)
  // }

  def cast[A: c.WeakTypeTag, B: c.WeakTypeTag, C <: (_ | _): c.WeakTypeTag](c: Context): c.Expr[C] = {
    import c.universe._
    //check[A, B](c)(_ <:< _, "match or derive from")
    println(s"CAST a = ${weakTypeTag[A].tpe}, b = ${weakTypeTag[B].tpe}, c = ${weakTypeTag[C].tpe}")
    c.literalNull.asInstanceOf[c.Expr[C]]
  }

  private def check[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(test: (c.universe.Type, c.universe.Type) => Boolean, opString: String) {
    import c.universe._
    checkType(c)(weakTypeTag[A].tpe, weakTypeTag[B].tpe, test, opString)
  }
  private[union] def checkType(c: Context)(a: c.universe.Type, b: c.universe.Type, test: (c.universe.Type, c.universe.Type) => Boolean, opString: String) {
    import c.universe._

    // println(s"a = $a, b = $b, openImplicits = ${c.openImplicits}")
    if (!testType(c)(a, b, test)) {
      val types = getUnionTypes(c)(b)
      val msg = "Type " + a + " does not " + opString + " " +
        (if (types.size == 1) b else "any of " + types.mkString("(", " | ", ")"))
      // c.info(c.prefix.tree.pos, msg, true)
      c.error(c.prefix.tree.pos, msg)
    }
  }

  //private class TypeExtractor {}
  def extractTypes(u: api.Universe)(tpe: u.Type, filter: u.Type => Boolean): List[(u.Type, u.Type => u.Type)] = {
    import u._

    // TODO: deal with covariance vs. contravariance
    val selfExtraction = if (filter(tpe)) List(tpe -> ((t: Type) => t)) else Nil
    val subExtractions = tpe match {
      case TypeRef(pre, sym, tparams) =>
        lazy val getTParams = (t: Type) => {
          val TypeRef(pre, sym, tparams) = t
          tparams
        }
        for (
          (sub, i) <- tparams.map(extractTypes(u)(_, filter)).zipWithIndex;
          (subTpe, subExtractor) <- sub
        ) yield {
          subTpe -> ((t: Type) => subExtractor(getTParams(t)(i)))
        }
      case t @ ExistentialType(quantified, underlying) =>
        lazy val getUnderlying = (t: Type) => {
          val ExistentialType(quantified, underlying) = t
          underlying
        }
        for (((subTpe, subExtractor), i) <- extractTypes(u)(underlying, filter).zipWithIndex) yield {
          subTpe -> ((t: Type) => subExtractor(getUnderlying(t)))
        }
      case t =>
        println("TODO: handle type " + t + ": " + t.getClass.getName)
        Nil
    }
    selfExtraction ++ subExtractions
  }

  private[union] def testType(c: Context)(a: c.universe.Type, b: c.universe.Type, test: (c.universe.Type, c.universe.Type) => Boolean): Boolean = {
    import c.universe._

    val types = getUnionTypes(c)(b)
    if (types.size == 1) {
      val t = types.head
      test(a, t) || {
        val extractions = extractTypes(c.universe)(t, _ <:< typeOf[(_ | _)])
        val tWithoutUnions = t.map(tpe => {
          if (tpe <:< typeOf[(_ | _)])
            WildcardType
          else
            tpe
        })
        test(a, tWithoutUnions) && extractions.forall({
          case (tpe, extractor) =>
            val actualTpe = extractor(a)
            println(s"Extracted $actualTpe out of $a for placeholder union $tpe")
            testType(c)(actualTpe, tpe, test)
        })
      }
    } else {
      types.exists(tpe => testType(c)(a, tpe, test))
    }
  }
  private def getUnionTypes(c: Context)(tpe: c.universe.Type): List[c.universe.Type] = {
    import c.universe._

    // TODO: proper OR and AND of types, map types and explode them with alternatives...
    def sub(tpe: Type): List[Type] = {
      if (tpe <:< typeOf[_ | _]) {
        val TypeRef(_, _, List(a, b)) = tpe.normalize.baseType(typeOf[|[_, _]].typeSymbol)
        sub(a) ++ sub(b)
      } else if (tpe <:< typeOf[_ <|< _]) {
        val TypeRef(_, _, List(a, b)) = tpe.normalize.baseType(typeOf[<|<[_, _]].typeSymbol)
        sub(b)
      } else if (tpe <:< typeOf[_ =|= _]) {
        val TypeRef(_, _, List(a, b)) = tpe.normalize.baseType(typeOf[=|=[_, _]].typeSymbol)
        sub(b)
      } else {
        List(tpe)
      }
    }
    sub(tpe)
  }
}
