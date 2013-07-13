package scalaxy.compilets
package plugin

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
  val runsAfter = List("typer")
  val runsBefore = List("patmat")
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
    val resources = classLoader.getResources(compiletsListPath).toSeq
    if (options.veryVerbose)
      println("Compilet resources (" + compiletsListPath + "):\n\t" + resources.mkString("\n\t"))
    (for (resource <- resources) yield {
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

  case class CompiletAction(
    matchActionDefinition: MatchActionDefinition,
    compiletName: String,
    runsAfterCompiletNames: Seq[String])
    
  val compiletActions: Seq[CompiletAction] = {
    val actions = compiletNames.flatMap(compiletName => {
      val compiletDefinitions = getCompiletDefinitions(compiletName)
      // TODO: Sort compilets based on runsAfter.
      if (compiletDefinitions.definitions.isEmpty)
        sys.error("ERROR: no definition in compilet '" + compiletNames + "'")
      else
        compiletDefinitions.definitions.map(d => CompiletAction(d, compiletName, compiletDefinitions.runsAfter))
    })

    if (HacksAndWorkarounds.fixTypedExpressionsType) {
      val treeFixer = new ExprTreeFixer {
        val universe = patternUniv
      }
      for (MatchActionDefinition(n, m) <- actions.map(_.matchActionDefinition)) {
        treeFixer.fixTypedExpression(
          n.toString,
          m.pattern.asInstanceOf[treeFixer.universe.Expr[Any]])
      }
    }

    if (options.veryVerbose) {
      for (MatchActionDefinition(n, m) <- actions.map(_.matchActionDefinition)) {
        println("Registered match action '" + n + "' with pattern : " + m.pattern.tree)
      }
    }

    order(actions.toSeq)
  }
  
  def order(actions: Seq[CompiletAction]): Seq[CompiletAction] = {
    if (actions.size <= 1)
      actions
    else {
      val nameToAction = actions.map(a => a.compiletName -> a).toMap
      
      import scala.collection.mutable
      val ordered = mutable.ArrayBuilder.make[CompiletAction]()
      val set = mutable.HashSet[CompiletAction]()
      def sub(a: CompiletAction) {
        if (set.add(a)) {
          for (d <- a.runsAfterCompiletNames)
            sub(nameToAction(d))
          ordered += a
        }
      }
      actions.foreach(sub _)
      
      ordered.result()
    }
  }
  
  val matchActionsByTreeClass: Map[Class[_], Seq[CompiletAction]] = {
    compiletActions.
      toSeq.
      map(cd => cd.matchActionDefinition.matchAction.pattern.tree.getClass -> cd).
      groupBy(_._1).
      map({ case (c, l) => 
        c -> l.map(_._2) 
      }).
      toMap
  }
  
  // Try and get the match actions that match a tree's class (or any of its super classes)
  // TODO: add cross-checks to avoid discarding too many trees.
  def getMatchActions(tree: Tree): Seq[MatchActionDefinition] = {
    if (HacksAndWorkarounds.onlyTryPatternsWithSameClass) {
      def actions(c: Class[_]): Seq[CompiletAction] =
        if (c == null) Seq()
        else {
          matchActionsByTreeClass.get(c).getOrElse(Seq()) ++
          actions(c.getSuperclass)
        }
      order(
        if (tree == null) Seq()
        else actions(tree.getClass)
      ).map(_.matchActionDefinition)
    } else {
      compiletActions.map(_.matchActionDefinition)
    }
  }

  private def toTypedString(v: Any) =
    v + Option(v).map(_ => ": " + v.getClass.getName + " <- " + v.getClass.getSuperclass.getSimpleName).getOrElse("")

  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    override def transform(tree: Tree): Tree = 
    if (!shouldOptimize(tree)) 
      super.transform(tree) 
    else {
      lazy val prefix =
        "[" + phaseName + "] " + (
          if (tree.pos == NoPosition) "<no pos> "
          else {
            val pos = tree.pos
            new java.io.File(pos.source.path).getName + ":" + pos.line + " "
          }
        )
    
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
          //println(prefix + "matching " + n)
          val bindings =
            matchAndResolveTreeBindings(matchAction.pattern.tree.asInstanceOf[patternUniv.Tree], expanded.asInstanceOf[candidateUniv.Tree])
          //println("\t-> failure = " + bindings.failure)
          
          if (bindings.failure == null) {
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
          } else {
            bindings.failure match {
              case NoTypeMatchFailure(expected, found, msg, depth, insideExpected, insideFound) =>
                if (HacksAndWorkarounds.debugFailedMatches && depth > 1)
                {
                  println("TYPE ERROR (depth " + depth + "): in replacement '" + n + "' at " + tree.pos + " : " + msg +
                    " (")
                  println("\texpected = " + toTypedString(expected))
                  println("\tfound = " + toTypedString(found))
                  println("\tinside expected = " + insideExpected)
                  println("\tinside found = " + insideFound)
                  println("\ttree = " + tree)
                  println(")")
                }
              case NoTreeMatchFailure(expected, found, msg, depth) =>
                if (HacksAndWorkarounds.debugFailedMatches && depth > 1)
                {
                  println("TREE ERROR (depth " + depth + "): in replacement '" + n + "' at " + tree.pos + " : " + msg +
                    " (\n\texpected = " + toTypedString(expected) +
                    ",\n\tfound = " + toTypedString(found) + "\n)"
                  )
                  println("Tree was " + tree)
                  println("Match action was " + matchAction)
                }
            }
          }
        }

        if (expanded eq sup) {
          sup
        } else {
          val expectedTpe = tree.tpe.dealias.deconst.normalize
          val tpe = expanded.tpe

          if (options.debug)
          {
            println()
            println("EXPANSION BEFORE HEALING = \n" + expanded + "\n" + nodeToString(expanded))
            //println("FINAL EXPANSION = \n" + expanded)
            println()
          }
          
          if (HacksAndWorkarounds.healSymbols) {
            try {
              expanded = healSymbols(unit, currentOwner, expanded, expectedTpe)
            } catch { case ex: Throwable =>
              ex.printStackTrace
            }
          }

          try {
            expanded = typer.typed(expanded, EXPRmode, expectedTpe)
          } catch { case ex: Throwable =>
            ex.printStackTrace
          }

          if (options.debug)
          {
            println()
            println("FINAL EXPANSION = \n" + expanded + "\n" + nodeToString(expanded))
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
