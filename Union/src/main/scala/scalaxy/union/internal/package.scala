package scalaxy.union

import scala.language.experimental.macros
import scala.language.implicitConversions

import scala.reflect._
import scala.reflect.macros.Context

package object internal {

  def <|<[A: c.WeakTypeTag, B: c.WeakTypeTag, T <: (A <|< B): c.WeakTypeTag](c: Context): c.Expr[T] = {
    checkMatchesMemberOfUnion[A, B](c)
    c.literalNull.asInstanceOf[c.Expr[T]]
  }

  def =|=[A: c.WeakTypeTag, B: c.WeakTypeTag, T <: (A =|= B): c.WeakTypeTag](c: Context): c.Expr[T] = {
    checkMemberOfUnion[A, B](c)
    c.literalNull.asInstanceOf[c.Expr[T]]
  }

  def as[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context): c.Expr[B] = {
    import c.universe._

    val Apply(TypeApply(constr, List(tpt)), List(value)) = c.prefix.tree
    wrap[A, B](c)(c.Expr[A](value))
  }

  private[union] def checkMemberOfUnion[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context) {
    check[A, B](c)(_ =:= _, "match")
  }

  private[union] def checkMatchesMemberOfUnion[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context) {
    check[A, B](c)(_ <:< _, "match or derive from")
  }

  private def check[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(test: (c.universe.Type, c.universe.Type) => Boolean, opString: String) {
    import c.universe._

    val a = weakTypeTag[A].tpe
    val b = weakTypeTag[B].tpe

    if (!testType(c)(a, b, test)) {
      val types = getUnionTypes(c)(b)
      val msg = "Type " + a + " does not " + opString + " " +
        (if (types.size == 1) b else "any of " + types.mkString("(", " | ", ")"))
      c.error(c.prefix.tree.pos, msg)
    }
  }

  private def extractTypes(u: api.Universe)(tpe: u.Type, filter: u.Type => Boolean): List[(u.Type, u.Type => u.Type)] = {
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

  private def testType(c: Context)(a: c.universe.Type, b: c.universe.Type, test: (c.universe.Type, c.universe.Type) => Boolean): Boolean = {
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
            // println(s"Extracted $actualTpe out of $a for placeholder union $tpe")
            testType(c)(actualTpe, tpe, test)
        })
      }
    } else {
      types.exists(tpe => testType(c)(a, tpe, test))
    }
  }

  private[union] def getUnionTypes(c: Context)(tpe: c.universe.Type): List[c.universe.Type] = {
    import c.universe._

    def sub(tpe: Type): List[Type] = {
      val ntpe = tpe.normalize
      if (tpe <:< typeOf[_ | _]) {
        val TypeRef(_, _, List(a, b)) = ntpe.baseType(typeOf[|[_, _]].typeSymbol)
        sub(a) ++ sub(b)
      } else {
        List(tpe)
      }
    }
    sub(tpe)
  }
}
