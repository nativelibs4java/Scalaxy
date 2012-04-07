package scalaxy; package components

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.ast.TreeDSL
import scala.tools.nsc.transform.TypingTransformers
import Function.tupled

trait MirrorConversions
extends Replacements
//extends TypingTransformers
{
  this: PluginComponent =>

  import global.definitions._
  import scala.tools.nsc.symtab.Flags._

  import scala.reflect._
  /*
  implicit def mirrorToGlobal(n: mirror.Name): global.Name = { 
    if (n.isTermName)
      mirrorToGlobal(n.asInstanceOf[mirror.TermName])
    else
      mirrorToGlobal(n.asInstanceOf[mirror.TypeName])
  }
  implicit def mirrorToGlobal(n: mirror.TermName): global.TermName =
    global.newTermName(n.toString)
    
  implicit def mirrorToGlobal(n: mirror.TypeName): global.TypeName =
    global.newTypeName(n.toString)
    
  implicit def mirrorToGlobal(sym: mirror.Symbol): global.Symbol = {
    assert(sym == mirror.NoSymbol)
    global.NoSymbol
  }
  import mirror.{ definitions => mdefs }
  import global.{ definitions => gdefs }
  lazy val commonConversions = Map[mirror.Type, global.Type](
    mdefs.IntClass.asType -> gdefs.IntClass.tpe,
    mdefs.ShortClass.asType -> gdefs.ShortClass.tpe,
    mdefs.ByteClass.asType -> gdefs.ByteClass.tpe,
    mdefs.CharClass.asType -> gdefs.CharClass.tpe,
    mdefs.LongClass.asType -> gdefs.LongClass.tpe,
    mdefs.BooleanClass.asType -> gdefs.BooleanClass.tpe,
    mdefs.UnitClass.asType -> gdefs.UnitClass.tpe,
    mdefs.DoubleClass.asType -> gdefs.DoubleClass.tpe,
    mdefs.FloatClass.asType -> gdefs.FloatClass.tpe
  )
  implicit def mirrorToGlobal(m: mirror.Type): global.Type = m match {
    case mirror.NoType =>
      global.NoType
    case mirror.PolyType(syms, tpe) =>
      global.PolyType(syms.map(mirrorToGlobal _), mirrorToGlobal(tpe))
    case mirror.ThisType(sym) =>
      global.ThisType(mirrorToGlobal(sym))
    //case mirror.NoArgsTypeRef
    case mirror.TypeRef(t, sym, targs) =>
      println("FOUND TypeRef " + m)
      global.TypeRef(mirrorToGlobal(t), mirrorToGlobal(sym), targs.map(mirrorToGlobal _))
    case _ =>
      commonConversions.get(m).getOrElse {
        try {
          val s = m.toString
          global.definitions.getClass(global.newTypeName(s)).tpe
        } catch { case ex =>
          ex.printStackTrace
          throw new UnsupportedOperationException("Cannot convert type of type " + m.getClass.getName + " (extends " + m.getClass.getSuperclass.getName + ") : " + m)
        }
      }
  }
  implicit def mirrorToGlobal(l: List[mirror.Tree]): List[global.Tree] = 
    l.map(m => m: global.Tree)
    */
  import scala.tools.nsc.symtab.Flags._
  
  
  def newImporter(bindings: Bindings) = {
    new global.Importer {
      //val from: self.type = mirror//self
      //val f: scala.reflect.internal.SymbolTable = mirror: scala.reflect.internal.SymbolTable
      val from = mirror.asInstanceOf[scala.reflect.internal.SymbolTable]
      
      override def importTree(tree: from.Tree): global.Tree = {
        tree match {
          case from.Ident(n) =>
            val in = importName(n)
            bindings.nameBindings.get(in).getOrElse {
              //println("No name binding for " + n + ", so using conversion " + in)
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
          bindings.getType(it).getOrElse {
            //println("Imported " + tpe + " as " + it)
            //println("No type binding for " + tpe + ", so using conversion " + it + " (bindings = " + bindings + ")")
            it
          }
        }
      }
      override def importSymbol(sym: from.Symbol) = {
        val is = super.importSymbol(sym)
        try {
          if (sym != from.NoSymbol && sym.info != from.NoType && sym.info.decls != from.EmptyScope && (is.info == global.NoType || is.info.decls == global.EmptyScope))
            println("Converted " + sym + " with info " + sym.info + " and decls = " + sym.info.decls + " to " + is + " with info " + is.info + " with empty scope")
        } catch { case _ => }
        is
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
  implicit def mirrorToGlobal(m: mirror.Tree): global.Tree = m match {
    case mirror.If(c, t, e) =>
      global.If(c, t, e)
    case mirror.LabelDef(lab, idents, v) =>
      global.LabelDef(lab, idents.map(i => mirrorToGlobal(i).asInstanceOf[global.Ident]), v)
    case mirror.TypeTree() =>
      global.TypeTree(m.tpe)
    case mirror.Block(l, v) =>
      global.Block(l, v)
    case mirror.Ident(n) =>
      global.Ident(n)
    case mirror.Select(a, n) =>
      global.Select(a, n)
    case mirror.Apply(t, args) =>
      global.Apply(t, args)
    case mirror.Assign(l, r) =>
      global.Assign(l, r)
    case mirror.Literal(mirror.Constant(c)) =>
      global.Literal(global.Constant(c))
    case mirror.ValDef(mods, name, tpt, rhs) =>
      global.ValDef(global.Modifiers(mods.modifiers), name, tpt, rhs) //MUTABLE
    case _ =>
      throw new UnsupportedOperationException("Cannot convert mirror tree of type " + m.getClass.getName + " (" + m + ") to global type yet")
  }
  */
    
}
