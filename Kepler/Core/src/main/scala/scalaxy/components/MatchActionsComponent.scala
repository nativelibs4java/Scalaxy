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
import scala.reflect._

//import scala.tools.nsc.typechecker.Contexts._

object MatchActionsComponent {
  val runsAfter = List[String](
    "typer"
  )
  val runsBefore = List[String](
    "refchecks"
  )
  val phaseName = "scalaxy-rewriter"
}

class MatchActionsComponent(val global: Global, val options: PluginOptions, val matchActionHolders: AnyRef*)
extends PluginComponent
   with Transform
   with TypingTransformers
   with Modes
   
   with WithOptions
   with PatternMatchers
   with MirrorConversions
   with SymbolHealers
{
  import global._
  import global.definitions._
  import gen._
  import CODE._
  import scala.tools.nsc.symtab.Flags._
  import typer.typed
  import analyzer.{SearchResult, ImplicitSearch, UnTyper}

  override val runsAfter = MatchActionsComponent.runsAfter
  override val runsBefore = MatchActionsComponent.runsBefore
  override val phaseName = MatchActionsComponent.phaseName

  override val patternUniv = mirror 
  override val candidateUniv = global
  
  import MatchActionDefinitions._
  
  trait TreeFixer {
    val universe: api.Universe
    
    def fixTypedExpression[T](name: String, x: universe.Expr[T]) = {
      if (x.tree.tpe == null) {
        if (x.tpe != null) {
          x.tree.tpe = x.tpe
          println("Fixed pattern tree type for '" + name + "' :\n\t" + x.tree + ": " + x.tree.tpe)
        } else {
          println("Failed to fix pattern tree typefor '" + name + "' :\n\t" + x.tree)
        }
      }
    }
  }
  
  val treeFixer = new TreeFixer {
    val universe = patternUniv
  }
  
  val matchActions = {
    val filteredHolders = matchActionHolders.filter(_ != null)
    val rawMatchActions = filteredHolders.flatMap(holder => {
      val defs = getMatchActionDefinitions(holder)
      if (defs.isEmpty)
        sys.error("ERROR: no definition in holder " + holder)
      defs
    })
    
    for ((n, a) <- rawMatchActions) {
      treeFixer.fixTypedExpression(n.toString, a.pattern.asInstanceOf[treeFixer.universe.Expr[Any]])
    }
    println("Found " + rawMatchActions.size + " match actions in " + filteredHolders.size + " different holders")
    
    /*
    lazy val mirrorToolBox = mirror.mkToolBox()
    lazy val globalToolBox = mkToolBox()
    
    // TODO fix bugs that happen when this is true and/or false : 
    val typeCheckInMirrorSpace = true
    
    rawMatchActions.flatMap { 
      case (n, m) =>
        try {
          val converted =
            if (typeCheckInMirrorSpace) 
              mirrorToGlobal(mirrorToolBox.typeCheck(m.pattern.tree), EmptyBindings)
            else 
              globalToolBox.typeCheck(mirrorToGlobal(m.pattern.tree, EmptyBindings))
            
          println("Registered match action '" + n + "' = " + m)
          println("Converted pattern = " + converted)
          
          Some(n -> ConvertedMatchAction(converted, m))
        } catch { 
          case ex =>
            println("Failed to convert match action '" + n + "':\n\t" + ex + "\n\t" + m)
            ex.printStackTrace
            None
        }
    }
    */
    
    for ((n, m) <- rawMatchActions) {
      println("Registered match action '" + n + "' with pattern : " + m.pattern.tree)// = " + m)
      //println("\tpattern = " + m.pattern)
      //println("\tpattern.tpe = " + m.pattern.tpe)
      //println("\tpattern.tree.tpe = " + m.pattern.tree.tpe)
    }
    
    rawMatchActions.toMap
    /*
    val ret = rawMatchActions.filter(_._1.toString == "simpleForeachUntil")
    ret.toMap
    */
  }
  
  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {  
    override def transform(tree: Tree): Tree = {
      val sup = super.transform(tree)
      var expanded = sup
  
      //println("NOW AT TREE " + tree + " : " + tree.getClass.getName)
      for ((n, matchAction) <- matchActions) {
        try {
          val bindings = 
            matchAndResolveBindings(matchAction.pattern.tree.asInstanceOf[patternUniv.Tree], expanded.asInstanceOf[candidateUniv.Tree])
            
          println("Bindings for '" + n + "':\n\t" + (bindings.nameBindings ++ bindings.typeBindings).mkString("\n\t"))
          
          //matchAction.matchAction match {
          matchAction match  {
            case r @ Replacement(_, _) =>
              val replacement = mirrorToGlobal(r.replacement.tree, bindings)
              println("Replacement '" + n + "':\n\t" + replacement.toString.replaceAll("\n", "\n\t"))
              expanded = replacement
            case MatchWarning(_, message) =>
              unit.warning(tree.pos, message)
            case MatchError(_, message) =>
              unit.error(tree.pos, message)
            case ConditionalAction(_, when, thenMatch) =>
              val treesToTest: List[mirror.Tree] = 
                when.toList.map(n => { 
                  globalToMirror(bindings.nameBindings(n.toString).asInstanceOf[global.Tree])
                })
              
              if (thenMatch.isDefinedAt(treesToTest)) {
                thenMatch.apply(treesToTest) match {
                  case r: ReplaceBy[_] =>
                    val replacement = mirrorToGlobal(r.replacement.tree, bindings)
                    println("Replace by '" + n + "':\n\t" + replacement.toString.replaceAll("\n", "\n\t"))
                    expanded = replacement
                  case Warning(message) =>
                    unit.warning(tree.pos, message)
                  case Error(message) =>
                    unit.error(tree.pos, message)
                  case null =>
                }
              }
          }
        } catch { 
          case NoTypeMatchException(expected, found, msg, depth) =>
            if (false)//depth > 0) 
            {
              println("TYPE ERROR: in replacement '" + n + "' at " + tree.pos + " : " + msg +
                " (\n\texpected = " + expected + ": " + Option(expected).map(_.getClass.getName) + 
                ",\n\tfound = " + found + ": " + Option(found).map(_.getClass.getName) + "\n)"
              )
            }
          case NoTreeMatchException(expected, found, msg, depth) =>
            if (false)//depth > 0) 
            {
              println("TREE ERROR: in replacement '" + n + "' at " + tree.pos + " : " + msg +
                " (\n\texpected = " + expected + ": " + Option(expected).map(_.getClass.getName) + 
                ",\n\tfound = " + found + ": " + Option(found).map(_.getClass.getName) + "\n)"
              )
              println("Tree was " + tree)
              println("Match action was " + matchAction)
            }
        }
      }
      
      try {
        if (expanded eq sup) {
          sup
        } else {
          val expectedTpe = tree.tpe.dealias.deconst.normalize
          
          val tpe = expanded.tpe
          //eraseTypes(expanded)
          //expanded.tpe = null
          expanded = healSymbols(unit, currentOwner, expanded, expectedTpe)
          expanded = typer.typed(expanded, EXPRmode, expectedTpe)
          
          println()
          println("FINAL EXPANSION = \n" + nodeToString(expanded))
          println()
          
          if (expanded.tpe == null || expanded.tpe == NoType)
            expanded.tpe = tpe
          expanded
        }
      } catch { case ex =>
        ex.printStackTrace
        println("Error while trying to replace " + tree + " : " + ex)
        tree
      }
    }
  }
}
