*Still very experimental, don't rely on this yet!* (if you need fast loops now, [check this out](https://github.com/ochafik/optimized-loops-macros))

This is an experimental rewrite of [ScalaCL / Scalaxy](http://code.google.com/p/scalacl/) using Scala 2.10.0-RC1 and its powerful macro system.

Key features:
*   Natural expression of rewrite patterns and replacements that makes it easy to express rewrites
*   Will eventually support all the rewrites from ScalaCL 0.2, and more
*   Easy to express AOP-style rewrites (to add or remove logs, runtime checks, etc...)
*   Will support easy warnings and errors

# Usage

To compile your sbt project with Scalaxy's compiler plugin and default compilets, make sure your `build.sbt` file looks like this:

	scalaVersion := "2.10.0-RC1"
	
	resolvers += Resolver.sonatypeRepo("snapshots")
	
	autoCompilerPlugins := true
	
	addCompilerPlugin("com.nativelibs4java" %% "scalaxy" % "0.3-SNAPSHOT")
	
	scalacOptions += "-Xplugin-require:Scalaxy"

(please use sbt 0.12.1 or later)

To see what's happening:

	SCALAXY_VERBOSE=1 sbt
	
Or to see the code after it's been rewritten during compilation:

	scalacOptions += "-Xprint:scalaxy-rewriter"
	
# Hacking

To build the sources and compile a file test.scala using the compiler plugin, use [paulp's sbt script](https://github.com/paulp/sbt-extras) :

    sbt "run Test/test.scala"

To see what's happening, you might want to print the AST before and after the rewrite :

    sbt "run Test/test.scala -Xprint:typer -Xprint:scalaxy-rewriter"
    
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

Here's how to run tests:

	sbt clean test
	
To deploy to Sonatype (assuming ~/.sbt/0.12.1/sonatype.sbt contains the correct credentials), then advertise a release on ls.implicit.ly:

	sbt assembly publish
	sbt "project scalaxy" ls-write-version lsync

