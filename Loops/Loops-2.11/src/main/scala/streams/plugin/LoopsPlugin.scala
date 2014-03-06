// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.loops

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

object LoopsPlugin {
  def getInternalPhases(global: Global): List[PluginComponent] =
    List(new LoopsComponent(global))
}

class LoopsPlugin(override val global: Global) extends Plugin {

  override val name = "scalaxy-loops"

  override val description = "Compiler plugin that rewrites collection streams into while loops."

  override val components = LoopsPlugin.getInternalPhases(global)
}
