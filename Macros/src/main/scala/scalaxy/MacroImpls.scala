package scalaxy

import language.experimental.macros
                                         
import scala.reflect.macros.Context

import scala.reflect.runtime._
import scala.reflect.runtime.universe._

object MacroImpls
{
  
  private def expr[T](c: Context)(x: c.Expr[T]): c.Expr[Expr[T]] = {
    val tree = //x.tree//
      c.typeCheck(x.tree)
    //assert(typeChecked.tpe != null, "Unable to typecheck " + x.tree)
    
    //new c.universe.Traverser {
    //  override def traverse(tree: c.universe.Tree) = {
    //    if (tree.tpe == null)
    //      println("NULL tree.tpe: " + tree)
    //    c.typeCheck(tree)
    //    super.traverse(tree)
    //  }
    //}.traverse(tree)
    
    c.Expr[Expr[T]](
      c.reifyTree(
        c.runtimeUniverse,
        c.universe.EmptyTree, //c.reflectMirrorPrefix, 
        tree
      )
    )
  }
  private def tree(c: Context)(x: c.Expr[Any]): c.universe.Tree = 
    expr[Any](c)(x).tree
  
  def fail(c: Context)(message: c.Expr[String])(pattern: c.Expr[Any]): c.Expr[MatchError] = 
    c.universe.reify(new MatchError(expr(c)(pattern).splice, message.splice))
  
  def warn(c: Context)(message: c.Expr[String])(pattern: c.Expr[Any]): c.Expr[MatchWarning] = 
    c.universe.reify(new MatchWarning(expr(c)(pattern).splice, message.splice))
  
  def replace[T](c: Context)(pattern: c.Expr[T], replacement: c.Expr[T]): c.Expr[Replacement] =
    c.universe.reify(new Replacement(expr(c)(pattern).splice, expr(c)(replacement).splice))
  
  def when[T](c: Context)(pattern: c.Expr[T])(idents: c.Expr[Any]*)(thenMatch: c.Expr[PartialFunction[List[Tree], Action[T]]])
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
    
  def replacement[T: c.AbsTypeTag](c: Context)(replacement: c.Expr[T]): c.Expr[ReplaceBy[T]] = 
    c.universe.reify(new ReplaceBy[T](expr(c)(replacement).splice))
}
