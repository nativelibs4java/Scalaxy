package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

import scala.collection.mutable.ListBuffer
import scala.tools.reflect.ToolBox
import scala.tools.reflect.FrontEnd

case class PerfRun(optimized: Boolean, output: Any, nanoTime: Long)

class PerformanceTestBase extends StreamComponentsTestBase {

  val perfRuns = 4
  val defaultExpectedFasterFactor = 0.95
  val testSizes = Array(2, 10, 1000, 100000)
  // val testSizes = Array(10)

  def testClassInfo = {
    val testTrace = new RuntimeException().getStackTrace.filter(se => se.getClassName.endsWith("Test")).last
    val testClassName = testTrace.getClassName
    val methodName = testTrace.getMethodName
    (testClassName, methodName)
  }

  type PerformanceRunner = Int => (Boolean => PerfRun)

  private def getPerformanceRunner(
      optimized: Boolean,
      decls: String,
      code: String,
      sizeParam: String = "n"): PerformanceRunner =
  {
    if (optimized) println(code)

    val functionCode = s"""
      ($sizeParam: Int) => {
        $decls;
        () => ${if (optimized) optimizedCode(code, scalaxy.streams.strategy.default) else code}
      }
    """

    val (compiled, messages) = try {
      compile(functionCode)
    } catch { case ex: Throwable =>
      // ex.printStackTrace()
      throw new RuntimeException(s"Failed to compile:\n$functionCode", ex)
    }
    val sizeToExecutor = compiled().asInstanceOf[Int => (() => Any)]

    (n: Int) => {
      val executor = sizeToExecutor(n)
      (isWarmup: Boolean) => {
        if (isWarmup) {
          executor()
          null
        } else {
          System.gc
          Thread.sleep(50)
          val start = System.nanoTime
          val output = executor().asInstanceOf[AnyRef]
          val nanoTime = System.nanoTime - start
          PerfRun(optimized, output = output, nanoTime = nanoTime)
        }
      }
    }
  }

  def ensureFasterCodeWithSameResult(
      decls: String,
      code: String,
      params: Seq[Int] = testSizes,
      minFaster: Double = 1.0,
      nRuns: Int = perfRuns): Unit = {

    val (testClassName, methodName) = testClassInfo

    val runners @
      Array(
        optimizedRunner,
        normalRunner
      ) = Array(
        getPerformanceRunner(optimized = true, decls = decls, code = code),
        getPerformanceRunner(optimized = false, decls = decls, code = code)
      )

    def run = params.toList.sorted.map(param => {
      val testers = runners.map(_(param))
      val firstRun = testers.map(_(false))

      val Array(optimizedOutput, normalOutput) = firstRun.map(_.output)

      val pref = "[" + testClassName + "." + methodName + ", n = " + param + "] "
      if (normalOutput != optimizedOutput) {
        fail(pref + "ERROR: Output is not the same !\n" + pref + "\t   Normal output = " + normalOutput + "\n" + pref + "\tOptimized output = " + optimizedOutput)
      }

      val runs: List[PerfRun] = firstRun.toList ++ (1 until nRuns).toList.flatMap(_ => testers.map(_(false)))
      def calcTime(list: List[PerfRun]) = {
        val times = list.map(_.nanoTime)
        times.sum / times.size.toDouble
      }
      val (runsOptimized, runsNormal) = runs.partition(_.optimized)
      val (timeOptimized, timeNormal) = (calcTime(runsOptimized), calcTime(runsNormal))

      (param, timeNormal / timeOptimized)
    }).toMap

    // val (logName, log, properties) = Results.getLog(testClassName)

    //println("Cold run...")
    val coldRun = run

    //println("Warming up...");
    // Warm up the code being benchmarked :
    {
      val testers = runners.map(_(5))
      (0 until 2500).foreach(_ => testers.foreach(_(true)))
    };

    //println("Warm run...")
    val warmRun = run

    val errors = coldRun.flatMap { case (param, coldFactor) =>
      val warmFactor = warmRun(param)
      //println("coldFactor = " + coldFactor + ", warmFactor = " + warmFactor)

      def f2s(f: Double) = ((f * 10).toInt / 10.0) + ""
      def printFacts(warmFactor: Double, coldFactor: Double) = {
        val txt = methodName + "\\:" + param + "=" + Array(warmFactor, coldFactor).map(f2s).mkString(";")
        //println(txt)
        println(txt)
      }
      //def printFact(factor: Double) = log.println(methodName + "\\:" + param + "=" + f2s(factor))
      val (expectedWarmFactor, expectedColdFactor) = {
        (defaultExpectedFasterFactor, defaultExpectedFasterFactor)
        // val p = Option(properties.getProperty(methodName + ":" + param)).map(_.split(";")).orNull
        // if (p != null && p.length == 2) {
        //   //val Array(c) = p.map(_.toDouble)
        //   //val c = p.toDouble; printFact(c); c
        //   //log.print("# Test result (" + (if (actualFasterFactor >= f) "succeeded" else "failed") + "): ")
        //   val Array(w, c) = p.map(_.toDouble)
        //   printFacts(w, c)
        //   (w, c)
        // } else {
        //   //printFact(coldFactor - 0.1); 1.0
        //   printFacts(warmFactor - 0.1, coldFactor - 0.1)
        //   (defaultExpectedFasterFactor, defaultExpectedFasterFactor)
        // }
      }

      def check(warm: Boolean, factor: Double, expectedFactor: Double) = {
        val pref = "[" + testClassName + "." + methodName + ", n = " + param + ", " + (if (warm) "warm" else "cold") + "] "

        if (factor >= expectedFactor) {
          println(pref + "  OK (" + factor + "x faster, expected > " + expectedFactor + "x)")
          Nil
        } else {
          val msg = "ERROR: only " + factor + "x faster (expected >= " + expectedFactor + "x)"
          println(pref + msg)
          List(msg)
        }
      }

      check(false, coldFactor, expectedColdFactor) ++
      check(true, warmFactor, expectedWarmFactor)
    }
    try {
      if (!errors.isEmpty)
        assertTrue(errors.mkString("\n"), false)
    } finally {
      println()
    }
  }
}
