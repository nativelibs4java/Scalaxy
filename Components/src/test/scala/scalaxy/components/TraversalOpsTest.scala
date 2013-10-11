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

class TraversalOpsTest
    extends TraversalOpsMatchers
    with WithRuntimeUniverse
    with WithTestFresh {
  import global._
  import definitions._

  override def warning(pos: Position, msg: String) =
    println(msg + " (" + pos + ")")

  object op {
    def unapply(t: Tree) = Option(t) collect {
      case SomeTraversalOp(TraversalOp(op, _, _, _, _, _)) => op
    }
  }

  @Test def testForall {
    val op(AllOrSomeOp(_, _, _)) = typeCheck(reify(Seq(1).forall(_ == 1)))
  }
  @Test def testExists {
    val op(AllOrSomeOp(_, _, _)) = typeCheck(reify(Seq(1).exists(_ == 1)))
  }
  @Ignore
  @Test def testCollect {
    val op(CollectOp(_, _, _)) =
      typeCheck(reify(Seq(1).collect({ case v: Int => 1 })).tree, typeOf[Seq[Int]])
  }
  @Test def testCount {
    val op(CountOp(_, _)) = typeCheck(reify(Seq(1).count(_ == 1)))
  }
  @Test def testFilter {
    val op(FilterOp(_, _, _)) = typeCheck(reify(Seq(1).filter(_ == 1)))
  }

  @Test def testTakeWhile {
    val op(FilterWhileOp(_, _, _)) = typeCheck(reify(Seq(1).takeWhile(_ == 1)))
  }
  @Test def testDropWhile {
    val op(FilterWhileOp(_, _, _)) = typeCheck(reify(Seq(1).dropWhile(_ == 1)))
  }

  @Test def testFind {
    val op(FindOp(_, _)) = typeCheck(reify(Seq(1).find(_ == 1)))
  }
  @Test def testFoldLeft {
    val op(o @ FoldOp(_, _, _, _)) = typeCheck(reify(Seq(1).foldLeft(0)(_ + _)))
    assertEquals(true, o.isLeft)
  }
  // @Ignore
  @Test def testFoldRight {
    val op(o @ FoldOp(_, _, _, _)) = typeCheck(reify(Seq(1).foldRight(0)(_ + _)))
    assertEquals(false, o.isLeft)
  }

  @Test def testForeach {
    val op(ForeachOp(_, _)) = typeCheck(reify(Seq(1).foreach(_ == 1)))
  }
  @Test def testMap {
    val op(MapOp(_, _, _)) = typeCheck(reify(Seq(1).map(_ == 1)))
  }
  @Test def testMax {
    val op(MaxOp(_)) = typeCheck(reify(Seq(1).max))
  }
  @Test def testMin {
    val op(MinOp(_)) = typeCheck(reify(Seq(1).min))
  }
  @Test def testProduct {
    val op(ProductOp(_)) = typeCheck(reify(Seq(1).product))
  }

  @Test def testReduceLeft {
    val op(o @ ReduceOp(_, _, _)) = typeCheck(reify(Seq(1).reduceLeft(_ + _)))
    assertEquals(true, o.isLeft)
  }
  // @Ignore
  @Test def testReduceRight {
    val op(o @ ReduceOp(_, _, _)) = typeCheck(reify(Seq(1).reduceRight(_ + _)))
    assertEquals(false, o.isLeft)
  }

  @Test def testReverse {
    val op(ReverseOp(_)) = typeCheck(reify(Seq(1).reverse))
  }

  @Test def testScanLeft {
    val op(o @ ScanOp(_, _, _, _, _)) = typeCheck(reify(Seq(1).scanLeft(0)(_ + _)))
    assertEquals(true, o.isLeft)
  }
  // @Ignore
  @Test def testScanRight {
    val op(o @ ScanOp(_, _, _, _, _)) = typeCheck(reify(Seq(1).scanRight(0)(_ + _)))
    assertEquals(false, o.isLeft)
  }

  @Test def testSum {
    val op(SumOp(_)) = typeCheck(reify(Seq(1).sum))
  }

  @Test def testToArray {
    val op(ToArrayOp(_)) = typeCheck(reify(Seq(1).toArray).tree, typeOf[Array[Int]])
  }
  @Test def testToIndexedSeq {
    val op(ToIndexedSeqOp(_)) = typeCheck(reify(Seq(1).toIndexedSeq))
  }
  @Test def testToList {
    val op(ToListOp(_)) = typeCheck(reify(Seq(1).toList))
  }
  @Test def testToSeq {
    val op(ToSeqOp(_)) = typeCheck(reify(Seq(1).toSeq))
  }
  @Test def testToSet {
    val op(ToSetOp(_)) = typeCheck(reify(Seq(1).toSet))
  }
  @Test def testToVector {
    val op(ToVectorOp(_)) = typeCheck(reify(Seq(1).toVector))
  }

  @Test def testZip {
    val op(ZipOp(_, _)) = typeCheck(reify(Seq(1).zip(Seq(2))))
  }
  @Ignore
  @Test def testZipWithIndex {
    val op(ZipWithIndexOp(_)) = typeCheck(reify(Seq(1).zipWithIndex))
  }
}
