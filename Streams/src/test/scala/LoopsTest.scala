package scalaxy.streams.test

import org.junit._
import org.junit.Assert._

class LoopsTest {
  import scalaxy.streams.optimize

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
      // def ff {
        val n = 3;
        println(for (v <- 0 to n) yield v)
      // }
      // for (i <- 0 to n) {
      //   println(i)
      // }
        
      // for (l <- 10L until 2L by -2) yield { l + 1 }


      // val a = Array(1, 2, 4)
      // a.map(_ + 2).map(_ * 10).filter(_ < 3)
      
      // val arrays = Array(Array(1, 2), Array(3, 4))

      // arrays.map(_.map(_ + 2).map(_ * 10).filter(_ < 3))

      // for ((a, i) <- Array(Array(1)).zipWithIndex; len = a.length; if len < i) {
      //   println(s"$a, $len, $i")
      // }


  
      // for (array <- arrays;
      //      length = array.length * 30;
      //      if length < 10;
      //      v <- array)
      //   yield
      //     (length, v)

      
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

}
