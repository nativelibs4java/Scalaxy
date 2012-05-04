package scalaxy

//import language.experimental.macros

//package macros {

import scala.reflect._
import makro._

object MacroImpls 
{
  /*
  def tree(c: Context)(v: c.Expr[Any]): c.Expr[api.Exprs#Expr[Any]] = {
    c.Expr[api.Exprs#Expr[Any]](
      c.reifyTree(c.reflectMirrorPrefix, v.tree)
    )
  }
  */
  
  def fail(c: Context)(message: c.Expr[String])(pattern: c.Expr[Any]): c.Expr[MatchError] = {
    import c.mirror._
    c.Expr[MatchError](
      New(
        Select(Ident(newTermName("scalaxy")), newTypeName("MatchError")), 
        List(List(c.reifyTree(c.reflectMirrorPrefix, pattern.tree), message.tree))
      )
    )
  }
  
  def warn(c: Context)(message: c.Expr[String])(pattern: c.Expr[Any]): c.Expr[MatchWarning] = {
    import c.mirror._
    c.Expr[MatchWarning](
      New(
        Select(Ident(newTermName("scalaxy")), newTypeName("MatchWarning")), 
        List(List(c.reifyTree(c.reflectMirrorPrefix, pattern.tree), message.tree))
      )
    )
  }
  
  def replace[T: c.TypeTag](c: Context)(pattern: c.Expr[T], replacement: c.Expr[T]): c.Expr[Replacement/*[T]*/] = {
    //import c.mirror._
    
    //val pat = c.reify(pattern.tree).asInstanceOf[Expr[mirror.Expr[T]]]
    //val rep = c.reify(replacement.tree).asInstanceOf[Expr[mirror.Expr[T]]]
    
    //if (false)
    /*
    {
      //val pat = tb.typeCheck(pattern.tree, pattern.tpe, silent = false)
      //val rep = tb.typeCheck(replacement.tree, replacement.tpe, silent = false)
      val tb = c.mirror.mkToolBox()
      //mirror.asInstanceOf[scala.reflect.api.Universe].phase = null
      
      //phaseOf(infos.validFrom)
      def typeTree[T](x: Expr[T]) = {
        //c.mirror.typer.typed(x.tree)
        //tb.asInstanceOf[scala.reflect.api.ToolBoxes#ToolBox].typeCheckExpr(pattern.tree, pattern.tpe)
        try {
          tb.typeCheck(x.tree, x.tpe)
        } catch { case ex =>
          println("Failed to typeCheck " + x + " : " + ex)
          throw ex
        }
        //val pat = c.mirror.asInstanceOf[scala.reflect.runtime.ToolBoxes].compiler.typeCheckExpr(pattern.tree, pattern.tpe)
      }
      c.Expr[Replacement[T]](
        New(
          Select(Ident(newTermName("scalaxy")), newTypeName("Replacement")), 
          List(
            List(
              c.reifyTree(c.reflectMirrorPrefix, typeTree(pattern)), 
              c.reifyTree(c.reflectMirrorPrefix, typeTree(replacement))
              //c.reifyTree(c.reflectMirrorPrefix, pattern.tree), 
              //c.reifyTree(c.reflectMirrorPrefix, replacement.tree)
            )
          )
        )
      )
    }*/
    {
      val pat = c.mirror.Expr[mirror.Expr[Any]](c.reifyTree(c.reflectMirrorPrefix, pattern.tree))
      val rep = c.mirror.Expr[mirror.Expr[Any]](c.reifyTree(c.reflectMirrorPrefix, replacement.tree))
    
      //c.reify(new Replacement(Utils.typed(pat.eval), Utils.typed(rep.eval)))
      c.reify(new Replacement(pat.eval, rep.eval))
    } 
    /*
    else if (false) 
    {
      c.Expr[Replacement[T]](
        New(
          Select(Ident(newTermName("scalaxy")), newTypeName("Replacement")), 
          List(List(c.reifyTree(c.reflectMirrorPrefix, pattern.tree), c.reifyTree(c.reflectMirrorPrefix, replacement.tree)))
        )
      )
    } 
    else 
    {
      c.Expr[Replacement[T]](
        Apply(
          TypeApply(
            Select(Select(Ident(newTermName("scalaxy")), newTermName("Replacement")), newTermName("apply")),
            List(TypeTree(implicitly[c.TypeTag[T]].tpe))
          ),
          List(
            c.reifyTree(c.reflectMirrorPrefix, pattern.tree), 
            c.reifyTree(c.reflectMirrorPrefix, replacement.tree)
          )
        )
      )
    }*/
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
