/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package scalaxy ; package plugin
import pluginBase._
import components._

import java.io.File
import scala.collection.immutable.Stack

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.Settings
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.util.parsing.input.Position

/**
 * http://www.scala-lang.org/node/140
 * http://lamp.epfl.ch/~emir/bqbase/2005/06/02/nscTutorial.html
 * http://code.google.com/p/simple-build-tool/wiki/CompilerPlugins
 *
 * sbt -sbt-snapshot "run test.scala"
 */
trait ScalaxyPluginDefLike extends PluginDef {
  override val name = "Scalaxy"
  override val description =
    "This plugin rewrites some Scala constructs (like for loops) to make them faster."
    
  override def envVarPrefix = "SCALAXY_"
  
  override def createOptions(settings: Settings): PluginOptions =
    new PluginOptions(this, settings)
  
  def matchActionHolders: Seq[AnyRef]
  
  override def createComponents(global: Global, options: PluginOptions): List[PluginComponent] =
    List(
      new MatchActionsComponent(global, options, matchActionHolders:_*)
    )
      
  override def getCopyrightMessage: String =
    "Scalaxy Plugin\nCopyright Olivier Chafik 2010-2012"
}
object ScalaxyPluginDef extends ScalaxyPluginDefLike {
  override def matchActionHolders = Seq(
    //compilets.Example,
    //compilets.Streams,
    
    //compilets.Java,
    //compilets.Numeric,
    //compilets.ForLoops
    //compilets.Maps,
    
    compilets.SingleForLoop
  )
}

class ScalaxyPlugin(override val global: Global) 
extends PluginBase(global, ScalaxyPluginDef)

object Compile extends CompilerMain {
  override def pluginDef = ScalaxyPluginDef
  override def commandName = "scalaxy"
}

