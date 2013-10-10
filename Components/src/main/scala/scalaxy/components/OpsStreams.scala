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
package scalaxy.components

import scala.reflect.api.Universe

trait OpsStreams
    extends MiscMatchers
    with TreeBuilders
    with TraversalOps
    with Streams
    with StreamSources
    with StreamOps
    with StreamSinks {
  val global: Universe
  import global._
  import definitions._
  import Flag._

  case class OpsStream(
    source: StreamSource,
    colTree: Tree,
    ops: List[StreamTransformer])

  object SomeOpsStream {
    def unapply(tree: Tree) = {
      var ops = List[StreamTransformer]()
      var colTree = tree
      var source: StreamSource = null
      var finished = false
      while (!finished) {
        //println("Trying to match " + colTree)
        colTree match {
          case SomeTraversalOp(traversalOp) if traversalOp.op.isInstanceOf[StreamTransformer] =>
            //println("found op " + traversalOp + "\n\twith collection = " + traversalOp.collection)
            val trans = traversalOp.op.asInstanceOf[StreamTransformer]
            if (trans.resultKind != StreamResult)
              ops = List()
            ops = trans :: ops
            colTree = traversalOp.collection
          case StreamSource(cr) =>
            //println("found streamSource " + cr.getClass + " (ops = " + ops + ")")
            source = cr
            if (colTree != cr.unwrappedTree) {
              println("Unwrapping " + colTree.tpe + " into " + cr.unwrappedTree.tpe)
              colTree = cr.unwrappedTree
            } else
              finished = true
          case _ =>
            //if (!ops.isEmpty) println("Finished with " + ops.size + " ops upon "+ tree + " ; source = " + source + " ; colTree = " + colTree)
            finished = true
        }
      }
      if (ops.isEmpty || source == null)
        None
      else
        Some(new OpsStream(source, colTree, ops))
    }
  }
}
