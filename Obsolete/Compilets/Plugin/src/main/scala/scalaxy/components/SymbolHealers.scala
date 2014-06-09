package scalaxy.compilets
package plugin
import pluginBase._
import components._

import scala.tools.nsc.Global

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.typechecker.Analyzer
import scala.tools.nsc.typechecker.Contexts
import scala.tools.nsc.typechecker.Modes
import scala.tools.nsc.typechecker.RefChecks
import scala.Predef._

trait SymbolHealers
extends TypingTransformers
   with Modes
{
  this: PluginComponent =>

  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._

  def healSymbols(unit: CompilationUnit, rootOwner: Symbol, root: Tree, expectedTpe: Type): Tree = {
    
    val transformerV1 = new TypingTransformer(unit) {
      var syms = new collection.mutable.HashMap[String, Symbol]()
      //currentOwner = rootOwner
      
      def subSyms[V](v: => V): V = {
        val oldSyms = syms
        syms = new collection.mutable.HashMap[String, Symbol]()
        syms ++= oldSyms
        try {
          v
        } finally {
          syms = oldSyms
        }
      }
      
      override def transform(tree: Tree) = {
        try {
          def transformValDef(vd: ValDef) = {
            val ValDef(mods, name, tpt, rhs) = vd
            //println("Found valdef " + vd + ", tpt = " + tpt + ", tpe = " + vd.tpe)
            val sym = (
              if (mods.hasFlag(MUTABLE))
                currentOwner.newVariable(name)
              else
                currentOwner.newValue(name)
            ).setFlag(mods.flags)

            tree.setSymbol(sym)

            syms(name.toString) = sym

            atOwner(sym) {
              transform(tpt)
              transform(rhs)
            }

            typer.typed(rhs)

            var tpe = rhs.tpe
            if ((tpe == null || tpe == NoType) && rhs.symbol != null && rhs.symbol != NoSymbol)
              tpe = if (rhs.symbol.isType) rhs.symbol.asType.toType else rhs.symbol.typeSignature
            if (tpe.isInstanceOf[ConstantType])
              tpe = tpe.widen

            sym.setInfo(Option(tpe).getOrElse(NoType))

            val rep = ValDef(mods, name, TypeTree(tpe), rhs)
            rep.symbol = sym
            rep
          }

          tree match {
            case (_: Block) | (_: ClassDef) =>
              //println("Found block or class " + tree)
              //subSyms 
              {
                super.transform(tree)
              }

            case Function(vparams, body) =>
              //println("FUNCTION.tpe = " + tree.tpe)
              val sym = currentOwner.newAnonymousFunctionValue(NoPosition)
              tree.setSymbol(sym)

              atOwner(sym) {
                //subSyms 
                {
                  vparams.foreach(transformValDef _)
                  transform(body)
                }
              }
              tree

            case Ident(n) =>
              // if tree.symbol.owner.isNestedIn(rootOwner)
              for (s <- syms.get(n.toString))
                tree.setSymbol(s)
              tree

            case vd: ValDef =>
              transformValDef(vd)

            case _ =>
              super.transform(tree)
          }
        } catch { case ex: Throwable =>
          println("ERROR while assigning missing symbols to " + tree + ": " + tree.getClass.getName + " : " + ex + "\n\t" + nodeToString(tree))
          ex.printStackTrace
          throw ex
        }
      }
    }
    
    val transformerV2 = new TypingTransformer(unit) {
      //var scopes: List[Scope] = newScope :: Nil
      //def scoped[T](v: => T): T = {
      //  scopes = newNestedScope(currentScope) :: scopes
      //  try {
      //    v
      //  } finally {
      //    scopes = scopes.tail
      //  }
      //}
      //def currentScope = scopes.head 
      def currentScope: Option[Scope] = Option(typer.context).flatMap(c => Option(c.scope))
      
      override def transform(tree: Tree) = {
        try {
          tree match {
            case Function(vparams, body) =>
              val sym = currentOwner.newAnonymousFunctionValue(NoPosition)
              tree.setSymbol(sym)
              
              // Not really useful, is it?
              for (scope <- currentScope)
                scope.enter(sym)
              
              atOwner(sym) { //scoped {
                super.transform(tree)
              }
              //  typer.typed(body)
              //}
              //typer.typed(super.transform(tree))
            case Block(_, _) =>
              //scoped {
                super.transform(tree)
              //}
              
            case vd @ ValDef(mods, name, tpt, rhs) =>
              //println("Found valdef " + vd + ", tpt = " + tpt + ", tpe = " + vd.tpe)
              val sym = (
                if (mods.hasFlag(MUTABLE))
                  currentOwner.newVariable(name)
                else
                  currentOwner.newValue(name)
              ).setFlag(mods.flags)
  
              tree.setSymbol(sym)
              
              for (scope <- currentScope) {
                println("ENTER " + sym)
                scope.enter(sym)
              }
              
              atOwner(sym) {
                val rhs2 = transform(rhs)
                typer.typed(rhs2)
              
                var tpe = rhs2.tpe
                if ((tpe == null || tpe == NoType) && rhs2.symbol != null && rhs2.symbol != NoSymbol)
                  tpe = 
                    if (rhs.symbol.isType) rhs.symbol.asType.toType
                    else rhs.symbol.typeSignature
                if (tpe.isInstanceOf[ConstantType])
                  tpe = tpe.widen
    
                for (t <- Option(tpe)) {
                  tpe = //tpe.dealias.dealias// 
                    tpe.deconst.dealias.dealias
                  sym.setInfo(tpe)
                }
  
                val tpt2 = TypeTree(tpe)//typer.typed(TypeTree(tpe))
                val c = typer.typed(vd.copy(mods, name, tpt2, rhs2))
                //rhs2.tpe = NoType
                //val c = vd.copy(mods, name, TypeTree(tpe), rhs2)
                //c.tpe = tpe
                c
                //super.transform(tree)
              }
          
            case Ident(name) =>
              if (tree.symbol == null || tree.symbol == NoSymbol) {
                for (scope <- currentScope) {
                  val sym = scope.lookup(name)
                  if (sym != null && sym != NoSymbol) {
                    tree.symbol = sym
                    println("FOUND " + name + " -> " + tree.symbol)
                  } else {
                    println("NOT FOUND " + name + " (scope = " + scope + ")")
                  }
                }
              }
                //tree.symbol = currentScope.lookup(name)
              
              super.transform(tree)
            
            case Literal(Constant(v)) =>
              val tpe = v match {
                case _: Int => IntTpe
                case _: Short => ShortTpe
                case _: Long => LongTpe
                case _: Byte => ByteTpe
                case _: Double => DoubleTpe
                case _: Float => FloatTpe
                case _: Char => CharTpe
                case _: Boolean => BooleanTpe
                case _: String => StringClass.asType.toType
                case _: Unit => UnitClass.asType.toType
                case _ =>
                  null
              }
              for (t <- Option(tpe))
                tree.tpe = t
              
              super.transform(tree)
              
            case _ =>
              super.transform(tree)
              //typer.typed(super.transform(tree))
          }
          
        } catch { case ex: Throwable =>
          println("ERROR while assigning missing symbols to " + tree + ": " + tree.getClass.getName + " : " + ex + "\n\t" + nodeToString(tree))
          ex.printStackTrace
          throw ex
        }
        
      }
    }
    
    val transformer =
      if ("1" == System.getenv("SCALAXY_HEALER_V2"))
        transformerV2
      else
        transformerV1
        
    transformer.atOwner(rootOwner) {
      transformer.transform(root)
    }
    /*
    val refChecks = new RefChecks {
      override val global = SymbolHealers.this.global
      override val runsAfter = Nil
      override val runsRightAfter = None
    }
    refChecks.newTransformer(unit.asInstanceOf[refChecks.global.CompilationUnit]).transform(root.asInstanceOf[refChecks.global.Tree]).asInstanceOf[global.Tree]
    */
  }
}
