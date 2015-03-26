// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.fastcaseclasses

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin

class FastCaseClassesPlugin(override val global: Global) extends Plugin {
  override val name = "scalaxy-fastcaseclasses"
  override val description = "Compiler plugin that creates faster case classes."
  override val components = List(new TypedFastCaseClassesComponent(global))
}
