import scala.language.postfixOps
import scalaxy.loops._

object TestIntRangeLoopsOptimized {
    import TestUtils._
    
    def main(args: Array[String]): Unit = {
        val name = if (args.isEmpty) "Normal" else args(0)
        val m = 10
        val n = 10
        val o = 30
        val mn = m * n 
        val mno = m * n * o
        def test1_mno = {
            var t = 0.0
            for (i <- 0 until mno optimized)
                    t += i / 10
            t
        }
        def test2_mn_o = {
            var t = 0.0
            for (i <- 0 until mn optimized)
                for (j <- 0 until o optimized)
                    t += (i + j) / 10
            t
        }
        def test3_mno = {
            var t = 0.0
            for (i <- 0 until n optimized)
                for (j <- 0 until m optimized)
                    for (k <- 0 until o optimized)
                        t += (i + j + k) / 10
            t
        }
        def test4_mnom = {
            var t = 0.0
            for (i <- 0 until n optimized)
                for (j <- 0 until m optimized)
                    for (k <- 0 until o optimized)
                        for (l <- 0 until m optimized)
                            t += (i + j + k + l) / 10
            t
        }
        val (cold1, warm1) = tst(mno) { test1_mno } 
        val (cold2, warm2) = tst(mno) { test2_mn_o } 
        val (cold3, warm3) = tst(mno) { test3_mno }
        val (cold4, warm4) = tst(mno * m) { test4_mnom }
        
        println(Array(name, "Cold", 1, cold1).mkString("\t"));
        println(Array(name, "Cold", 2, cold2).mkString("\t"));
        println(Array(name, "Cold", 3, cold3).mkString("\t"));
        println(Array(name, "Cold", 4, cold4).mkString("\t"));
        println(Array(name, "Warm", 1, warm1).mkString("\t"));
        println(Array(name, "Warm", 2, warm2).mkString("\t"));
        println(Array(name, "Warm", 3, warm3).mkString("\t"));
        println(Array(name, "Warm", 4, warm4).mkString("\t"));
    }
}
