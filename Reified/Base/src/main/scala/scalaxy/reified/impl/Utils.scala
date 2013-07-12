package scalaxy.reified.impl

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

import scalaxy.reified.base.ReifiedValue

object Utils {

  private[reified] def newExpr[A](tree: Tree): Expr[A] = {
    Expr[A](
      currentMirror,
      CurrentMirrorTreeCreator(tree))
  }

  def typeCheck[A](expr: Expr[A]): Expr[A] = {
    newExpr[A](typeCheck(expr.tree))
  }

  def createReifiedValue_[A](
    value: A,
    taggedExpr: Expr[A],
    capturedTerms: Seq[(AnyRef, Type)],
    paramTypeTags: Map[String, TypeTag[_]]): ReifiedValue[A] = {
    //println("RESOLVING " + expr)

    def transformSymbol(sym: Symbol): Symbol = {
      if (sym == null || sym == NoSymbol) {
        sym
      } else {
        for (s <- sym) yield {
          if (s.isType) {
            transformType(s.asType.toType).typeSymbol
          } else {
            s
          }
        }
      }
    }

    def transformType(tpe: Type): Type = {
      if (tpe == null || tpe == NoType) {
        //println("no tpe")
        tpe
      } else {
        val sym = tpe.typeSymbol
        if (sym != NoSymbol) {
          val tsym = sym.asType
          //println("FOUND TYPE TREE " + tsym)
          if (tsym.isFreeType || tsym.isParameter) {
            println("FOUND PARAM " + tsym)

            paramTypeTags.get(tsym.name.toString) match {
              case Some(ttag) =>
                ttag.tpe
              case None =>
                tpe
            }
          } else {
            tpe
          }
        } else {
          tpe
        }
      }
    }
    val typeTagsTransformer = new Transformer {
      override def transform(tree: Tree): Tree = {
        //println("Transforming " + tree)

        tree match {
          case TypeTree() =>
            val s = tree.toString
            if (paramTypeTags.contains(s)) {
              println("TYPE TREE " + tree)
            }
            //println(tree)
            TypeTree(transformType(tree.tpe))
          case _ =>
            val trans = transformType(tree.tpe)
            val transSym = transformSymbol(tree.symbol)
            //println(s"[$tree]\n\t${tree.tpe} -> $trans\n\t${tree.symbol} -> $transSym")
            super.transform(tree)
        }
      }
    }
    val transformedTaggedExpr = newExpr[A](typeTagsTransformer.transform(typeCheck(taggedExpr.tree)))
    println(s"transformedTaggedExpr = $transformedTaggedExpr")
    ReifiedValue[A](
      value,
      transformedTaggedExpr,
      capturedTerms.map { case (v, tpe) => (v, transformType(tpe)) }
    )
  }

  private[reified] val toolbox = currentMirror.mkToolBox()

  private[reified] def getModulePath(u: scala.reflect.api.Universe)(moduleSym: u.ModuleSymbol): u.Tree = {
    import u._
    val elements = moduleSym.fullName.split("\\.").toList
    def rec(root: Tree, sub: List[String]): Tree = sub match {
      case Nil => root
      case name :: rest => rec(Select(root, name: TermName), rest)
    }
    rec(Ident(elements.head: TermName), elements.tail)
  }

  private[reified] def resolveModulePaths(u: scala.reflect.api.Universe)(root: u.Tree): u.Tree = {
    import u._
    new Transformer {
      override def transform(tree: Tree) = tree match {
        case Ident() if tree.symbol != null && tree.symbol.isModule =>
          println("REPLACING " + tree + " BY MODULE PATH")
          getModulePath(u)(tree.symbol.asModule)
        case _ =>
          super.transform(tree)
      }
    }.transform(root)
  }

  def replaceTypes(tree: Tree, replacements: Map[String, TypeTag[_]]): Tree = {
    val transformer = new Transformer {
      override def transform(tree: Tree): Tree = {
        super.transform(tree)
      }
    }
    transformer.transform(tree)
  }

  def typeCheck(tree: Tree, pt: Type = WildcardType): Tree = {
    // TODO reuse toolbox if costly to create and if doesn't take too much memory. 
    val ttree = tree.asInstanceOf[toolbox.u.Tree]
    if (ttree.tpe != null && ttree.tpe != NoType)
      tree
    else {
      try {
        toolbox.typeCheck(
          ttree,
          pt.asInstanceOf[toolbox.u.Type])
      } catch {
        case ex: Throwable =>
          throw new RuntimeException(s"Failed to typeCheck($tree, $pt): $ex", ex)
      }
    }.asInstanceOf[Tree]
  }
}
