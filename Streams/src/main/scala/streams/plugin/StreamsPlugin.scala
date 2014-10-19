// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.streams

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

object StreamsPlugin {
  def getInternalPhases(global: Global): List[PluginComponent] =
    List(new StreamsComponent(global))
}

class StreamsPlugin(override val global: Global) extends Plugin {

  override val name = "scalaxy-streams"

  override val description = "Compiler plugin that rewrites collection streams into while loops."

  override val components = StreamsPlugin.getInternalPhases(global)
}
