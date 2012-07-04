This is a simple prototype to port ScalaCL / Scalaxy's loop rewrites to Scala 2.10.

The goal is to integrate it into Scala, which should be relatively easy.

Please use [paulp/sbt-extras](https://github.com/paulp/sbt-extras)'s [sbt script](https://raw.github.com/paulp/sbt-extras/master/sbt).

Run with:

    sbt ~test
    
(you should be get a "failed optimization" compilation warning, and see two series of 0 to 10 println)
    
Current prototype only aims at optimizing Range loops, and do it well.

Later (i.e. after InlinableRange is integrated to Scala / merged into Range), one might add Array.foreach and other pin-pointed optimizations (see which optimizations [ScalaCL supported](https://code.google.com/p/scalacl/wiki/ScalaCLPlugin#General_optimizations), for instance). 

Notes:
- The magic of macros-as-methods means we hardly need to check the target type / symbols in tree matchers (we know that self is)

