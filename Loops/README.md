# Scalaxy/Loops

Optimized loops for Scala 2.10 (using a macro to rewrite them to an equivalent while loop), currently limited to Range foreach loops.
([BSD-licensed](https://github.com/ochafik/Scalaxy/blob/master/LICENSE))

The following expression:
```scala
import scalaxy.loops._
import scala.language.postfixOps // Optional.
    
for (i <- 0 until 100000000 optimized) {
  ...
}
```
Gets rewritten at compilation time into:
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

(see [this blog post](http://ochafik.com/blog/?p=806) for a recap on the ScalaCL project rationale / story)

# Usage with Sbt

If you're using `sbt` 0.12.2+, just put the following lines in `build.sbt`:
```scala
// Only works with 2.10.0+
scalaVersion := "2.10.0"

// Dependency at compilation-time only (not at runtime).
libraryDependencies += "com.nativelibs4java" %% "scalaxy-loops" % "0.3-SNAPSHOT" % "provided"

// Scalaxy/Loops snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")

// This one usually doesn't hurt, but it may slow compilation down:
// scalacOptions += "-optimise"
```

And append `optimized` to all the ranges you want to optimize:
```scala
for (i <- 0 until n optimized; j <- i until n optimized) {
  ...
}
```

You can always disable loop optimizations without removing the `optimized` postfix operator from your code: just recompile with the environment variable `SCALAXY_LOOPS_OPTIMIZED=0` or the System property `scalaxy.loops.optimized=false` set:
```
SCALAXY_LOOPS_OPTIMIZED=0 sbt clean compile ...
```
Or if you're not using sbt:
```
scalac -J-Dscalaxy.loops.optimized=false ...
```

# Usage with Maven

With Maven, you'll need this in your `pom.xml` file:
```xml
<dependencies>
  ...
  <dependency>
    <groupId>com.nativelibs4java</groupId>
    <artifactId>scalaxy-loops_2.10</artifactId>
    <version>0.3-SNAPSHOT</version>
  </dependency>
</dependencies>

<repositories>
  ...
  <repository>
    <id>sonatype-oss-public</id>
    <url>https://oss.sonatype.org/content/groups/public/</url>
  </repository>
</repositories>
```

# Hacking

If you want to build / test / hack on this project:
- Make sure to use [paulp's sbt script](https://github.com/paulp/sbt-extras) with `sbt` 0.12.2+
- Use the following commands to checkout the sources and build the tests continuously: 

    ```
    git clone git://github.com/ochafik/Scalaxy.git
    cd Scalaxy
    sbt "project scalaxy-loops" "; clean ; ~test"
    ```

# What's next?

There's lots of work to reach the level of [ScalaCL 0.2](https://code.google.com/p/scalacl/wiki/ScalaCLPlugin), and that may never happen.

However, if there is a particular loop optimization that's very important to you, please let me know:
- [@ochafik on Twitter](http://twitter.com/ochafik)
- [NativeLibs4Java mailing-list](groups.google.com/group/nativelibs4java)

You can also [file bugs and enhancement requests here](https://github.com/ochafik/Scalaxy/issues/new).

Anyway, current plans are to support the following loop rewrites:
- Range.{ foreach, map } with filters
- Array.{ foreach, map } with filters

Any help (testing, patches, bug reports) will be greatly appreciated!
