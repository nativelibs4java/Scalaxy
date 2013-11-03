package scalaxy.casbah

import scala.reflect.macros.Context

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.query.dsl._

package object internal {

  private val verbose = "1" == System.getenv("SCALAXY_CASBAH_VERBOSE")

  def queryImpl[A: c.WeakTypeTag](c: Context)(f: c.Expr[Doc => A]): c.Expr[MongoDBObject] = {
    import c.universe._

    // Annotation types.
    val BoolColTpe = typeOf[BoolCol]
    val OpTpe = typeOf[op]
    val Func1Tpe = typeOf[func1]
    val Func2Tpe = typeOf[func2]
    val FuncOpTpe = typeOf[funcOp]
    val OpFuncTpe = typeOf[opFunc]
    val ApplyDynNamedFuncTpe = typeOf[applyDynNamedFunc]
    val UpdateDynFuncTpe = typeOf[updateDynFunc]
    val PathTpe = typeOf[path]
    val PeelTpe = typeOf[peel]
    val TestOp0Tpe = typeOf[testOp0]
    val TestMeth1 = typeOf[testMeth1]
    val TestOp1Tpe = typeOf[testOp1]

    // Some useful extractors.
    object Str {
      def unapply(tree: Tree) = Option(tree) collect {
        case Literal(Constant(s: String)) => s
      }
      def apply(s: String) = Literal(Constant(s))
    }
    object N {
      def unapply(n: Name) = Option(n.toString)
    }
    object Annotated {
      def unapply(tree: Tree) = {
        Option(tree.symbol).flatMap(_.annotations.headOption.map(a => {
          (a.tpe, a.scalaArgs map { case Str(s) => s }, tree)
        }))
      }
    }
    object TermsAnnotated {
      def unapply(tree: Tree) = Annotated.unapply(tree).map {
        case (tpe, names, tree) => (tpe, Option(names).map(_.map(newTermName(_))).orNull, tree)
      }
    }
    object Eq {
      def unapply(tree: Tree) = Option(tree) collect {
        case q"$a == $b" => (a, b)
      }
    }
    object Typed {
      def unapply(tree: Tree) = if (tree.tpe != null) Some(tree.tpe, tree) else None
    }
    object NullaryApply {
      def unapply(tree: Tree) = Option(tree) collect {
        case Apply(s @ Select(_, _), Nil) => s
        case Select(_, _) => tree
      }
    }
    object Not {
      def unapply(tree: Tree) = Option(tree) collect {
        case q"!$a" => a
      }
    }
    object Op {
      def unapply(tree: Tree) = Option(tree) collect {
        case Apply(TermsAnnotated(OpTpe, List(op), Select(a, n)), List(b)) =>
          (a, op, b)
        case Eq(a, b) if a.tpe != null && a.tpe <:< typeOf[Col] =>
          (a, "$eq": TermName, b)
      }
    }

    // Transform closure body recursively.
    val result = c.typeCheck(f.tree) match {
      case Function(List(param), body) =>
        c.Expr[MongoDBObject](
          new Transformer {
            override def transform(tree: Tree) = tree match {

              case Eq(NullaryApply(TermsAnnotated(TestOp0Tpe, List(op), Select(a, _))), b) =>
                q"${transform(a)} $op ${transform(b)}"

              case Eq(Apply(TermsAnnotated(TestOp1Tpe, List(op), Select(a, _)), List(b)), c) =>
                q"${transform(a)} $op (${transform(b)} , ${transform(c)}) "

              case Typed(BoolColTpe, Apply(TermsAnnotated(TestMeth1, List(func), Select(_, _)), List(b))) =>
                q"${transform(b)} $func true"

              case Op(a, op, b) =>
                q"${transform(a)} $op ${transform(b)}"

              case Not(Typed(BoolColTpe, Apply(TermsAnnotated(TestMeth1, List(func), Select(_, _)), List(b)))) =>
                q"${transform(b)} $func false"

              case Apply(TermsAnnotated(Func2Tpe, List(op), Select(a, _)), List(b)) =>
                q"$op(${transform(a)}, ${transform(b)})"

              case Apply(TermsAnnotated(Func1Tpe, List(op), Select(_, _)), List(a)) =>
                q"$op(${transform(a)})"

              case Apply(TermsAnnotated(FuncOpTpe, List(func, op), Select(a, _)), List(b)) =>
                q"$func(${transform(a)}) $op ${transform(b)}"

              case Apply(TermsAnnotated(OpFuncTpe, List(op, func), Select(a, _)), List(b)) =>
                q"$func(${transform(a)} $op ${transform(b)})"

              case Apply(Apply(TermsAnnotated(ApplyDynNamedFuncTpe, List(func), s @ Select(_, _)), List(Str(m))), args) =>

                if (m.toString != "apply")
                  c.error(s.pos, s"Expected `apply`, got `$m`")

                val transformedArgs = args.map {
                  case a @ Apply(_, List(Str(n), v)) =>
                    if (n == "")
                      c.error(v.pos, "Please specify names for all arguments.")
                    transform(a)
                }
                q"$func(..$transformedArgs)"

              case Apply(Apply(TermsAnnotated(UpdateDynFuncTpe, List(func), Select(_, N("updateDynamic"))), List(c)), List(v)) =>
                q"$func(${transform(c)} -> ${transform(v)})"

              case Apply(Annotated(PeelTpe, Nil, Select(_, _)), List(a)) =>
                transform(a)

              case Apply(Annotated(PathTpe, Nil, s @ Select(a, N("selectDynamic"))), List(Str(b))) =>
                transform(a) match {
                  case Str(aa) => Str(aa + "." + b)
                  case _ => s
                }

              case q"$a.isInstanceOf[..$tparams]" =>
                val n: TermName = "$type"
                q"${transform(a)}.$n[..$tparams]"

              case _ =>
                super.transform(tree)
            }
          } transform body
        )
      case _ =>
        c.error(f.tree.pos, "Query function must be a closure.")
        null
    }
    if (verbose)
      println(result)
    result
  }
}
