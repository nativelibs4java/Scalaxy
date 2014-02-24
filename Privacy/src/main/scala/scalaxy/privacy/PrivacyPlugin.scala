// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.privacy

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

object PrivacyPlugin {
  def getInternalPhases(global: Global): List[PluginComponent] =
    List(
      new PrivacyComponent(global),
      new ExplicitTypeAnnotationsComponent(global, runAfter = PrivacyComponent.phaseName))
}

class PrivacyPlugin(override val global: Global) extends Plugin {

  override val name = "scalaxy-privacy"

  override val description = "Compiler plugin that makes all vals and defs private by default (unless @scalaxy.public is used)."

  override val components = PrivacyPlugin.getInternalPhases(global)
}
