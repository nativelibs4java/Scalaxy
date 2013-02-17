*Compilets* are a rewrite of [ScalaCL / Scalaxy](http://code.google.com/p/scalacl/) using Scala 2.10.0 and its powerful macro system that provide:
- Natural expression of rewrite patterns and replacements that makes it easy to express rewrites
- Will eventually support all the rewrites from ScalaCL 0.2, and more
- Easy to express AOP-style rewrites (to add or remove logs, runtime checks, etc...)
- Add your own warnings and errors to scalac in a few lines!

([BSD-licensed](https://github.com/ochafik/Scalaxy/blob/master/LICENSE))

# Usage

The preferred way to use Scalaxy/Compilets is with Sbt 0.12.2 and the [sbt-scalaxy](http://github.com/ochafik/sbt-scalaxy) Sbt plugin, but the `Examples` subfolder demonstrates how to use it [with Maven or with Sbt but without `sbt-scalaxy`](https://github.com/ochafik/Scalaxy/tree/master/Examples/UsageWithMavenOrWithoutSbtPlugin). 

To compile your Sbt project with Scalaxy's compiler plugin and default compilets:
*   Put the following in `project/plugins.sbt` (or in `~/.sbt/plugins/build.sbt` for global setup):

    ```scala
    resolvers += Resolver.sonatypeRepo("snapshots")
    
    addSbtPlugin("com.nativelibs4java" % "sbt-scalaxy" % "0.3-SNAPSHOT")
    ```

*   Make your `build.sbt` look like this:

    ```scala
    scalaVersion := "2.10.0"
    
    autoCompilets := true
    
    addDefaultCompilets()
    ```

See a full example in [Scalaxy/Examples/UsageWithSbtPlugin](https://github.com/ochafik/Scalaxy/tree/master/Examples/UsageWithSbtPlugin).

To see what's happening:

	SCALAXY_VERBOSE=1 sbt clean compile
	
Or to see the code after it's been rewritten during compilation:

	scalacOptions += "-Xprint:scalaxy-rewriter"

# Creating your own Compilets

It's very easy to define your own compilets to, say, optimize your shiny DSL's overhead away, or enforce some corporate coding practices (making any call to `Thread.stop` a compilation error, for instance).

This is very easy to do, please have a look at `Examples/CustomCompilets` and `Examples/DSLWithOptimizingCompilets`.

# Hacking

To build the sources and compile a file test.scala using the compiler plugin, use [paulp's sbt script](https://github.com/paulp/sbt-extras) :

    sbt "run Test/test.scala"

To see what's happening, you might want to print the AST before and after the rewrite :

    sbt "run Test/test.scala -Xprint:typer -Xprint:scalaxy-rewriter"
    
The rewrites are defined in `Compilets` and look like this :

```scala
import scalaxy.compilets._
import scalaxy.compilets.matchers._

object SomeExamples {

  def simpleForeachUntil[U](start: Int, end: Int, body: Int => U) = replace(
    for (i <- start until end) 
        body(i),
    {
      var ii = start; val ee = end
      while (ii < ee) {
        val i = ii
        body(i)
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
```

Here's how to run tests:

	sbt clean test
	
To deploy to Sonatype (assuming ~/.sbt/0.12.2/sonatype.sbt contains the correct credentials), then advertise a release on ls.implicit.ly:

	sbt "+ assembly" "+ publish"
	sbt "project scalaxy" ls-write-version lsync

