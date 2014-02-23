// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.privacy

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin

class PrivacyPlugin(override val global: Global) extends Plugin {
  override val name = "scalaxy-privacy"
  override val description = "Compiler plugin that makes all vals and defs private by default (unless @scalaxy.public is used)."
  override val components = List(
    new PrivacyComponent(global),
    new ExplicitTypeAnnotations(global))
}
