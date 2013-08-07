package scalaxy

import scala.language.experimental.macros
import scala.reflect._
import scala.reflect.macros.Context
import scala.annotation.implicitNotFound

package union {
  /**
   * Union type.
   */
  trait |[A, B] {
    /**
     * Type-class definition, handy to ask for a proof that `T` matches this union type.
     */
    type Union[T] = T =|= (A | B)

    // TODO: implement Generic-style dynamic methods and check with macro they belong to one of the types.
    ???
  }
  // def apply: List[TypeTag[_]] = macro internal.flattenUnion[A, B]

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
  }

  object =|= {
    implicit def =|=[A, B]: A =|= B = macro internal.=|=[A, B, A =|= B]
    // implicit def =|=[A, B]: A =|= B = macro prove[A, A =|= B]
    //implicit def derived_=|=[A, B, T <: (A =|= B)]: T = macro internal.=|=[A, B, T]
    // implicit def prove_=|=[T <: (_ =|= _)]: T = macro internal.prove_=|=[T]
    // implicit def prove_=|=[A, B, T <: (A =|= B)]: B = macro prove[A, T]
  }
}

package object union {
  /**
   * Type-class alias to prove that `A` is a number.
   */
  @implicitNotFound(msg = "Cannot prove that ${A} is a number.")
  type Number[A] = A =|= (Byte | Short | Int | Long | Float | Double)

  def prove[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context): c.Expr[B] = {
    import c.universe._

    val a = c.universe.weakTypeTag[A].tpe
    val b = c.universe.weakTypeTag[B].tpe
    println("### proving: " + a + " as " + b)

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

  object internal {

    def <|<[A: c.WeakTypeTag, B: c.WeakTypeTag, T <: (A <|< B): c.WeakTypeTag](c: Context): c.Expr[T] = {
      check[A, B](c)(_ <:< _, "match or derive from")
      c.literalNull.asInstanceOf[c.Expr[T]]
    }

    def =|=[A: c.WeakTypeTag, B: c.WeakTypeTag, T <: (A =|= B): c.WeakTypeTag](c: Context): c.Expr[T] = {
      check[A, B](c)(_ =:= _, "match")
      c.literalNull.asInstanceOf[c.Expr[T]]
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
        val extractions = extractTypes(c.universe)(t, _ <:< typeOf[(_ | _)])
        val tWithoutUnions = t.map(tpe => {
          if (tpe <:< typeOf[(_ | _)])
            WildcardType
          else
            tpe
        })
        assert(typeOf[Array[Int]] <:< typeOf[Array[_]])
        test(a, tWithoutUnions) && extractions.forall({
          case (tpe, extractor) =>
            val actualTpe = extractor(a)
            println(s"Extracted $actualTpe out of $a for placeholder union $tpe")
            testType(c)(actualTpe, tpe, test)
        })
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
}
