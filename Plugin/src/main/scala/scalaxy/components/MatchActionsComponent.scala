package scalaxy ; package plugin

import pluginBase._
import components._

import scala.collection.JavaConversions._

import scala.io.Source

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.typechecker.Modes

import scala.reflect.runtime.{ universe => ru }

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

  override val patternUniv = ru
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
        while ({ line = try { in.readLine } catch { case _: Throwable => null } ; line != null }) {
          line = line.trim
          if (line.length > 0)
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
  
  if (options.veryVerbose) {
    println("Compilet names:\n\t" + compiletNames.mkString(",\n\t"))
  }

  val matchActions = {
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

    if (options.veryVerbose) {
      for (MatchActionDefinition(n, m) <- rawMatchActions) {
        println("Registered match action '" + n + "' with pattern : " + m.pattern.tree)
      }
    }

    //rawMatchActions.map(d => (d.name, d)).toMap
    rawMatchActions
  }
  
  val matchActionsByTreeClass: Map[Class[_], Seq[MatchActionDefinition]] = {
    matchActions.
      toSeq.
      map(a => a.matchAction.pattern.tree.getClass -> a).
      groupBy(_._1).
      map({ case (c, l) => 
        c -> l.map(_._2) 
      }).
      toMap
  }
  
  // Try and get the match actions that match a tree's class (or any of its super classes)
  // TODO: add cross-checks to avoid discarding too many trees.
  def getMatchActions(tree: Tree) = {
    def actions(c: Class[_]): Seq[MatchActionDefinition] =
      if (c == null) Seq()
      else {
        matchActionsByTreeClass.get(c).getOrElse(Seq()) ++
        actions(c.getSuperclass)
      }
    if (tree == null) Seq()
    else actions(tree.getClass)
  }

  private def toTypedString(v: Any) =
    v + Option(v).map(_ => ": " + v.getClass.getName + " <- " + v.getClass.getSuperclass.getSimpleName).getOrElse("")

  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    override def transform(tree: Tree): Tree = 
    if (!shouldOptimize(tree)) 
      super.transform(tree) 
    else {
      lazy val prefix =
        "[" + phaseName + "] " + 
        new java.io.File(tree.pos.source.path).getName + ":" +
        tree.pos.line + " "
    
      try {
        val sup = try {
          super.transform(tree)
        } catch { case ex: Throwable =>
          ex.printStackTrace
          //println("Failed to super.transform(" + tree + "): " + ex)
          tree
        }
        var expanded = sup

        for (MatchActionDefinition(n, matchAction) <- getMatchActions(tree)) {
          try {
            val bindings =
              matchAndResolveTreeBindings(matchAction.pattern.tree.asInstanceOf[patternUniv.Tree], expanded.asInstanceOf[candidateUniv.Tree])

            if (options.veryVerbose) {
              println(prefix + "Bindings for '" + n + "':\n\t" + (bindings.nameBindings ++ bindings.typeBindings ++ bindings.functionBindings).mkString("\n\t"))
            }

            matchAction match  {
              case r @ Replacement(_, _) =>
                expanded = mirrorToGlobal(ru)(r.replacement.tree, bindings)
              case MatchWarning(_, message) =>
                unit.warning(tree.pos, message)
              case MatchError(_, message) =>
                unit.error(tree.pos, message)
              case ca @ ConditionalAction(_, when, thenMatch) =>
                val treesToTest: List[ru.Tree] =
                  when.toList.map(n => {
                    globalToMirror(ru)(bindings.nameBindings(n.toString).asInstanceOf[global.Tree])
                  })

                if (thenMatch.isDefinedAt(treesToTest)) {
                  thenMatch.apply(treesToTest) match {
                    case r: ReplaceBy[_] =>
                      expanded = mirrorToGlobal(ru)(r.replacement.tree, bindings)
                    case Warning(message) =>
                      unit.warning(tree.pos, message)
                    case Error(message) =>
                      unit.error(tree.pos, message)
                    case null =>
                  }
                }
            }
            if (options.verbose) {
              println(prefix + "Applied compilet " + n)
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
          
          if (HacksAndWorkarounds.healSymbols) {
              expanded = healSymbols(unit, currentOwner, expanded, expectedTpe)
          }

          try {
            expanded = typer.typed(expanded, EXPRmode, expectedTpe)
          } catch { case ex: Throwable =>
            ex.printStackTrace
          }

          if (options.debug)
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
        if (options.debug)
          ex.printStackTrace

        val msg = prefix + "Error while trying to replace " + tree + " : " + ex
        
        global.warning(msg)
        println(msg)
        tree
      }
    }
  }
}
