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
          case _ =>
            tree.setSymbol(NoSymbol)//.setType(NoType)
            //println("Reset symbol of " + tree + " : " + tree.symbol + " : " + nodeToString(tree))
        }
      }
    }.traverse(tree)
    tree
  }
  */
  def healSymbols(rootOwner: Symbol, root: Tree) = {
    new Traverser {
      var syms = new collection.mutable.HashMap[Name, Symbol]()
      currentOwner = rootOwner
      
      def enterToCurrentScope(sym: Symbol) = {
        var scope = currentOwner.info.decls
        if (scope == EmptyScope)
          println("Empty scope for currentOwner " + currentOwner + " (trying to enter symbol " + sym + ")")
        else
          scope enter sym
      }
      override def traverse(tree: Tree) = {
        val oldSyms = syms
        try {
          typer.typed(tree)//, EXPRmode, NoType)
          
          def traverseValDef(vd: ValDef) = {
            val ValDef(mods, name, tpt, rhs) = vd
            val sym = (
              if (mods.hasFlag(MUTABLE))
                currentOwner.newVariable(name)
              else
                currentOwner.newValue(name)
            ).setFlag(mods.flags)
            
            //tree.setSymbol(sym)
            
            tree.setSymbol(sym)
            
            syms(name) = sym
            enterToCurrentScope(sym)
    
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
            case (_: Block) | (_: Function) | (_: ClassDef) =>
              syms = new collection.mutable.HashMap[Name, Symbol]()
              syms ++= oldSyms
            case _ =>
          }
          tree match {
            case Function(vparams, body) =>
              val sym = currentOwner.newAnonymousFunctionValue(NoPosition)
              println("function sym = " + sym)
              
              tree.setSymbol(sym)
              enterToCurrentScope(sym)
              
              //setSymbolAndEnter(tree, currentOwner, sym)
              atOwner(sym) {
                vparams.foreach(traverseValDef _)
                traverse(body)
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
          //tree.tpe = NoType
          //typer.typed(tree)
        } catch { case ex =>
          println("ERROR while assigning missing symbols to " + tree + " : " + ex)
          throw ex
        } finally {
          syms = oldSyms
        }
      }
    }.traverse(root)
  }
}
