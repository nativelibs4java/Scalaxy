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

If you're using `sbt` 0.12.2+, just put the following lines in `build.sbt`:
```scala
scalaVersion := "2.10.2"

// Dependency at compilation-time only (not at runtime).
libraryDependencies += "com.nativelibs4java" %% "scalaxy-reified" % "0.3-SNAPSHOT" % "provided"

// Scalaxy/Reified snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
```

# Why?

To make it easy to deal with dynamic computations that could benefit from re-compilation at runtime for optimization purposes, or from conversion to other forms of executables (e.g. conversion to SQL, to OpenCL with ScalaCL, etc...).

For instance, let's say you have a complex financial derivatives valuation framework. It depends on lots of data (eventually stored in arrays and maps, e.g. dividend dates and values), which are fetched dynamically by your program, and it is composed of many pieces that can be assembled in many different ways (you might have several valuation algorithms, several yield curve types, and so on).
If each of these pieces returns a reified value (an instanceof `ReifiedValue[_]` returned by the `scalaxy.reified.reify` method, e.g. a `ReifiedValue[(Date, Map[Product, Double]) => Double]`), then thanks to reified values being composable your top level will be able to return a reified value as well, which will be a function of, say, the evaluation date, and maybe a map of market data bumps.
You can evaluate that function straight away, since every reified value holds the original value: evaluation will then be classically dynamic, with functions calling functions and all.
Or... if you need better performance from that function (which your program might call thousands of times), you can fetch that function's AST, compile it _at runtime_ with a `scala.tool.ToolBox` and get a fresh function with the same signature, but with all the static analysis optimizations the compiler was able to shove in. 

More detailed examples will hopefully come soon...

# How can it be faster?

## Caveat: compilation overhead

First off, re-compiling and evaluating reified functions is only faster if you indent to evaluate them enough times to overcome the overhead of firing the compiler (expect around 70 ms to compile one of the example ASTs below on a MacBook Pro 2012 with Scala 2.10.2).

## A silly example

That said, take the following example:
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

If you ignore the `reify` calls, this is some straightforward function composition code, which results in 3 distinct function objects being instantiated.
Calling `f` with an index value triggers a dynamic execution flow that roughly does `square.apply(getter.apply(index))`. 

Keep in mind that `getter` and `square` could come from outside this code and could not have the same value at every call. `Function1.apply` being megamorphic, it's likely that the JVM will not optimize much of these composition calls, keeping some indirection at every step.

Now what does `reify` do? It preserves the AST of its argument and its captured values in a way that allows for runtime composition and inlining.
In the case of `f`, here's the AST in `f.taggedExpr`:
```scala
Expr[A](
  (c: Int) => CaptureTag(square, 0).apply(CaptureTag(getter, 1).apply(c))
)
```
Which refers to two captured values, present in `f.capturedTerms`:
```scala
List(
  (
    // AST from square:
    ReifiedFunction1(
      <function1>, // the original pure-Scala `square` function object
      taggedExpr = ((x: Int) => x * x),
      capturedTerms = Nil)
    -> typeOf[ReifiedFunction1[Int,Int]]
  ),
  (
    // AST from getter:
    ReifiedFunction1(
      <function1>, // the original pure-Scala `getter` function object
      taggedExpr = ((index: Int) => CaptureTag(offset, 0) + CaptureTag(values, 1).apply(index))),
      capturedTerms = List(
        10 -> typeOf[Int],
        [I@66c0046 -> typeOf[Array[Int]])
    -> typeOf[ReifiedFunction1[Int,Int]]
  )
)
```
As you can see, these captured values are themselves reified values which also have an AST and captured values.

When we want to compile `f` into `ff`, Scalaxy/Reified first flattens the hierarchy of captured reified values, which gives the following AST (order of captures is reshuffled):
```scala
{
  val capture$0 = ((x: Int) => x.*(x));
  val capture$1: Int = 10;
  val capture$2: Array[Int] = Array(10, 20, 30)
  val capture$3 = (index: Int) => capture$1 + capture$2.apply(index)
  ((c: Int) => capture$0.apply(capture$3.apply(c)))
}
```
You can see that captured constants (ints, arrays, and functions) are inlined in the code. They have been converted from runtime value to AST representations amenable for compilation, so that this AST has no external dependency apart from classes, stable objects and methods.

Now comes the magic: Scalaxy/Reified performs some optimizations on the AST. 

First, you can see in that previous AST that function values are always used as methods (i.e. nobody calls their `hashCode` or `compose` methods). After a quick static analysis, Scalaxy/Reified produces the following equivalent AST:
```scala
{
  def capture$0(x: Int): Int = x.*(x);
  val capture$1: Int = 10;
  val capture$2: Array[Int] = scala.Array.apply[Int](10, 20, 30)(scala.reflect.ClassTag.Int);
  def capture$3(index: Int): Int = capture$1.+(capture$2.apply(index));
  ((c: Int) => capture$0(capture$3(c)))
}
```
The advantage of this form is that the Scala runtime compiler (a [ToolBox](http://www.scala-lang.org/archives/downloads/distrib/files/nightly/docs-2.10.2/compiler/index.html#scala.tools.reflect.ToolBox) with optimization flags like `-inline`) will be able to inline `capture$0(capture$3)` (it wasn't able to inline `capture$0.apply(capture$3.apply(c))`).

This alone can produce 10x speed improvements if your functions have a small payload!

And this isn't it! Scalaxy/Reified also optimizes inlined Range.foreach loops away, akin to what Scalaxy/Loops does but without the need to write the pesky `optimized` suffix.

## An almost useful example

The following integrator code:
```scala
import scalaxy.reified._
def createDiscreteIntegrator(f: ReifiedValue[Double => Double]): ReifiedValue[(Int, Int) => Double] = {
  reify((start: Int, end: Int) => {
    var tot = 0.0
    for (i <- start until end) {
      tot += f(i)
    }
    tot
  })
}
import scala.math._
val fIntegrator = createDiscreteIntegrator(reify(v => {
  cos(v / Pi) * exp(v)
}))
```
Get optimized to:
```scala
{
  def capture$0(v: Double): Double = 
    scala.math.cos(v / scala.math.Pi) * scala.math.exp(v)
    
  (start: Int, end: Int) => {
    var tot: Double = 0.0;
    var i$1: Int = start;
    val end$1: Int = end;
    val step$1: Int = 1;
    while(i$1 < end$1) {
      val i = i$1;
      tot += capture$0(i);
      i$1 += step$1
    }
    tot
  }
}
```

In short, with Scalaxy/Reified you combine the flexibility of dynamic composition and the speed of highly-optimized static compilation.

# TODO

- Add many more tests
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

