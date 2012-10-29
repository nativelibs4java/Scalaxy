package scalaxy; package components

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.symtab.Flags._

import scala.reflect._
import Function.tupled

trait MirrorConversions
extends PatternMatchers
{
  this: PluginComponent =>

  import global.definitions._

  private def ultraLogConversions(txt: => String) {
    //println(txt)
  }

  private def tryAndType(tree: global.Tree) = {
    try {
      global.typer.typed { tree }
    } catch { case _: Throwable => }
    tree
  }

  /**
   * TODO report missing API : scala.reflect.api.SymbolTable
   * (scala.reflect.mirror does not extend scala.reflect.internal.SymbolTable publicly !)
   */
  def newMirrorToGlobalImporter(mirror: api.Universe)(bindings: Bindings) = {
    new global.StandardImporter {
      val from = mirror.asInstanceOf[scala.reflect.internal.SymbolTable]

      override def importTree(tree: from.Tree) = {
        ultraLogConversions("IMPORT TREE (tpe = " + tree.tpe + ", cls = " + tree.getClass.getName + "): " + tree)
        val imp = tree match {
          case from.Ident(n) =>
            bindings.nameBindings.get(n.toString).getOrElse(super.importTree(tree)).asInstanceOf[global.Tree]

          case _ =>
            super.importTree(tree)
        }
        ultraLogConversions("-> TREE " + imp)

        imp
      }
      override def importType(tpe: from.Type): global.Type = {
        ultraLogConversions("IMPORT TYPE " + tpe)
        val imp = if (tpe == null) {
          null
        } else {
          val rtpe = resolveType(from)(tpe)
          val it = resolveType(global)(super.importType(rtpe))
          //TODO?
          //bindings.getType(it.asInstanceOf[patternUniv.Type]).getOrElse(it).asInstanceOf[global.Type]
          bindings.getType(rtpe.asInstanceOf[patternUniv.Type]).getOrElse(it).asInstanceOf[global.Type]
        }
        ultraLogConversions("-> TYPE " + imp)
        imp
      }
    }
  }

  def newGlobalToMirrorImporter(mirror: api.Universe) = {
    val mm = mirror.asInstanceOf[scala.reflect.internal.SymbolTable]
    new mm.StandardImporter {
      val from = global
    }
  }

  def mirrorToGlobal(mirror: api.Universe)(m: mirror.Tree, bindings: Bindings): global.Tree = {
    val importer =
      newMirrorToGlobalImporter(mirror)(bindings)
    try {
      importer.importTree(m.asInstanceOf[importer.from.Tree])
    } catch { case ex: Throwable =>
      ultraLogConversions("FAILED importer.importTree(" + m + "):\n\t" + ex)
      throw ex
    }
  }

  def globalToMirror(mirror: api.Universe)(t: global.Tree): mirror.Tree = {
    val importer = newGlobalToMirrorImporter(mirror)
    importer.importTree(t.asInstanceOf[importer.from.Tree]).asInstanceOf[mirror.Tree]
  }
}
