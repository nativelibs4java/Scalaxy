This is a simple prototype to port ScalaCL / Scalaxy's loop rewrites to Scala 2.10.

The goal is to integrate it into Scala, which should be relatively easy.

Please use [paulp/sbt-extras](https://github.com/paulp/sbt-extras)'s [sbt script](https://raw.github.com/paulp/sbt-extras/master/sbt).

Run with:

    sbt ~test
    
(you should be get a "failed optimization" compilation warning, and see two series of 0 to 10 println)
    
Current prototype only aims at validating the macro approach, with the very limited scope of ranges with constant start, end and step parameters.

Once this works, you can expect:
- full Range foreach support (including non-constant bounds)
- _maybe_ Array.foreach support and other pin-pointed optimizations, but only after InlinableRange is integrated to Scala

