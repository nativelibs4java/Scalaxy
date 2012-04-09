package scalaxy; package components

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.symtab.Flags._

import scala.reflect._
import Function.tupled

trait MirrorConversions
extends Replacements
{
  this: PluginComponent =>

  import global.definitions._
  
  def newImporter(bindings: Bindings) = {
    new global.Importer {
      val from = mirror.asInstanceOf[scala.reflect.internal.SymbolTable]
      
      override def importTree(tree: from.Tree): global.Tree = {
        tree match {
          case from.Ident(n) =>
            val in = importName(n)
            bindings.nameBindings.get(in).map(t => {
              //println("Replaced tree " + tree + " by " + t)
              t
            }).getOrElse {
              val imp = global.Ident(in)
              imp.tpe = importType(tree.tpe)
              imp
            }
          case _ =>
            super.importTree(tree)
        }
      }
      override def importType(tpe: from.Type): global.Type = {
        if (tpe == null) {
          null
        } else {
          val it = resolveType(super.importType(tpe))
          bindings.getType(it).map(t => {
            //println("Replaced type " + tpe + " by " + t)
            t
          }).getOrElse(it)
        }
      }
    }
  }
  
  /**
   * TODO report missing API : scala.reflect.api.SymbolTable 
   * (scala.reflect.mirror does not extend scala.reflect.internal.SymbolTable publicly !)
   */
  def mirrorToGlobal(m: mirror.Tree, bindings: Bindings): global.Tree = {
    val importer = newImporter(bindings) 
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
    importer.importTree(m.asInstanceOf[importer.from.Tree])
  }
  
  implicit def mirrorToGlobal(m: mirror.Name, bindings: Bindings): global.Name = { 
    val importer = newImporter(bindings) 
    importer.importName(m.asInstanceOf[importer.from.Name])
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
