// Author: Olivier Chafik (http://ochafik.com)
package scalaxy.debug.plugin
 
import scala.reflect.internal._
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.transform.TypingTransformers
 
/**
 *  To use this, just write the following in `src/main/resources/scalac-plugin.xml`:
 *  <plugin>
 *    <name>debuggable-macros</name>
 *    <classname>scalaxy.debug.plugin.DebuggableMacroPlugin</classname>
 *  </plugin>
 */
class DebuggableMacrosPlugin(override val global: Global) extends Plugin {
  override val name = "debuggable-macros"
  override val description = "Compiler plugin that adds a `@extend(Int) def toStr = self.toString` syntax to create extension methods."
  override val components: List[PluginComponent] =
    List(new DebuggableMacrosComponent(global))
}
 
