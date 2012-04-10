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

object ReplacementsComponent {
  val runsAfter = List[String](
    "typer"
  )
  val runsBefore = List[String](
    "refchecks"
  )
  val phaseName = "scalaxy-rewriter"
}

class ReplacementsComponent(val global: Global, val options: PluginOptions, val replacementHolders: AnyRef*)
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

  override val runsAfter = ReplacementsComponent.runsAfter
  override val runsBefore = ReplacementsComponent.runsBefore
  override val phaseName = ReplacementsComponent.phaseName

  import ReplacementDefinitions._
  
  case class ConvertedReplacement(pattern: Tree, replacement: Bindings => Tree)
  
  val replacements = replacementHolders.filter(_ != null).flatMap(getReplacementDefinitions(_)).map { 
    case (n, r) =>
      val conv = mirrorToGlobal(r.pattern, EmptyBindings)
      println("Registered replacement '" + n + "'")
      (n, ConvertedReplacement(conv, bindings => {
        mirrorToGlobal(r.replacement, bindings)
      }))
  } 
  
  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {  
    override def transform(tree: Tree): Tree = {
      val sup = super.transform(tree)
      var expanded = sup
  
      for ((n, r) <- replacements) {
        try {
          val bindings @ Bindings(nameBindings, typeBindings) = matchAndResolveBindings(r.pattern, expanded)
          println("Bindings for '" + n + "':\n\t" + (nameBindings ++ typeBindings).mkString("\n\t"))
          
          val replacement = r.replacement(bindings)
          println("Replacement '" + n + "':\n\t" + replacement.toString.replaceAll("\n", "\n\t"))
          expanded = replacement
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
