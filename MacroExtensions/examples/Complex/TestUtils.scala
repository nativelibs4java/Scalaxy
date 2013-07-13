
object TestUtils {
    def time[V](title: String, n: Long, loops: Int)(b: => V): Double = {
        val startTime = System.nanoTime
        var i = 0
        while (i < loops) {
            b
            i += 1
        }
        val time = System.nanoTime - startTime
        val timePerLoop = time / loops
        val timePerItem = timePerLoop / n.toDouble
        //System.err.println("Time[" + title + "] = " + (time / 1000000L) + " milliseconds (" + (timePerLoop / 1000L) + " microseconds per iteration, " + timePerItem + " nanoseconds per item)")
        if (title != null)
            System.err.println(title + " = " + timePerItem + " nanoseconds per item)")
        
        timePerItem
    }
    def tst(count: Long, n: String = null)(b: => Unit) = {
        // Run cold code
        val suffix = if (n == null) "" else "[" + n + "]"
        val cold = time("Cold" + suffix, count, 1000) { b }
        // Finish warmup : (1000 + 3000 > hotspot threshold both in server and client modes)
        time(null, count, 3000) { b }
        val warm = time("Warm" + suffix, count, 10000) { b }
        (cold, warm)
    }
}
