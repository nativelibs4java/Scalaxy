package scalaxy

import language.experimental.macros
                                         
import scala.reflect.macros.Context

//import scala.reflect.runtime._
import scala.reflect.runtime.{ universe => ru }

object MacroImpls
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
  private def enclosingTypeParams(c: Context): c.Expr[List[ru.Type]] = {
    import c.universe._
    c.enclosingMethod match {
      case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
        //println("YEYEYE ENCLOSING DefDef(" + name + ").tparams = " + tparams)
        val uref = c.universe.treeBuild.mkRuntimeUniverseRef
        
        newListTree[ru.Type](c)(
          tparams.map(tparam => {
            Select(
              // This actually creates a WeakTypeTag, so we need to call .tpe on it to get a Type.
              c.reifyType(
                uref,
                Select(uref, newTermName("rootMirror")), 
                tparam.symbol.asType.toType
              ),
              newTermName("tpe")
            )
          })
        )
        
      case d =>
        c.error(c.enclosingMethod.pos, "Failed to get enclosing method's type parameters list")
        null
    }  
  }
  
  private def expr[T](c: Context)(x: c.Expr[T]): c.Expr[ru.Expr[T]] = {
    var tree = //x.tree//
      c.typeCheck(x.tree)
    //assert(typeChecked.tpe != null, "Unable to typecheck " + x.tree)
    
    /*
    tree = new c.universe.Transformer {
      override def transform(tree: c.universe.Tree) = {
        //if (tree.tpe == null)
        //  println("NULL tree.tpe: " + tree)
        val s = super.transform(tree)
        try {
          c.typeCheck(s)
        } catch { case ex =>
          ex.printStackTrace
          s
        }
      }
    }.transform(tree)
    */
        
    c.Expr[ru.Expr[T]](
      c.reifyTree(
        c.universe.treeBuild.mkRuntimeUniverseRef,
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
  
  def replace[T](c: Context)(pattern: c.Expr[T], replacement: c.Expr[T]): c.Expr[Replacement] = {
    import c.universe._
    //c.universe.reify(new Replacement(expr(c)(pattern).splice, expr(c)(replacement).splice, enclosingTypeParams(c).splice:_*))
    
    c.Expr[Replacement](
      New(
        Select(Ident(newTermName("scalaxy")), newTypeName("Replacement")), 
        List(List(
          tree(c)(pattern),
          tree(c)(replacement),
          enclosingTypeParams(c).tree
        ))// ++ enclosingTypeParams(c).map(_.tree))
      )
    )
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
          thenMatch.tree,
          enclosingTypeParams(c).tree
        ))// ++ enclosingTypeParams(c).map(_.tree))
      )
    )
  }
    
  def replacement[T: c.WeakTypeTag](c: Context)(replacement: c.Expr[T]): c.Expr[ReplaceBy[T]] = 
    c.universe.reify(new ReplaceBy[T](expr(c)(replacement).splice))
}


  /*
  
  //val uref = c.typeCheck(Select(Select(Select(Ident(newTermName("scala")).setSymbol(definitions.ScalaPackage), newTermName("reflect")), newTermName("runtime")), newTermName("package")))
          
          //
          //  c.reifyType(
          //    c.universe.treeBuild.mkRuntimeUniverseRef,
          //    Select(c.universe.treeBuild.mkRuntimeUniverseRef, newTermName("rootMirror")), 
          //    tparam.symbol.asType.toType
          //  )
          //)
  private def enclosingTypeParams(c: Context): c.Expr[List[ru.Expr[ru.TypeTree]]] = {
    //c.universe.reify(List[Any]())
    import c.universe._
    c.enclosingMethod match {
      case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
        println("YEYEYE ENCLOSING DefDef(" + name + ").tparams = " + tparams)
        val ret = c.Expr[List[ru.Expr[ru.TypeTree]]](
          Apply(
            TypeApply(
              Select(
                Select(
                  Ident(newTermName("scala")).setSymbol(definitions.ScalaPackage),
                  newTermName("List")
                ), 
                newTermName("apply")
              ),
              List(TypeTree(typeOf[ru.TypeTree]))
            ),
            tparams.map(tparam => {
              //c.reifyType(
              //  c.runtimeUniverse,
              //  EmptyTree, 
              //  tparam.symbol.asType.toType
              //)
              //c.Expr[ru.TypeTree](
                c.reifyTree(
                  c.runtimeUniverse,
                  EmptyTree, 
                  TypeTree(tparam.symbol.asType.toType)
                )
              //)
            })
          )
        )
        println(ret)
        ret
      case d =>
        c.error(c.enclosingMethod.pos, "Failed to get enclosing method's type parameters list")
        null
    }  
  }*/
