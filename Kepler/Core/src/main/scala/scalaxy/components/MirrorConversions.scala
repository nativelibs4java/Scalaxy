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
  
  /**
   * TODO report missing API : scala.reflect.api.SymbolTable 
   * (scala.reflect.mirror does not extend scala.reflect.internal.SymbolTable publicly !)
   */
  def newMirrorToGlobalImporter(mirror: base.Universe)(bindings: Bindings) = {
    new global.StandardImporter {
      val from = mirror.asInstanceOf[scala.reflect.internal.SymbolTable]
      
      override def importTree(tree: from.Tree) = tree match {
        case from.Ident(n) =>
          //val in = importName(n).asInstanceOf[patternUniv.Name]
          bindings.nameBindings.get(n.toString).getOrElse(super.importTree(tree)).asInstanceOf[global.Tree]
        case _ =>
          super.importTree(tree)
      }
      override def importType(tpe: from.Type): global.Type = {
        if (tpe == null)
          null
        else {
          val it = resolveType(global)(super.importType(tpe))
          bindings.getType(it.asInstanceOf[patternUniv.Type]).getOrElse(it).asInstanceOf[global.Type]
        }
      }
    }
  }
  
  def newGlobalToMirrorImporter(mirror: base.Universe) = {
    val mm = mirror.asInstanceOf[scala.reflect.internal.SymbolTable]
    new mm.StandardImporter {
      val from = global//.asInstanceOf[scala.reflect.internal.SymbolTable]
      /*override def importTree(tree: from.Tree): mm.Tree = tree match {
        case from.Ident(n) =>
          val imp = mm.Ident(importName(n)) ; imp.tpe = importType(tree.tpe) ; imp
        case _ =>
          super.importTree(tree)
      }*/
    }
  }
  
  //@deprecated("dying code")
  //private def fixMissingMirrorTypes(m: mirror.Tree) = {
  //  new mirror.Traverser {
  //    override def traverse(t: mirror.Tree) = {
  //      val tpe = t.tpe
  //      if (tpe == null || tpe == mirror.NoType) {
  //        val sym = t.symbol
  //        if (sym != null && sym != mirror.NoSymbol) {
  //          t.tpe = sym.asType
  //        }
  //      }
  //      super.traverse(t)
  //    }
  //  }.traverse(m)
  //}
  
  /**
    scala.reflect.mirror
    scala.reflect.runtime.universe
  */
  def mirrorToGlobal(mirror: base.Universe)(m: mirror.Tree, bindings: Bindings): global.Tree = {
    val importer =
      newMirrorToGlobalImporter(mirror)(bindings)
    //fixMissingMirrorTypes(m)
    try {
      importer.importTree(m.asInstanceOf[importer.from.Tree])
    } catch { case ex => 
      println("FAILED importer.importTree(" + m + "):\n\t" + ex)
      throw ex
    }
  }
  
  def globalToMirror(mirror: base.Universe)(t: global.Tree): mirror.Tree = {
    val importer = newGlobalToMirrorImporter(mirror)
    importer.importTree(t.asInstanceOf[importer.from.Tree]).asInstanceOf[mirror.Tree]
  }
  
  /*
  def importName(from: api.Universe, to: api.Universe)(n: from.Name): to.Name =
    n match { 
      case _: from.TermName =>
        to.newTermName(n.toString)
      case _: from.TypeName =>
        to.newTypeName(n.toString)
    }
    
  def globalToMirror(t: global.Name): mirror.Name = {
    importName(global, mirror)(t)
  }
  */
}
