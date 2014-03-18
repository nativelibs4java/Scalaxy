# Scalaxy/Streams

Scalaxy/Streams makes your Scala collections code faster:
* Fuses collection streams down to while loops
* Avoids unnecessary tuples
* Usable as a compiler plugin (whole project) or as a macro (surgical strikes)

# TODO

* Null tests for tuple unapply withFilter calls
* Fix `a pure expression does nothing in statement position` warnings.
* Publish artifacts
* Performance tests, including peak memory usage measurements:

  ```scala

  import java.lang.management.{ ManagementFactory, MemoryMXBean, MemoryPoolMXBean }
  import collection.JavaConversions._

  for (pool <- ManagementFactory.getMemoryPoolMXBeans) {
    println(String.format("%s: %,d", pool.getName, pool.getPeakUsage.getUsed.asInstanceOf[AnyRef]))
  }
  ```

* Test plugin as well as macros
* Support @optimize(true) / @optimize(false) / -Dscalaxy.streams.optimize=true/false/never/always

