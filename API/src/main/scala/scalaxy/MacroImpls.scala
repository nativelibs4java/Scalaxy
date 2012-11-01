package scalaxy

import language.experimental.macros

import scala.reflect.macros.Context

import scala.reflect.runtime.{ universe => ru }

object macros
{
  def newListTree[T: ru.TypeTag](c: Context)(
      values: List[c.universe.Tree]): c.Expr[List[T]] =
  {
    import c.universe._
    c.Expr[List[T]](
      Apply(
        TypeApply(
          Select(
            Select(
              Ident(newTermName("scala")).setSymbol(definitions.ScalaPackage),
              newTermName("List")
            ),
            newTermName("apply")
          ),
          List(TypeTree(typeOf[T]))
        ),
        values
      )
    )
  }

  private def expr[T](c: Context)(x: c.Expr[T]): c.Expr[ru.Expr[T]] = {
    c.Expr[ru.Expr[T]](
      c.reifyTree(
        c.universe.treeBuild.mkRuntimeUniverseRef,
        c.universe.EmptyTree,
        c.typeCheck(x.tree)
      )
    )
  }
  private def tree(c: Context)(x: c.Expr[Any]): c.universe.Tree =
    expr[Any](c)(x).tree

  def fail(c: Context)(message: c.Expr[String])(pattern: c.Expr[Any]): c.Expr[MatchError] =
    c.universe.reify(new MatchError(expr(c)(pattern).splice, message.splice))

  def warn(c: Context)(message: c.Expr[String])(pattern: c.Expr[Any]): c.Expr[MatchWarning] =
    c.universe.reify(new MatchWarning(expr(c)(pattern).splice, message.splice))

  def replace[T](c: Context)(pattern: c.Expr[T], replacement: c.Expr[T]): c.Expr[Replacement] = {
    import c.universe._
    c.universe.reify(new Replacement(expr(c)(pattern).splice, expr(c)(replacement).splice))
  }

  def when[T](c: Context)(pattern: c.Expr[T])(idents: c.Expr[Any]*)(thenMatch: c.Expr[PartialFunction[List[ru.Tree], Action[T]]])
  : c.Expr[ConditionalAction[T]] =
  {
    import c.universe._

    val scalaCollection =
      Select(Ident(newTermName("scala")), newTermName("collection"))

    c.Expr[ConditionalAction[T]](
      New(
        Select(Ident(newTermName("scalaxy")), newTypeName("ConditionalAction")),
        List(List(
          tree(c)(pattern),
          Apply(
            Select(Select(scalaCollection, newTermName("Seq")), newTermName("apply")),
            idents.map(_.tree).toList.map { case Ident(n) => Literal(Constant(n.toString)) }
          ),
          thenMatch.tree
        ))
      )
    )
  }

  def replacement[T: c.WeakTypeTag](c: Context)(replacement: c.Expr[T]): c.Expr[ReplaceBy[T]] =
    c.universe.reify(new ReplaceBy[T](expr(c)(replacement).splice))
}

