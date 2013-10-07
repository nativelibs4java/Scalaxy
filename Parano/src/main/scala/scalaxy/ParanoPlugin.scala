// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.parano

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin

class ParanoPlugin(override val global: Global) extends Plugin {
  override val name = "scalaxy-parano"
  override val description = "Compiler plugin that enforces 'parano' checks to avoid common mistakes."
  override val components = List(new ParanoComponent(global))
}
