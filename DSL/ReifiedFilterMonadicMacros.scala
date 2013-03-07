package scalaxy.dsl

import scala.language.experimental.macros
import scala.reflect.macros.Context

import scala.collection.GenTraversableOnce
import scala.collection.generic.CanBuildFrom
import scala.collection.breakOut

import scala.reflect.runtime.{ universe => ru }
    
object ReifiedFilterMonadicMacros {
  def reifyFunction[A : c.WeakTypeTag, B : c.WeakTypeTag]
        (c: Context)
        (f: c.Expr[A => B])
      : c.Expr[ReifiedFunction[A, B]] = 
  {
    import c.universe._
    
    val tf = c.typeCheck(f.tree)
    
    var definedSymbols = Set[Symbol]()
    var referredSymbols = Set[Symbol]()
    new Traverser {
      override def traverse(tree: Tree) {
        val sym = tree.symbol
        if (sym != NoSymbol) {
          tree match {
            case _: DefTree => 
              definedSymbols += sym
            case Ident(_) => 
              referredSymbols += sym
            case _: RefTree => 
              c.warning(
                tree.pos,
                s"Maybe an unsupported reference type: $tree (${showRaw(tree)})")
            case _ =>
          }
        }
        super.traverse(tree)
      }
    }.traverse(tf)
    
    val capturedSymbols: Map[Symbol, String] =
      (
        for (capturedSymbol <- (referredSymbols -- definedSymbols)) yield {
          capturedSymbol -> c.fresh(capturedSymbol.name.toString)
        }
      ).toMap
    
    val ttf = c.Expr[A => B](
      new Transformer {
        object CapturedSymbol {
          def unapply(tree: Tree) = tree match {
            case Ident(_) =>
              capturedSymbols.get(tree.symbol).map(Some(_)).getOrElse(None)
            case _ =>
              None
          }
        }
        override def transform(tree: Tree): Tree = tree match {
          case CapturedSymbol(newName) =>
            Ident(newName: TermName)
          case _ => 
            super.transform(tree)
        }
      }.transform(tf)
    )
    
    
    val capturesExpr = c.Expr[Map[String, () => Any]](
      Apply(
        reify({ Map }).tree, 
        for ((capturedSymbol, newName) <- capturedSymbols.toList) yield {
          val s = c.literal(newName)
          val v = c.Expr[Any](Ident(capturedSymbol))
          reify((s.splice, () => v.splice)).tree 
        }
      )
    )
    
    val reifiedTree = c.Expr[ru.Expr[ru.Tree]](c.reifyTree(
      treeBuild.mkRuntimeUniverseRef,
      EmptyTree,
      ttf.tree
    ))
    
    reify({
      new ReifiedFunction(
        ttf.splice,
        capturesExpr.splice,
        reifiedTree.splice.tree.asInstanceOf[ru.Function]
      )
    })
  }
  
  
  def foreachImpl[A, Repr, U](c: Context)(f: c.Expr[A => U]): c.Expr[Unit] = 
  {
    import c.universe._
    val reifiedFunction = reifyFunction(c)(f)
    val colExpr = c.prefix.asInstanceOf[c.Expr[ReifiedFilterMonadic[A, Repr]]]
    reify({
      val col = colExpr.splice
      col.reifiedForeach(reifiedFunction.splice, col.reifiedFilters)
    })
  }
  
  def withFilterImpl[A, Repr](c: Context)(f: c.Expr[A => Boolean])
      : c.Expr[ReifiedFilterMonadic[A, Repr]] = 
  {
    import c.universe._
    val reifiedFunction = reifyFunction(c)(f)
    val colExpr = c.prefix.asInstanceOf[c.Expr[ReifiedFilterMonadic[A, Repr]]]
    reify({
      val col = colExpr.splice
      col.withFilters(col.reifiedFilters :+ reifiedFunction.splice): ReifiedFilterMonadic[A, Repr]
    })
  }
  
  def flatMapImpl[A, Repr, B, That]
        (c: Context)
        (f: c.Expr[A => GenTraversableOnce[B]])
        (bf: c.Expr[CanBuildFrom[Repr, B, That]])
      : c.Expr[That] =
  {
    import c.universe._
    val reifiedFunction = reifyFunction(c)(f)
    val colExpr = c.prefix.asInstanceOf[c.Expr[ReifiedFilterMonadic[A, Repr]]]
    reify({
      val col = colExpr.splice
      col.reifiedFlatMap(reifiedFunction.splice, col.reifiedFilters)(bf.splice)
    })
  }
}
