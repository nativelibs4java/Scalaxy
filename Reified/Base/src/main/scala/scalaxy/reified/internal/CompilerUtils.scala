package scalaxy.reified.internal

import scala.tools.reflect.ToolBox
import scala.reflect.runtime.universe

object CompilerUtils {
  import universe._
  import definitions._

  /** Try to compile an AST, with or without the attributes */
  def compile(tree: Tree, toolbox: ToolBox[universe.type] = Utils.optimisingToolbox): () => Any = {
    try {
      // val resetTree = Utils.safeReset(tree, toolbox)
      val resetTree = toolbox.resetLocalAttrs(tree)
      toolbox.compile(resetTree)
      // toolbox.compile(Utils.typeCheckTree(resetTree))
    } catch {
      case ex1: Throwable =>
        try {
          toolbox.compile(tree)
        } catch {
          case ex2: Throwable =>
            ex1.printStackTrace()
            throw new RuntimeException("Compilation failed: " + ex1 + "\nSource:\n\t" + tree, ex1)
        }
    }
  }
}

