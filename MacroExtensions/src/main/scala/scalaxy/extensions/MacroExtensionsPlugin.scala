// Author: Olivier Chafik (http://ochafik.com)
// Feel free to modify and reuse this for any purpose ("public domain / I don't care").
package scalaxy.extensions

import scala.collection.mutable

import scala.reflect.internal._
import scala.reflect.ClassTag

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
 *    <name>scalaxy-macro-extensions</name>
 *    <classname>scalaxy.extensions.MacroExtensionsPlugin</classname>
 *  </plugin>
 */
class MacroExtensionsPlugin(override val global: Global) extends Plugin {
  override val name = "scalaxy-extensions"
  override val description = "Compiler plugin that adds a `@scalaxy.extension[Int] def toStr = self.toString` syntax to create extension methods."
  override val components: List[PluginComponent] =
    List(new MacroExtensionsComponent(global))
}
