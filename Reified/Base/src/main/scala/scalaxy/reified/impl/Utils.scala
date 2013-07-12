package scalaxy.reified.impl

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

  def createReifiedValue[A](
    value: A,
    taggedExpr: Expr[A],
    capturedTerms: Seq[(AnyRef, Type)],
    paramTypeTags: Map[String, TypeTag[_]]): ReifiedValue[A] = {
    //println("RESOLVING " + expr)

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
            super.transform(tree)
        }
      }
    }
    ReifiedValue[A](
      value,
      newExpr[A](typeTagsTransformer.transform(typeCheck(taggedExpr.tree))),
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
