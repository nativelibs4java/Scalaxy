*Not in a usable state right now !*

This is an experimental rewrite of [ScalaCL / Scalaxy](http://code.google.com/p/scalacl/) using Scala 2.10 and its powerful macro system (tested with Scala 2.10.0-M2 and the snapshot from 20120409) :

*   Natural expression of rewrite patterns and replacements that makes it easy to express rewrites
*   Will eventually support all the rewrites from ScalaCL 0.2, and more
*   Easy to express AOP-style rewrites (to add or remove logs, runtime checks, etc...)
*   Will support easy warnings and errors

To compile a file test.scala using the compiler plugin, use [paulp's sbt script](https://github.com/paulp/sbt-extras) :

    sbt -sbt-snapshot "run test.scala"

To see what's happening, you might want to print the AST before and after the rewrite :

    sbt -sbt-snapshot "run test.scala -Xprint:typer -Xprint:scalaxy-rewriter"
    
The rewrites are defined in `Rewrites` and look like this :

	import scalaxy.macros._
	import scalaxy.matchers._
	
	object SomeExamples {
	
	  def simpleForeachUntil[U](start: Int, end: Int, body: U) = Replacement(
		for (i <- start until end) 
			body,
		{
		  var ii = start
		  while (ii < end) {
			val i = ii
			body
			ii = ii + 1  
		  }
		}
	  )
		
	  def forbidThreadStop(t: Thread) = 
		fail("You must NOT call Thread.stop() !") {
		  t.stop
		}
	  
	  def warnAccessibleField(f: java.lang.reflect.Field, b: Boolean) =
		when(f.setAccessible(b))(b) {
		  case True() :: Nil =>
			warning("You shouldn't do that")
		}
	}

