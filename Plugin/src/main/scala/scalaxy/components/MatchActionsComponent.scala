package scalaxy ; package plugin

import pluginBase._
import components._

import scala.collection.JavaConversions._

import scala.io.Source

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.typechecker.Modes

import scala.reflect._

object MatchActionsComponent {
  val runsAfter = List[String](
    "typer"
  )
  val runsBefore = List[String](
    "patmat"
  )
  val phaseName = "scalaxy-rewriter"
}

class MatchActionsComponent(val global: Global, val options: PluginOptions, val compiletNamesOpt: Option[Seq[String]] = None)
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

  override val patternUniv = runtime.universe
  override val candidateUniv = global

  import MatchActionDefinitions._

  // SPI / java.util.ServiceLoader-like convention to list compilets in JARs.
  val compiletsListPath =
    "META-INF/services/" + classOf[Compilet].getName
    
  val detectCompilets = true
  val compiletNames: Set[String] = (compiletNamesOpt.getOrElse {
    val classLoader = classOf[Compilet].getClassLoader
    (for (resource <- classLoader.getResources(compiletsListPath)) yield {
      if (options.verbose) {
        println("Found resource '" + resource + "' for '" + compiletsListPath + "'")
      }
      import java.io._
        
      val in = new BufferedReader(new InputStreamReader(resource.openStream()))
      try {
        var line: String = null
        var out = collection.mutable.ArrayBuilder.make[String]()
        while ({ line = try { in.readLine } catch { case _ => null } ; line != null }) {
          out += line
        }
        out.result()
        /*for {
          line <- Source.fromInputStream(in).getLines.map(_.trim)
          if line.length > 0
        } yield line*/
      } finally {
        in.close()
      }
    }).flatten
  }).toSet
  
  if (options.verbose) {
    println("Compilet names:\n\t" + compiletNames.mkString(",\n\t"))
  }

  val matchActions = {
    //println("compiletNames = " + compiletNames)
    val rawMatchActions = compiletNames.flatMap(compiletName => {
      val defs = getCompiletDefinitions(compiletName)
      // TODO: Sort compilets based on runsAfter.
      if (defs.definitions.isEmpty)
        sys.error("ERROR: no definition in compilet '" + compiletNames + "'")
      else
        defs.definitions
    })

    if (HacksAndWorkarounds.fixTypedExpressionsType) {
      val treeFixer = new ExprTreeFixer {
        val universe = patternUniv
      }
      for (MatchActionDefinition(n, m) <- rawMatchActions) {
        treeFixer.fixTypedExpression(
          n.toString,
          m.pattern.asInstanceOf[treeFixer.universe.Expr[Any]])
      }
    }

    if (options.verbose) {
      for (MatchActionDefinition(n, m) <- rawMatchActions) {
        println("Registered match action '" + n + "' with pattern : " + m.pattern.tree)
      }
    }

    rawMatchActions.map(d => (d.name, d)).toMap
  }

  private def toTypedString(v: Any) =
    v + Option(v).map(_ => ": " + v.getClass.getName + " <- " + v.getClass.getSuperclass.getSimpleName).getOrElse("")

  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    override def transform(tree: Tree): Tree = {
      try {
        val sup = try {
          super.transform(tree)
        } catch { case ex: Throwable =>
          ex.printStackTrace
          //println("Failed to super.transform(" + tree + "): " + ex)
          tree
        }
        var expanded = sup

        for ((n, MatchActionDefinition(_, matchAction)) <- matchActions) {
          try {
            val bindings =
              matchAndResolveTreeBindings(matchAction.pattern.tree.asInstanceOf[patternUniv.Tree], expanded.asInstanceOf[candidateUniv.Tree])

            if (options.verbose)
            {
              println("Bindings for '" + n + "':\n\t" + (bindings.nameBindings ++ bindings.typeBindings ++ bindings.functionBindings).mkString("\n\t"))
            }

            matchAction match  {
              case r @ Replacement(_, _) =>
                val replacement =
                  mirrorToGlobal(runtime.universe)(r.replacement.tree, bindings)
                //println("Replacement '" + n + "':\n\t" + replacement.toString.replaceAll("\n", "\n\t"))
                expanded = replacement
              case MatchWarning(_, message) =>
                unit.warning(tree.pos, message)
              case MatchError(_, message) =>
                unit.error(tree.pos, message)
              case ca @ ConditionalAction(_, when, thenMatch) =>
                val treesToTest: List[scala.reflect.runtime.universe.Tree] =
                  when.toList.map(n => {
                    globalToMirror(
                      scala.reflect.runtime.universe
                    )(
                      bindings.nameBindings(n.toString).asInstanceOf[global.Tree]
                    )
                  })

                if (thenMatch.isDefinedAt(treesToTest)) {
                  thenMatch.apply(treesToTest) match {
                    case r: ReplaceBy[_] =>
                      val replacement = mirrorToGlobal(
                        scala.reflect.runtime.universe
                      )(
                        r.replacement.tree, bindings
                      )
                      //println("Replace by '" + n + "':\n\t" + replacement.toString.replaceAll("\n", "\n\t"))
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
            case NoTypeMatchException(expected, found, msg, depth, insideExpected, insideFound) =>
              if (false)//depth > 0)
              {
                println("TYPE ERROR: in replacement '" + n + "' at " + tree.pos + " : " + msg +
                  " (")
                println("\texpected = " + toTypedString(expected))
                println("\tfound = " + toTypedString(found))
                println("\tinside expected = " + insideExpected)
                println("\tinside found = " + insideFound)
                println(")")
              }
            case NoTreeMatchException(expected, found, msg, depth) =>
              if (false)//depth > 1)
              {
                println("TREE ERROR: in replacement '" + n + "' at " + tree.pos + " : " + msg +
                  " (\n\texpected = " + toTypedString(expected) +
                  ",\n\tfound = " + toTypedString(found) + "\n)"
                )
                println("Tree was " + tree)
                println("Match action was " + matchAction)
              }
          }
        }

        if (expanded eq sup) {
          sup
        } else {
          val expectedTpe = tree.tpe.dealias.deconst.normalize

          val tpe = expanded.tpe
          //eraseTypes(expanded)
          //expanded.tpe = null

          if (HacksAndWorkarounds.healSymbols) {
              expanded = healSymbols(unit, currentOwner, expanded, expectedTpe)
          }

          try {
            expanded = typer.typed(expanded, EXPRmode, expectedTpe)
          } catch { case ex: Throwable =>
            ex.printStackTrace
          }

          if (options.verbose)
          {
            println()
            println("FINAL EXPANSION = \n" + nodeToString(expanded))
            //println("FINAL EXPANSION = \n" + expanded)
            println()
          }

          if (expanded.tpe == null || expanded.tpe == NoType)
            expanded.tpe = tpe
          expanded
        }
      } catch { case ex: Throwable =>
        println(ex)
        //if (options.verbose)
          ex.printStackTrace
        println("Error while trying to replace " + tree + " : " + ex)
        tree
      }
    }
  }
}
