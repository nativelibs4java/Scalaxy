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

import org.junit._
import Assert._
import org.hamcrest.CoreMatchers._

import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

class StreamTransformersTest
    extends MiscMatchers
    with StreamTransformers
    with WithRuntimeUniverse
    with WithTestFresh {
  import global._

  // val toolbox = currentMirror.mkToolBox()
  private def comp[T](t: Tree, reset: Boolean): T = {
    var tree = t
    if (reset)
      toolbox.compile(toolbox.resetLocalAttrs(t.asInstanceOf[toolbox.u.Tree]))().asInstanceOf[T]
    else
      toolbox.compile(t.asInstanceOf[toolbox.u.Tree])().asInstanceOf[T]
  }

  private def conv[T](x: Expr[T]) {
    val original = x.tree
    val result = newStreamTransformer(false) transform typeCheck(original, WildcardType)

    println(original)
    println(result)
    assertFalse(original.toString == result.toString)

    val resultValue = comp[T](result, true)
    val originalValue = comp[T](original, true)
    assertEquals(originalValue, resultValue)
  }

  // @Ignore
  @Test def simpleMap {
    conv(reify((0 to 10).map(i => i)))
  }
  @Test def simpleFilterMapMax {
    conv(reify((0 to 10).filter((x: Int) => x % 2 == 0).map((x: Int) => x * 10).max))
  }
  @Test def simpleFilterMapSum {
    conv(reify((0 to 10).filter((x: Int) => x % 2 == 0).map((x: Int) => x * 10).sum))
  }
  @Test def simpleFilterMapToSet {
    conv(reify((0 to 10).filter((x: Int) => x % 2 == 0).map((x: Int) => x * 10).toSet))
  }

  @Ignore
  @Test def matrix {
    conv(reify {
      val rows = 10
      val columns = 20
      import scala.Predef

      val aData = (0 until rows * columns).map(_ * 3.0 + 0.2).toArray
      val bData = (rows * columns until 0 by -1).map(_ * 0.2 + 3.0).toArray
      val outData = new Array[Double](rows * columns)
      val outColumns = columns
      val aRows = rows
      val aColumns = columns
      val bColumns = columns
      for (
        i <- 0 until aRows;
        j <- 0 until bColumns
      ) {
        // TODO chain map and sum (to avoid creating a builder here !)
        // outData(i * outColumns + j) =
        //   (0 until aColumns).map(k => {
        //     aData(i * aColumns + k) * bData(k * bColumns + j)
        //   }).sum
        var sum = 0.0
        for (k <- 0 until aColumns) {
          sum = sum + aData(i * aColumns + k) * bData(k * bColumns + j)
        }
        outData(i * outColumns + j) = sum
      }
    })
  }
}
