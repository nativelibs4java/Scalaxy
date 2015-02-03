# Scalaxy/Reified

Simple reified values / functions framework that leverages Scala 2.10 macros ([Scaladoc](http://ochafik.github.io/Scalaxy/Reified/latest/api/index.html)).

Package `scalaxy.reified` provides a `reify` method that goes beyond the stock `Universe.reify` method, by taking care of captured values and allowing composition of reified functions for improved flexibility of dynamic usage of ASTs.
The original expression is also available at runtime, without having to compile it with `ToolBox.eval`.

This is still highly experimental, documentation will come soon enough.

```scala
import scalaxy.reified._

def comp(capture1: Int): ReifiedFunction1[Int, Int] = {
  val capture2 = Seq(10, 20, 30)
  val f = reify((x: Int) => capture1 + capture2(x))
  val g = reify((x: Int) => x * x)

  g.compose(f)
}

val f = comp(10)
// Normal evaluation, using regular function:
println(f(1))

// Get the function's AST, inlining all captured values and captured reified values:
val ast = f.expr().tree
println(ast)

// Compile the AST at runtime (needs scala-compiler.jar in the classpath):
val compiledF = ast.compile()()
// Evaluation, using the freshly-compiled function:
println(compiledF(1))
```

# Usage

If you're using `sbt` 0.13.0+, just put the following lines in `build.sbt`:
```scala
scalaVersion := "2.11.5"

// Dependency at compilation-time only (not at runtime).
libraryDependencies += "com.nativelibs4java" %% "scalaxy-reified" % "0.3-SNAPSHOT"

// Avoid sbt-related macro classpath issues.
fork := true

// Scalaxy/Reified snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
```

# Why?

To make it easy to deal with dynamic computations that could benefit from re-compilation at runtime for optimization purposes, or from conversion to other forms of executables (e.g. conversion to SQL, to OpenCL with ScalaCL, etc...).

For instance, let's say you have a complex financial derivatives valuation framework. It depends on lots of data (eventually stored in arrays and maps, e.g. dividend dates and values), which are fetched dynamically by your program, and it is composed of many pieces that can be assembled in many different ways (you might have several valuation algorithms, several yield curve types, and so on).
If each of these pieces returns a reified value (an instanceof `Reified[_]` returned by the `scalaxy.reified.reify` method, e.g. a `Reified[(Date, Map[Product, Double]) => Double]`), then thanks to reified values being composable your top level will be able to return a reified value as well, which will be a function of, say, the evaluation date, and maybe a map of market data bumps.
You can evaluate that function straight away, since every reified value holds the original value: evaluation will then be classically dynamic, with functions calling functions and all.
Or... if you need better performance from that function (which your program might call thousands of times), you can fetch that function's AST, compile it _at runtime_ with a `scala.tool.ToolBox` and get a fresh function with the same signature, but with all the static analysis optimizations the compiler was able to shove in.

More detailed examples will hopefully come soon...

# How can it be faster?

## Caveat: compilation overhead

First off, re-compiling and evaluating reified functions is only faster if you indent to evaluate them enough times to overcome the overhead of firing the compiler (expect around 70 ms to compile one of the example ASTs below on a MacBook Pro 2012 with Scala 2.10.2).

## A dummy integrator example

That said, let's take the following example:
```scala
import scalaxy.reified._
def createDiscreteIntegrator(f: Reified[Double => Double]): Reified[(Int, Int) => Double] = {
  reify((start: Int, end: Int) => {
    var tot = 0.0
    for (i <- start until end) {
      tot += f(i)
    }
    tot
  })
}
import scala.math._
val factor = 1 / Pi
val fIntegrator = createDiscreteIntegrator(reify(v => {
  cos(v * factor) * exp(v)
}))

println("fIntegrator.taggedExpr = " + fIntegrator.taggedExpr)
println("fIntegrator.capturedTerms " + fIntegrator.capturedTerms)

val fasterIntegrator = fIntegrator.compile()()
for ((start, end) <- Seq((0, 1), (0, 10))) {
  assert(fIntegrator(start, end) == fasterIntegrator(start, end))
}

```
(see [full code here](https://github.com/ochafik/Scalaxy/blob/master/Reified/src/test/scala/scalaxy/reified/READMEExamplesTest.scala))

## Let's ignore reification

If you ignore the `reify` calls, this is a straightforward higher-kinded function that creates a function integrator specialized for a particular input function `f`.

In the integrator's loop, each call to `f.apply` incurs some indirection, which the JVM might not optimize away.

## I can haz AST?

So what does `reify` do that is so special?
It creates a [`Reified[A]`](http://ochafik.github.io/Scalaxy/Reified/latest/api/index.html#scalaxy.reified.Reified), which preserves the original value, its AST and its captured values.

For instance, here's the AST of `fIntegrator`:
```scala
fIntegrator.taggedExpr = Expr[A](
  (start: Int, end: Int) => {
    var tot: Double = 0.0
    start.until(end).foreach((i: Int) => {
      tot += ReifiedFunction1[Double, Double](CaptureTag(f, 0).apply(i))
    }
    tot
  }
)
```
You can see it contains some special "capture tags" that clearly indicate where captured values go, using an index (here only `0`, since `fIntegrator` only has one direct capture: `f`).
Capture indices refer to the list of captured terms of the reified value:
```scala
fIntegrator.capturedTerms = List(
  (
    // Captured `f`:
    Reified(
      // The original pure-Scala function to integrate:
      value = <function1>,
      // The AST of that captured reified function:
      taggedExpr = (v: Double) => {
        scala.math.cos(v * CaptureTag(factor, 0)) * scala.math.exp(v)
      },
      // List of captures of that captured reified function:
      capturedTerms = List(
        // Captured `factor`:
        0.3183098861837907 -> typeOf[Double]
      )
    ) -> typeOf[Reified[Double => Double]
  )
)
```
As you can see, it is possible to capture reified values that capture reified values...

## Flattening AST and inlining captures

When we want to compile `fIntegrator` into `fasterIntegrator`, Scalaxy/Reified first flattens the hierarchy of captured reified values, which gives the following AST:
```scala
{
  val capture$0: Double = 0.3183098861837907;
  val capture$1 = (v: Double) => {
    scala.math.cos(v * capture$0) * scala.math.exp(v)
  }
  (start: Int, end: Int) => {
    var tot: Double = 0.0
    start.until(end).foreach((i: Int) => {
      tot += capture$1.apply(i.toDouble)
    }
    tot
  }
}
```
You can see that captured constants are inlined in the code (see [CaptureConversions](http://ochafik.github.io/Scalaxy/Reified/latest/api/index.html#scalaxy.reified.CaptureConversions$)).
They have been converted from runtime value to AST representations amenable for compilation. The resulting AST has no external value dependency (apart from stable paths &amp; methods).

## Rewriting ASTs for better performance

Now comes the magic: Scalaxy/Reified performs some optimizations on the AST.

### Promoting functional vals to defs

First, you can see in that previous AST that the function value `capture$1` is always used as a methods (i.e. nobody calls its `hashCode` or `compose` methods). After a quick static analysis, Scalaxy/Reified produces the following equivalent AST:
```scala
{
  val capture$0: Double = 0.3183098861837907;
  def capture$1(v: Double): Double = {
    scala.math.cos(v * capture$0) * scala.math.exp(v)
  }
  (start: Int, end: Int) => {
    var tot: Double = 0.0
    start.until(end).foreach((i: Int) => {
      tot += capture$1(i.toDouble)
    }
    tot
  }
}
```
The advantage of this form is that Scalaxy/Reified create a [ToolBox](http://www.scala-lang.org/archives/downloads/distrib/files/nightly/docs-2.10.2/compiler/index.html#scala.tools.reflect.ToolBox) with optimization flags like `-optimise -inline`, which might be able to inline the `capture$1` call away, which wasn't possible with the `val` version.

This alone can produce 10x speed improvements if your functions have a small payload and if you compose them a lot! (see [PerfTest](https://github.com/ochafik/Scalaxy/blob/master/Reified/src/test/scala/scalaxy/reified/PerfTest.scala))

### Rewriting loops

But if you've heard about [ScalaCL](http://scalacl.googlecode.com) and [Scalaxy](http://github.com/ochafik/Scalaxy) over the past years, you know this can't end here: Scalaxy/Reified includes some loop optimizations (currently only inlined `Range.foreach` calls, with more to come).

This gives us a final optimized AST:
```scala
{
  val capture$0: Double = 0.3183098861837907;
  def capture$1(v: Double): Double = {
    scala.math.cos(v * capture$0) * scala.math.exp(v)
  }
  (start: Int, end: Int) => {
    var tot: Double = 0.0;
    var i$1: Int = start;
    val end$1: Int = end;
    val step$1: Int = 1;
    while(i$1 < end$1) {
      val i = i$1;
      tot += capture$1(i);
      i$1 += step$1
    }
    tot
  }
}
```

## Wrapping it up



## Another, dull example

The following integrator code:
```scala

import scalaxy.reified._
def comp(offset: Int) = {
  val values = Array(10, 20, 30)
  val getter = reify((index: Int) => offset + values(index))
  val square = reify((x: Int) => x * x)
  square.compose(getter)
}
val f: ReifiedFunction1[Int, Int] = comp(10)
println(f.taggedExpr)
println(f.capturedTerms)

val ff = f.compile()()
for (index <- Seq(0, 1, 2)) {
  assert(f(index) == ff(index))
}
```
Will get optimized to:
```scala
{
  def capture$0(x: Int): Int = x.*(x);
  val capture$1: Int = 10;
  val capture$2: Array[Int] = scala.Array.apply[Int](10, 20, 30)(scala.reflect.ClassTag.Int);
  def capture$3(index: Int): Int = capture$1.+(capture$2.apply(index));
  ((c: Int) => capture$0(capture$3(c)))
}
```

In short, with Scalaxy/Reified you combine the flexibility of dynamic composition and the speed of highly-optimized static compilation.

# TODO

- Add many more tests
- Add conversion for `immutable.{ TreeSet, TreeMap, SortedSet, SortedMap }` captured values
- Implement a simple Romberg integrator
- Fix case where same term symbol might point to different values.
- Write an end-to-end usage example with benchmarks (maybe an algebraic expressions parser / compiler?)
- Fix `ReifiedFunction2.curried`
- Embed Scalaxy loop optimizations
- Provide a `ReifiedPartialFunction` wrapper with an `orElse` method that extracts match cases and recomposes a match that's optimizable by the compiler (blocked by [SI-6619](https://issues.scala-lang.org/browse/SI-6619))
- Handle case where some captured values refer to others (e.g. nested immutable collections)

# Hacking

If you want to build / test / hack on this project:
- Make sure to use [paulp's sbt script](https://github.com/paulp/sbt-extras) with `sbt` 0.12.2+
- Use the following commands to checkout the sources and build the tests continuously:

    ```
    git clone git://github.com/ochafik/Scalaxy.git
    cd Scalaxy
    sbt "project scalaxy-reified" "; clean ; ~test"
    ```

