package scalaxy.reified

import scala.language.experimental.macros

import scala.reflect._
import scala.reflect.macros.Context
import scala.reflect.runtime.universe

import scalaxy.reified.internal.Utils._
import scalaxy.generic.Generic

package object internal {

  private[reified] lazy val verbose =
    System.getProperty("scalaxy.reified.verbose") == "true" ||
      System.getenv("SCALAXY_REIFIED_VERBOSE") == "1"

  private[reified] val syntheticVariableNamePrefix = "scalaxy$reified$"

  private def runtimeExpr[A](c: Context)(tree: c.universe.Tree): c.Expr[universe.Expr[A]] = {
    c.Expr[universe.Expr[A]](
      c.reifyTree(
        c.universe.treeBuild.mkRuntimeUniverseRef,
        c.universe.EmptyTree,
        tree
      )
    )
  }

  private[reified] def reifyMacro[A: universe.TypeTag](v: A): ReifiedValue[A] = macro reifyImpl[A]
  private[reified] def reifyWithDifferentRuntimeValue[A: universe.TypeTag](v: A, runtimeValue: A): ReifiedValue[A] = macro reifyWithDifferentRuntimeValueImpl[A]

  def reifyImpl[A: c.WeakTypeTag](c: Context)(v: c.Expr[A])(tt: c.Expr[universe.TypeTag[A]]): c.Expr[ReifiedValue[A]] = {

    import c.universe._

    // println("REIFYING: " + v)

    val (expr, capturesExpr) = transformReifiedRefs(c)(v)
    val res = reify({
      implicit val valueTag = tt.splice
      new ReifiedValue[A](
        v.splice,
        Utils.typeCheck(expr.splice, valueTag.tpe),
        capturesExpr.splice)
    })
    // println("RESULT: " + res)
    res
  }

  def reifyWithDifferentRuntimeValueImpl[A: c.WeakTypeTag](c: Context)(v: c.Expr[A], runtimeValue: c.Expr[A])(tt: c.Expr[universe.TypeTag[A]]): c.Expr[ReifiedValue[A]] = {

    import c.universe._

    val (expr, capturesExpr) = transformReifiedRefs(c)(v)
    reify({
      implicit val valueTag = tt.splice
      new ReifiedValue[A](
        runtimeValue.splice,
        Utils.typeCheck(expr.splice, valueTag.tpe),
        capturesExpr.splice)
    })
  }

  /**
   * Detect captured references, replace them by capture tags and
   *  return their ordered list along with the resulting tree.
   */
  private def transformReifiedRefs[A](c: Context)(expr: c.Expr[A]): (c.Expr[universe.Expr[A]], c.Expr[Seq[(AnyRef, universe.Type)]]) = {
    import c.universe._
    import definitions._

    val tree = c.typeCheck(expr.tree)

    val localDefSyms = collection.mutable.HashSet[Symbol]()
    def isDefLike(t: Tree) = t match {
      case Template(_, _, _) => true
      case Function(_, _) => true
      case _ if t.isDef => true
      case _ => false
    }
    (new Traverser {
      override def traverse(t: Tree) {
        super.traverse(t)
        if (t.symbol != null && isDefLike(t)) {
          localDefSyms += t.symbol
        }
      }
    }).traverse(tree)

    var lastCaptureIndex = -1
    val capturedTerms = collection.mutable.ArrayBuffer[(Tree, Type)]()
    val capturedSymbols = collection.mutable.HashMap[TermSymbol, Int]()
    val capturedTypeTags = collection.mutable.Set[String]()

    val transformer = new Transformer {
      override def transform(t: Tree): Tree = {
        // TODO check which types can be captured
        val sym = t.symbol
        // if (t.toString == "c") {
        //   println(s"FOUND c: localDefSyms = $localDefSyms")
        // }
        if (sym != null && !isDefLike(t) && sym.isTerm && !localDefSyms.contains(sym)) {
          val tsym = sym.asTerm
          if (tsym.isVar) {
            c.error(t.pos, "Cannot capture this var: " + tsym)
            t
          } else if (tsym.isLazy && tsym.fullName != "scala.reflect.runtime.universe") {
            c.error(t.pos, "Cannot capture this lazy val: " + tsym)
            t
          } else if (tsym.isMethod || tsym.isModule && tsym.isStable) {
            super.transform(t)
          } else if (tsym.isVal || tsym.isAccessor) {
            val tpe = t.tpe.normalize.widen // if (tpe <:< typeOf[Generic[_]]) {

            //   Literal(Constant(null))
            // } else 
            {
              val captureIndexExpr = c.literal(
                capturedSymbols.get(tsym) match {
                  case Some(i) =>
                    // println("FOUND SYMBOL ALREADY CAPTURED: " + tsym + ", tree = " + t)
                    i
                  case None =>
                    c.info(t.pos, "Reified value will capture " + tsym + " (type: " + tpe + ")", false)

                    lastCaptureIndex += 1
                    capturedSymbols += tsym -> lastCaptureIndex
                    capturedTerms += Ident(tsym) -> t.tpe

                    lastCaptureIndex
                }
              )

              // Abuse reify to get correct reference to `capture`.
              val Apply(TypeApply(f, List(_)), _) = {
                reify(scalaxy.reified.internal.CaptureTag[Int](10, 1)).tree
              }
              c.typeCheck(
                Apply(
                  TypeApply(
                    f,
                    List(TypeTree(tpe))),
                  List(t, captureIndexExpr.tree)),
                tpe)
            }
          } else {
            c.error(t.pos, s"Cannot capture this type of expression (symbol = $tsym): " + tsym)
            t
          }
        } else {
          super.transform(t)
        }
      }
    }

    val transformed = transformer.transform(tree)
    val transformedExpr = runtimeExpr[A](c)(transformed)
    val capturesArrayExpr = {
      // Abuse reify to get correct seq constructor.
      val Apply(seqConstructor, _) = reify(Seq[(AnyRef, universe.Type)]()).tree
      // Build the list of captured terms (with their types).
      c.Expr[Seq[(AnyRef, universe.Type)]](
        c.typeCheck(
          Apply(
            seqConstructor,
            capturedTerms.map({
              case (term, tpe) =>
                val termExpr = c.Expr[Any](term)
                val typeExpr = c.Expr[universe.TypeTag[_]](
                  c.reifyType(
                    treeBuild.mkRuntimeUniverseRef,
                    Select(
                      treeBuild.mkRuntimeUniverseRef,
                      newTermName("rootMirror")
                    ),
                    tpe.normalize.widen
                  )
                )
                reify({
                  (
                    termExpr.splice.asInstanceOf[AnyRef],
                    typeExpr.splice.tpe
                  )
                }).tree
            }).toList),
          c.typeOf[Seq[(AnyRef, universe.Type)]]))
    }
    (transformedExpr, capturesArrayExpr)
  }
}
