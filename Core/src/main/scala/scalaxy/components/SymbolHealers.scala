package scalaxy ; package plugin
//import common._
import pluginBase._
import components._

import scala.tools.nsc.Global

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.typechecker.Analyzer
import scala.tools.nsc.typechecker.Contexts
import scala.tools.nsc.typechecker.Modes
import scala.Predef._

trait SymbolHealers
extends TypingTransformers
   with Modes
{
  this: PluginComponent =>

  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._

  /*
  def eraseSymbols(tree: Tree): Tree = {
    new Traverser {
      override def traverse(tree: Tree) = {
        super.traverse(tree)
        tree match {
          case (_: TypeTree) | (_: Block) =>
          case _ if tree.isEmpty =>
          //case Literal(_) =>
          case _ =>
            tree.setSymbol(NoSymbol)//.setType(NoType)
            //println("Reset symbol of " + tree + " : " + tree.symbol + " : " + nodeToString(tree))
        }
      }
    }.traverse(tree)
    tree
  }
  
  def eraseTypes(tree: Tree): Tree = {
    new Traverser {
      override def traverse(tree: Tree) = {
        super.traverse(tree)
        try {
          if (!tree.isEmpty)
            tree.setType(null)
        } catch { case ex => 
          ex.printStackTrace
        }
      }
    }.traverse(tree)
    tree
  }
  */
  def healSymbols(unit: CompilationUnit, rootOwner: Symbol, root: Tree, expectedTpe: Type): Tree = {
    new Traverser {
      var syms = new collection.mutable.HashMap[Name, Symbol]()
      currentOwner = rootOwner
      
      /*def enterToCurrentScope(sym: Symbol) = {
        var scope = currentOwner.info.decls
        if (scope == EmptyScope)
          println("ERROR: Empty scope for currentOwner " + currentOwner + " (trying to enter symbol " + sym + ")")
        else
          scope enter sym
      }*/
      def subSyms[V](v: => V): V = {
        val oldSyms = syms
        syms = new collection.mutable.HashMap[Name, Symbol]()
        syms ++= oldSyms
        try {
          v
        } finally {
          syms = oldSyms
        }
      }
      override def traverse(tree: Tree) = {
        try {
          /*try {
            typer.typed(tree)//, EXPRmode, NoType)
          } catch { case ex =>
            //println("FAILED to type " + tree + " in healSymbols") 
          }*/
          def traverseValDef(vd: ValDef) = {
            val ValDef(mods, name, tpt, rhs) = vd
            val sym = (
              if (mods.hasFlag(MUTABLE))
                currentOwner.newVariable(name)
              else
                currentOwner.newValue(name)
            ).setFlag(mods.flags)
            
            tree.setSymbol(sym)
            
            syms(name) = sym
            //enterToCurrentScope(sym)
    
            atOwner(sym) {
              traverse(tpt)
              traverse(rhs)
            }
            
            typer.typed(rhs)
            
            var tpe = rhs.tpe
            if (tpe.isInstanceOf[ConstantType])
              tpe = tpe.widen
              
            sym.setInfo(Option(tpe).getOrElse(NoType))
            //setSymbolAndEnter(tree, currentOwner, sym)
          }
          
          tree match {
            case (_: Block) | (_: ClassDef) =>
              subSyms {
                super.traverse(tree)
              }
              
            case Function(vparams, body) =>
              val sym = currentOwner.newAnonymousFunctionValue(NoPosition)
              //println("function sym = " + sym)
              
              tree.setSymbol(sym)
              //enterToCurrentScope(sym)
              
              //setSymbolAndEnter(tree, currentOwner, sym)
              atOwner(sym) {
                subSyms {
                  vparams.foreach(traverseValDef _)
                  traverse(body)
                }
              }
              
            case Ident(n) =>
              if (tree.symbol == null || tree.symbol == NoSymbol || tree.symbol.owner.isNestedIn(rootOwner)) {
                for (s <- syms.get(n))
                  tree.setSymbol(s)
              }
              
            case vd: ValDef =>
              traverseValDef(vd)
              
            case _ =>
              super.traverse(tree)
          }
          
          /*val tpe = tree.tpe
          try {
            tree.tpe = NoType 
            typer.typed(tree)
          } catch { case ex =>
            println("ERROR during typing : " + ex)
            tree.tpe = tpe
          }*/
          //tree.tpe = NoType
          //typer.typed(tree)
        } catch { case ex =>
          println("ERROR while assigning missing symbols to " + tree + " : " + ex)
          throw ex
        }
      }
    }.traverse(root)
    root
  }
  /*
  def healSymbols0(unit: CompilationUnit, rootOwner: Symbol, root: Tree, expectedTpe: Type): Tree = {
    val ctx = analyzer.rootContext(unit, root, true)//rootContext(unit).make(root, currentOwner)
    ctx.retyping = true
    
    val tp = new analyzer.Typer(ctx) {
      var syms = new collection.mutable.HashMap[Name, Symbol]()
      
      def enterToCurrentScope(sym: Symbol) = {
        var scope = context.owner.info.decls
        if (scope == EmptyScope)
          println("ERROR: Empty scope for currentOwner " + context.owner + " (trying to enter symbol " + sym + ")")
        else
          scope enter sym
      }
      override protected def typed1(tree: Tree, mode: Int, pt: Type): Tree = {
        println("TYPING1 (" + tree.tpe + ": " + tree.getClass.getName + " <- " + tree.getClass.getSuperclass.getName + ", sym = " + tree.symbol + ") " + tree)
        super.typed1(tree, mode, pt)
      }
      override def typed(tree: Tree, mode: Int, pt: Type): Tree = {
        println("TYPING (" + tree.tpe + ": " + tree.getClass.getName + " <- " + tree.getClass.getSuperclass.getName + ") " + tree)
        tree match {
          case Ident(n) =>
            println("Found ident " + n + " (syms = " + syms.mkString(", "))
            if (tree.symbol == null || tree.symbol == NoSymbol || tree.symbol.owner.isNestedIn(rootOwner)) {
              for (s <- syms.get(n)) {
                tree.setSymbol(s)
                println("Assigned symbol " + s + " to " + n)
              }
            }
          case _ =>
        }
        super.typed(tree, mode, pt) 
      }
      / *
      override def typedIdent(n: Name): Tree = {
        val tree = Ident(n)
        for (s <- syms.get(n))
          tree.setSymbol(s)
        tree
      }* /
      private def subSyms[V](v: => V): V = {
        val oldSyms = syms
        syms = new collection.mutable.HashMap[Name, Symbol]()
        syms ++= oldSyms
        try {
          v
        } finally {
          syms = oldSyms
        }
      }
      override def typedBlock(tree: Block, mode: Int, pt: Type) = {
        println("TYPING block")
        subSyms { super.typedBlock(tree, mode, pt) }
      }
        
      override def typedFunction(fun: Function, mode: Int, pt: Type) = {
        println("TYPING function")
        subSyms { super.typedFunction(fun, mode, pt) }
      }
        
      override def typedClassDef(tree: ClassDef) = {
        println("TYPING classdef")
        subSyms { super.typedClassDef(tree) }
      }
           
      override def typedValDef(vdef: ValDef): ValDef = {
        println("TYPING ValDef " + vdef)
        val ValDef(mods, name, tpt, rhs) = vdef
        val sym = (
          if (mods.hasFlag(MUTABLE))
            context.owner.newVariable(name)
          else
            context.owner.newValue(name)
        ).setFlag(mods.flags)
        
        vdef.setSymbol(sym)
        //sym.setInfo(Option(tpe).getOrElse(NoType))
        
        syms(name) = sym
        enterToCurrentScope(sym)
        / *
        atOwner(sym) {
          super.typed(tpt)
          super.typed(rhs)
        }
        
        typer.typed(rhs)
        
        var tpe = rhs.tpe
        if (tpe.isInstanceOf[ConstantType])
          tpe = tpe.widen
          
        sym.setInfo(Option(tpe).getOrElse(NoType))
        * /
        super.typedValDef(vdef)
      }
    }
    if (expectedTpe == UnitClass.tpe)
      tp.typed(root)
    else
      tp.typed(root, EXPRmode, expectedTpe)
  }
  */
}
