*Not in a usable state right now !*

This is an experimental rewrite of ScalaCL / Scalaxy using Scala 2.10 and its powerful macro system :

*   Will eventually support all the rewrites from ScalaCL 0.2
*   Natural expression of rewrite patterns and replacements that makes it easy to extend the plugin
*   Support for AOP-style rewrites

To compile a file test.scala using the compiler plugin, use [paulp's sbt script](https://github.com/paulp/sbt-extras) :

    sbt -sbt-snapshot "run test.scala"
    
The rewrites are defined in `Core/src/main/scala/scalaxy/rewrites` and look like this :

	import scalaxy.Macros._
	object ForLoops {
	  def simpleForeachUntil[U](start: Int, end: Int, body: U) = Replacement(
		for (i <- start until end) body,
		{
		  var ii = start
		  while (ii < end) {
			val i = ii
			body
			ii = ii + 1  
		  }
		}
	  )
	}

