# Scalaxy/Ranges

Optimized Range foreach loops (using a macro to rewrite them to an equivalent while loop).

The following expression:
```scala
import scalaxy.ranges._
    
for (i <- 0 until 100000000 optimized) { ... }
```
Gets replaced at compile time by:
```scala
{
  var ii = 0
  val end = 100000000
  val step = 1
  while (ii < end) {
    val i = ii
    ...
    ii += step
  }
}
```
    
This is a rejuvenation of some code initially written for [ScalaCL](http://scalacl.googlecode.com/) then for [optimized-loops-macros](https://github.com/ochafik/optimized-loops-macros).

# Usage

If you're using `sbt` 0.12.2+, just put the following lines in `build.sbt`:
```scala
// Only works with 2.10.0+
scalaVersion := "2.10.0"

// Dependency at compilation-time only (not at runtime).
libraryDependencies += "com.nativelibs4java" %% "scalaxy-ranges" % "0.3-SNAPSHOT" % "provided"

// Scalaxy/Beans snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
```
    
# Hacking

If you want to build / test / hack on this project:
- Make sure to use [paulp's sbt script](https://github.com/paulp/sbt-extras) with `sbt` 0.12.2+
- Use the following commands to checkout the sources and build the tests continuously: 

    ```
    git clone git://github.com/ochafik/Scalaxy.git
    cd Scalaxy
    sbt "project scalaxy-ranges" "; clean ; ~test"
    ```

