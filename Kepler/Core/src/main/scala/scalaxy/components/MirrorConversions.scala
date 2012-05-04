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
  
  def importName(from: api.Universe, to: api.Universe)(n: from.Name): to.Name =
    n match { 
      case _: from.TermName =>
        to.newTermName(n.toString)
      case _: from.TypeName =>
        to.newTypeName(n.toString)
    }
    
  def newMirrorToGlobalImporter(bindings: Bindings) = {
    new global.StandardImporter {
      val from = mirror.asInstanceOf[scala.reflect.internal.SymbolTable]
      
      override def importTree(tree: from.Tree) = {
        tree match {
          case from.Ident(n) =>
            val in = importName(n)
              //MirrorConversions.this.importName(mirror, global)(n.asInstanceOf[mirror.Name])
            bindings.nameBindings.get(in).getOrElse {
              super.importTree(tree)
              //val imp = global.Ident(in)
              //imp.tpe = importType(tree.tpe)
              //imp
            }
          case _ =>
            try {
              super.importTree(tree)
            } catch { case ex => 
              println("FAILED with " + tree + "\n\t" + ex)
              throw ex
            }
        }
      }
      override def importType(tpe: from.Type): global.Type = {
        if (tpe == null) {
          null
        } else {
          val it = resolveType(super.importType(tpe))
          //val it = super.importType(tpe)
          bindings.getType(it).getOrElse(it)
        }
      }
    }
  }
  def newGlobalToMirrorImporter = {
    val mm = mirror.asInstanceOf[scala.reflect.internal.SymbolTable]
    new mm.StandardImporter {
      val from = global
      //val from = global.asInstanceOf[scala.reflect.internal.SymbolTable]
      /*
      override def importTree(tree: from.Tree): mm.Tree = {
        tree match {
          case from.Ident(n) =>
            val in = importName(n)
            val imp = mm.Ident(in)
            imp.tpe = importType(tree.tpe)
            imp
          case _ =>
            super.importTree(tree)
        }
      }*/
    }
  }
  
  /**
   * TODO report missing API : scala.reflect.api.SymbolTable 
   * (scala.reflect.mirror does not extend scala.reflect.internal.SymbolTable publicly !)
   */
  def mirrorToGlobal(m: mirror.Tree, bindings: Bindings): global.Tree = {
    val importer = newMirrorToGlobalImporter(bindings)
    /*
    new mirror.Traverser {
      override def traverse(t: mirror.Tree) = {
        val tpe = t.tpe
        if (tpe == null || tpe == mirror.NoType) {
          val sym = t.symbol
          if (sym != null && sym != mirror.NoSymbol) {
            t.tpe = sym.asType
          }
        }
        super.traverse(t)
      }
    }.traverse(m)
    */
    importer.importTree(m.asInstanceOf[importer.from.Tree])
  }
  
  implicit def mirrorToGlobal(m: mirror.Name, bindings: Bindings): global.Name = { 
    //val importer = newMirrorToGlobalImporter(bindings) 
    //importer.
    importName(mirror, global)(m)//.asInstanceOf[importer.from.Name])
  }
  
  def globalToMirror(t: global.Name): mirror.Name = {
    //val importer = newGlobalToMirrorImporter
    //importer.
    importName(global, mirror)(t)//.asInstanceOf[importer.from.Name])//.asInstanceOf[mirror.Name]
  }
  
  def globalToMirror(t: global.Tree): mirror.Tree = {
    val importer = newGlobalToMirrorImporter
    importer.importTree(t.asInstanceOf[importer.from.Tree]).asInstanceOf[mirror.Tree]
  }
  
  /*
  def mirrorNodeToString(tree: mirror.Tree) = {
    new mirror.Traverser {
      var indent = 0
      def ptind =
        for (i <- 0 until indent)
          print("\t")
      override def traverse(t: mirror.Tree) = {
        ptind
        //println(t.getClass.getSimpleName + " // tpe = " + t.tpe + ", sym = " + t.symbol + ", sym.tpe = " + (if (Option(t.symbol).getOrElse(NoSymbol) == NoSymbol) "?" else t.symbol.asType))
        indent = indent + 1
        super.traverse(t)
        indent = indent - 1
      }
    }.traverse(tree)
  }
  */
}
