package scalaxy.loops.test

import org.junit._
import Assert._

class LoopsTest {
  import scalaxy.loops.optimize
  import scalaxy.loops.TuploidValues

  @Test
  def test {
    val n = 10
    // val cols = for (i <- 0 to n) yield for (i <- 0)
    // val s: Seq[Int] = optimize {
    //   for (i <- 1 to n; j <- 0 to n; val prod = i * j; if prod < (i + j)) yield {
    //     (i - j) / (prod + 1) 
    //   }
    // }

    optimize {
      for (i <- 0 to n) {
        println(i)
      }
      for (l <- 10L until 2L by -2) yield { l + 1 }

      val arrays = Array(Array(1, 2), Array(3, 4))

      for (array <- arrays;
           length = array.length * 30;
           if length < 10;
           v <- array)
        yield
          (length, v)

      
        // scala.this.Predef.refArrayOps(scala.this.Predef.refArrayOps(arrays)
        //   .map(((array: Array[Int]) => {
        //     val length: Int = array.length;
        //     scala.Tuple2.apply[Array[Int], Int](array, length)
        //   }))
        //   .withFilter(((x$1: (Array[Int], Int)) =>
        //     (x$1: (Array[Int], Int) @unchecked) match {
        //       case ((array @ _), (length @ _)) =>
        //         length.<(10)
        //   }))
        //   .flatMap(((x$2: (Array[Int], Int)) =>
        //     (x$2: (Array[Int], Int) @unchecked) match {
        //       case ((array @ _), (length @ _)) =>

        //       scala.this.Predef.refArrayOps(scala.this.Predef.intArrayOps(array)
        //         .map(((v: Int) =>
        //           scala.Tuple2.apply(length, v))
        //     }
        //   ))

        // var i = 0
        // val length1 = arrays.length
        // val b = ArrayBuffer[(Int, Int)]()
        // while (i < length1) {
        //   val array = arrays(i)
        //   val length = array.length
        //   if (length < 10) {
        //     var j = 0
        //     val length2 = array.length
        //     while (j < length2) {
        //       val v = array(j)
        //       b += ((length, v))
        //       j += 2
        //     }
        //   }
        //   i += 1
        // }
      

      {}


    }
  }

  @Test
  def tupleMappedToTuple {
    new TuploidValues {
      override val global = scala.reflect.runtime.universe
      import global._
      import scala.reflect.runtime.currentMirror
      import scala.tools.reflect.ToolBox
      val toolbox = currentMirror.mkToolBox()

      val f = toolbox.typeCheck(q"""
        (x2: (Int, Int)) => (x2: (Int, Int) @unchecked) match {
          case (x1 @ ((v @ _), (i @ _))) => {
            val c: Int = i.+(2);
            scala.Tuple2.apply[(Int, Int), Int]((v, i), c)
          }
        }
      """.asInstanceOf[toolbox.u.Tree]).asInstanceOf[Tree]

      println(parseTupleRewiringMapClosure(f))
    }
  }

  @Test
  def scalarMappedToTuple {
    new TuploidValues {
      override val global = scala.reflect.runtime.universe
      import global._
      import scala.reflect.runtime.currentMirror
      import scala.tools.reflect.ToolBox
      val toolbox = currentMirror.mkToolBox()

      val f = toolbox.typeCheck(q"""
        (array: Array[Int]) => {
          val length: Int = array.length.*(30);
          scala.Tuple2.apply[Array[Int], Int](array, length)
        }
      """.asInstanceOf[toolbox.u.Tree]).asInstanceOf[Tree]

      println(parseTupleRewiringMapClosure(f))
    }

  }
}
