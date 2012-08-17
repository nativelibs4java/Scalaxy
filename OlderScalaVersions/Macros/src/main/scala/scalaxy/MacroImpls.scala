package scalaxy

//import language.experimental.macros

//package macros {

import scala.reflect._
import makro._

object MacroImpls 
{
  def tree(c: Context)(v: c.Expr[Any]): c.Expr[api.Exprs#Expr[Any]] = {
    c.Expr[api.Exprs#Expr[Any]](
      c.reifyTree(c.reflectMirrorPrefix, v.tree)
    )
  }
  
  def fail[T: c.TypeTag](c: Context)(message: c.Expr[String])(pattern: c.Expr[T]): c.Expr[MatchAction[T]] = {
    import c.mirror._
    c.Expr[MatchAction[T]](
      New(
        Select(Ident(newTermName("scalaxy")), newTypeName("MatchError")), 
        List(List(c.reifyTree(c.reflectMirrorPrefix, pattern.tree), message.tree))
      )
    )
  }
  
  def warn[T: c.TypeTag](c: Context)(message: c.Expr[String])(pattern: c.Expr[T]): c.Expr[MatchAction[T]] = {
    import c.mirror._
    c.Expr[MatchAction[T]](
      New(
        Select(Ident(newTermName("scalaxy")), newTypeName("MatchWarning")), 
        List(List(c.reifyTree(c.reflectMirrorPrefix, pattern.tree), message.tree))
      )
    )
  }
  
  def replace[T: c.TypeTag](c: Context)(pattern: c.Expr[T], replacement: c.Expr[T]): c.Expr[Replacement[T]] = {
    import c.mirror._
    c.Expr[Replacement[T]](
      New(
        Select(Ident(newTermName("scalaxy")), newTypeName("Replacement")), 
        List(List(c.reifyTree(c.reflectMirrorPrefix, pattern.tree), c.reifyTree(c.reflectMirrorPrefix, replacement.tree)))
      )
    )
  }
  
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
          c.reifyTree(c.reflectMirrorPrefix, pattern.tree), 
          Apply(
            Select(Select(scalaCollection, newTermName("Seq")), newTermName("apply")),
            idents.map(_.tree).toList.map { case Ident(n) => Literal(Constant(n.toString)) }
          ),
          thenMatch.tree
        ))
      )
    )
  }
    
  def replacement[T: c.TypeTag](c: Context)(replacement: c.Expr[T]): c.Expr[ReplaceBy[T]] = {
    import c.mirror._
    c.Expr[ReplaceBy[T]](
      New(
        Select(Ident(newTermName("scalaxy")), newTypeName("ReplaceBy")), 
        List(List(c.reifyTree(c.reflectMirrorPrefix, replacement.tree)))
      )
    )
  }
}
//}
