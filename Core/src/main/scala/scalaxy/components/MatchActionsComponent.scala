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
   with Replacements
   with MirrorConversions
   with SymbolHealers
   with WithOptions
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

  import MatchActionDefinitions._
  
  case class ConvertedMatchAction(pattern: Tree, matchAction: MatchAction[Any])
  
  val matchActions = matchActionHolders.filter(_ != null).flatMap(getMatchActionDefinitions(_)).map { 
    case (n, m) =>
      val conv = mirrorToGlobal(m.pattern, EmptyBindings)
      println("Registered match action '" + n + "'")// = " + m)
      (n, ConvertedMatchAction(conv, m))/*bindings => {
        mirrorToGlobal(r.replacement, bindings)
      }))*/
  } 
  
  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {  
    override def transform(tree: Tree): Tree = {
      val sup = super.transform(tree)
      var expanded = sup
  
      for ((n, convertedMatchAction) <- matchActions) {
        try {
          val bindings @ Bindings(nameBindings, typeBindings) = 
            matchAndResolveBindings(convertedMatchAction.pattern, expanded)
            
          println("Bindings for '" + n + "':\n\t" + (nameBindings ++ typeBindings).mkString("\n\t"))
          
          convertedMatchAction.matchAction match {
            case r: Replacement[_] =>
              val replacement = mirrorToGlobal(r.replacement, bindings)
              println("Replacement '" + n + "':\n\t" + replacement.toString.replaceAll("\n", "\n\t"))
              expanded = replacement
            case MatchWarning(_, message) =>
              unit.warning(tree.pos, message)
            case MatchError(_, message) =>
              unit.error(tree.pos, message)
            case ConditionalAction(_, when, then) =>
              val treesToTest: List[mirror.Tree] = 
                when.toList.map(n => { 
                  globalToMirror(nameBindings(global.newTermName(n)))
                })
              
              if (then.isDefinedAt(treesToTest)) {
                then.apply(treesToTest) match {
                  case r: ReplaceBy[_] =>
                    val replacement = mirrorToGlobal(r.replacement, bindings)
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
          case NoTypeMatchException(expected, found, msg) =>
          case NoTreeMatchException(expected, found, msg) =>
            /*
            println("ERROR: in replacement '" + n + "' at " + tree.pos + " : " + msg +
              " (\n\texpected = " + expected + ": " + Option(expected).map(_.getClass.getName) + 
              ",\n\tfound = " + found + ": " + Option(found).map(_.getClass.getName) + "\n)"
            )
            */
        }
      }
      
      try {
        if (expanded eq sup) {
          sup
        } else {
          val expectedTpe = tree.tpe.dealias.deconst
          
          val tpe = expanded.tpe
          //eraseTypes(expanded)
          //expanded.tpe = null
          expanded = healSymbols(unit, currentOwner, expanded, expectedTpe)
          expanded = typer.typed(expanded, EXPRmode, expectedTpe)
          
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
