# Scalaxy/Reified

Simple reified values / functions framework (leverages Scala 2.10 macros).

Package `scalaxy.reified` provides a `reify` method that goes beyond the stock `Universe.reify` method, by taking care of captured values and allowing composition of reified functions for improved flexibility of dynamic usage of ASTs. 
The original expression is also available at runtime, without having to compile it with `ToolBox.eval`.

This is still highly experimental, documentation will come soon enough.

```scala
import scalaxy.reified._

def comp(capture1: Int): ReifiedFunction1[Int, Int] = {
  // Capture of arrays is TODO
  val capture2 = Seq(10, 20, 30)
  val f = reify((x: Int) => capture1 + capture2(x))
  val g = reify((x: Int) => x * x)
  
  g.compose(f)
}

println(comp(10).expr.tree)
println(comp(100).expr.tree)
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
    
# Hacking

If you want to build / test / hack on this project:
- Make sure to use [paulp's sbt script](https://github.com/paulp/sbt-extras) with `sbt` 0.12.2+
- Use the following commands to checkout the sources and build the tests continuously: 

    ```
    git clone git://github.com/ochafik/Scalaxy.git
    cd Scalaxy
    sbt "project scalaxy-reified" "; clean ; ~test"
    ```

