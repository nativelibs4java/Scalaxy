package scalaxy.compilets

import scala.language.experimental.macros
import scala.reflect.macros.Context

import scala.reflect.runtime.{ universe => ru }

object impl
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

  def traverse(u: scala.reflect.api.Universe)(tree: u.Tree)(f: PartialFunction[u.Tree, Unit]) {
    (new u.Traverser { override def traverse(tree: u.Tree) {
      //println("traversing " + tree)
      super.traverse(tree)
      if (f.isDefinedAt(tree))
        f.apply(tree)
    }}).traverse(tree)
  }
  
  def assertNoUnsupportedConstructs(c: Context)(tree: c.universe.Tree) {
    import c.universe._
    def notSupported(t: Tree, what: String) =
      c.error(t.pos, what + " definitions are not supported by Scalaxy yet")
    
    // Coarse validation of supported ASTs:
    traverse(c.universe)(tree) {
      case t @ DefDef(_, _, _, _, _, _) => notSupported(t, "Function / method")
      case t @ ClassDef(_, _, _, _) => notSupported(t, "Class")
      case t @ ModuleDef(_, _, _) => notSupported(t, "Module")
      case t @ TypeDef(_, _, _, _) => notSupported(t, "Type")
      case t @ PackageDef(_, _) => notSupported(t, "Package")
    }
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

  def fail(c: Context)(message: c.Expr[String])(pattern: c.Expr[Any]): c.Expr[MatchError] = {
    assertNoUnsupportedConstructs(c)(pattern.tree)
    c.universe.reify(new MatchError(expr(c)(pattern).splice, message.splice))
  }

  def warn(c: Context)(message: c.Expr[String])(pattern: c.Expr[Any]): c.Expr[MatchWarning] = {
    assertNoUnsupportedConstructs(c)(pattern.tree)
    c.universe.reify(new MatchWarning(expr(c)(pattern).splice, message.splice))
  }

  def replace[T](c: Context)(pattern: c.Expr[T], replacement: c.Expr[T]): c.Expr[Replacement] = {
    import c.universe._
    assertNoUnsupportedConstructs(c)(pattern.tree)
    assertNoUnsupportedConstructs(c)(replacement.tree)
    c.universe.reify(new Replacement(expr(c)(pattern).splice, expr(c)(replacement).splice))
  }

  def when[T](c: Context)(pattern: c.Expr[T])(idents: c.Expr[Any]*)(thenMatch: c.Expr[PartialFunction[List[ru.Tree], Action[T]]])
  : c.Expr[ConditionalAction[T]] =
  {
    import c.universe._
    assertNoUnsupportedConstructs(c)(pattern.tree)

    val scalaCollection =
      Select(Ident(newTermName("scala")), newTermName("collection"))

    c.Expr[ConditionalAction[T]](
      New(
        Select(Ident(rootMirror.staticPackage("scalaxy.compilets")), newTypeName("ConditionalAction")),
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

  def replacement[T: c.WeakTypeTag](c: Context)(replacement: c.Expr[T]): c.Expr[ReplaceBy[T]] = {
    assertNoUnsupportedConstructs(c)(replacement.tree)
    c.universe.reify(new ReplaceBy[T](expr(c)(replacement).splice))
  }
}

