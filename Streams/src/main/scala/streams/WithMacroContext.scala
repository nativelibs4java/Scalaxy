/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2013, Olivier Chafik (http://ochafik.com/)
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
package scalaxy.streams

import language.experimental.macros
import scala.reflect.macros.blackbox.Context

import scalaxy.streams.HacksAndWorkarounds.cast

trait WithMacroContext extends WithLocalContext {

  val context: Context
  lazy val global = context.universe
  import global._

  def verbose = false

  override def info(pos: Position, msg: String, force: Boolean) {
    context.info(cast(pos), msg, force = force)
  }
  override def warning(pos: Position, msg: String) {
    context.warning(cast(pos), msg)
  }
  override def error(pos: Position, msg: String) {
    context.error(cast(pos), msg)
  }

  def inferImplicitValue(pt: Type): Tree =
    context.inferImplicitValue(pt.asInstanceOf[context.universe.Type]).asInstanceOf[Tree]

  def fresh(s: String) =
    context.freshName(s)

  // def typeCheck(x: Expr[_]): Tree =
  //   context.typecheck(x.tree.asInstanceOf[context.universe.Tree]).asInstanceOf[Tree]

  def typecheck(tree: Tree): Tree =
    context.typecheck(tree.asInstanceOf[context.universe.Tree]).asInstanceOf[Tree]

  def typecheck(tree: Tree, pt: Type): Tree = {
    if (tree.tpe != null && tree.tpe =:= pt)
      tree
    else
      context.typecheck(
        tree.asInstanceOf[context.universe.Tree],
        pt = pt.asInstanceOf[context.universe.Type]
      ).asInstanceOf[Tree]
  }
}
