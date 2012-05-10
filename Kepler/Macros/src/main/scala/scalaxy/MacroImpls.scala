package scalaxy

//import language.experimental.macros

import scala.reflect._
import makro.Context

object MacroImpls 
{
  private def expr[T](c: Context)(x: c.Expr[T]): c.Expr[mirror.Expr[T]] = {
    val tree = x.tree//c.typeCheck(x.tree)
    //assert(typeChecked.tpe != null, "Unable to typecheck " + x.tree)
    c.mirror.Expr[mirror.Expr[T]](
      c.reifyTree(
        c.reflectMirrorPrefix, 
        tree
      )
    )
  }
  private def tree(c: Context)(x: c.Expr[Any]): c.mirror.Tree = 
    expr[Any](c)(x).tree
  
  def fail(c: Context)(message: c.Expr[String])(pattern: c.Expr[Any]): c.Expr[MatchError] = 
    c.reify(new MatchError(expr(c)(pattern).eval, message.eval))
  
  def warn(c: Context)(message: c.Expr[String])(pattern: c.Expr[Any]): c.Expr[MatchWarning] = 
    c.reify(new MatchWarning(expr(c)(pattern).eval, message.eval))
  
  def replace[T: c.TypeTag](c: Context)(pattern: c.Expr[T], replacement: c.Expr[T]): c.Expr[Replacement] =
    c.reify(new Replacement(expr(c)(pattern).eval, expr(c)(replacement).eval))
  
  def when[T: c.TypeTag](c: Context)(pattern: c.Expr[T])(idents: c.Expr[Any]*)(thenMatch: c.Expr[PartialFunction[List[mirror.Tree], Action[T]]])
  : c.Expr[ConditionalAction[T]] = 
  {
    import c.mirror._
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
    
  def replacement[T: c.TypeTag](c: Context)(replacement: c.Expr[T]): c.Expr[ReplaceBy[T]] = 
    c.reify(new ReplaceBy[T](expr(c)(replacement).eval))
}
