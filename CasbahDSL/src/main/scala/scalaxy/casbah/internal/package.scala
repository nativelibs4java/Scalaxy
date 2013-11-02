package scalaxy.casbah

import scala.reflect.macros.Context

import com.mongodb.casbah.Imports.MongoDBObject

package object internal {

  def queryImpl[A: c.WeakTypeTag](c: Context)(f: c.Expr[Doc => A]): c.Expr[MongoDBObject] = {
    import c.universe._

    // Annotation types.
    val OpTpe = typeOf[op]
    val Func1Tpe = typeOf[func1]
    val Func2Tpe = typeOf[func2]
    val FuncOpTpe = typeOf[funcOp]
    val OpFuncTpe = typeOf[opFunc]
    val ApplyDynNamedFuncTpe = typeOf[applyDynNamedFunc]
    val UpdateDynFuncTpe = typeOf[updateDynFunc]
    val PeelTpe = typeOf[peel]
    val TestOp0Tpe = typeOf[testOp0]
    val TestOp1Tpe = typeOf[testOp1]

    // Some useful extractors.
    object Str {
      def unapply(tree: Tree) = Option(tree) collect {
        case Literal(Constant(s: String)) => s
      }
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
    object Eq {
      def unapply(tree: Tree) = Option(tree) collect {
        case Apply(Select(a, N("$eq$eq")), List(b)) => (a, b)
      }
    }
    object Op {
      def unapply(tree: Tree) = Option(tree) collect {
        case Apply(Annotated(OpTpe, List(op), Select(a, n)), List(b)) =>
          (a, op: TermName, b)
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
              case Eq(Annotated(TestOp0Tpe, List(op), Select(a, n)), b) =>
                Apply(Select(transform(a), op: TermName), List(transform(b)))

              case Eq(Apply(Annotated(TestOp0Tpe, List(op), Select(a, n)), List()), b) =>
                Apply(Select(transform(a), op: TermName), List(transform(b)))

              case Eq(Apply(Annotated(TestOp1Tpe, List(op), Select(a, n)), List(b)), c) =>
                Apply(Select(transform(a), op: TermName), List(transform(b), transform(c)))

              case Op(a, op, b) =>
                Apply(Select(transform(a), op), List(transform(b)))

              case Apply(Annotated(Func2Tpe, List(op), Select(a, _)), List(b)) =>
                Apply(Ident(op: TermName), List(transform(a), transform(b)))

              case Apply(Annotated(Func1Tpe, List(op), Select(_, _)), List(a)) =>
                Apply(Ident(op: TermName), List(transform(a)))

              case Apply(Annotated(FuncOpTpe, List(func, op), Select(a, n)), List(b)) =>
                Apply(
                  Select(
                    Apply(Ident(func: TermName), List(transform(a))),
                    op: TermName),
                  List(transform(b)))

              case Apply(Annotated(OpFuncTpe, List(op, func), Select(a, n)), List(b)) =>
                Apply(
                  Ident(func: TermName),
                  List(Apply(Select(transform(a), op: TermName), List(transform(b)))))

              case Apply(Apply(Annotated(ApplyDynNamedFuncTpe, List(func), s @ Select(_, n)), List(Str(m))), args) =>

                if (m.toString != "apply")
                  c.error(s.pos, s"Expected `apply`, got `$m`")

                Apply(
                  Ident(func: TermName),
                  args.map {
                    case a @ Apply(_, List(Str(n), v)) =>
                      if (n == "")
                        c.error(v.pos, "Please specify names for all arguments.")
                      transform(a)
                  })

              case Apply(Apply(Annotated(UpdateDynFuncTpe, List(func), Select(_, n)), List(c)), List(v)) =>

                Apply(
                  Ident(func: TermName),
                  List(
                    Apply(
                      Select(transform(c), "$minus$greater": TermName),
                      List(transform(v)))))

              case Apply(Annotated(PeelTpe, Nil, Select(_, n)), List(a)) =>
                transform(a)

              case TypeApply(Select(a, N("isInstanceOf")), tparams) =>
                TypeApply(Select(transform(a), "$type": TermName), tparams)

              case _ =>
                super.transform(tree)
            }
          } transform body
        )
      case _ =>
        c.error(f.tree.pos, "Query function must be a closure.")
        null
    }
    println(result)
    result
  }
}
