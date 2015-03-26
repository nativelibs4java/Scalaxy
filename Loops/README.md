# Scalaxy/Loops

Optimized Range foreach loops for Scala (using a macro to rewrite them to an equivalent while loop).
([BSD-licensed](https://github.com/ochafik/Scalaxy/blob/master/LICENSE))

Works with Scala 2.10.x and Scala 2.11.x, but 2.11.x users are kindly advised to migrate to [Scalaxy/Streams](https://github.com/ochafik/Scalaxy/blob/master/Streams).

**Caveats**:
* Experimental material: use at your own risk (and avoid using in your tests :-D).
* The Scala 2.10.x variant has some ad-hoc Range foreach loop optimization code.
* The Scala 2.11.x variant leverages [Scalaxy/Streams](https://github.com/ochafik/Scalaxy/blob/master/Streams) in a non-recursive mode (which means only the stream in which the `optimized` range loop is involved is optimized). To get the full power of Scalaxy/Streams, please use it directly (this version is here to allow cross-compilation of 2.10.x code written with Scalaxy/Loops).

The following expression:
```scala
import scalaxy.loops._
import scala.language.postfixOps // Optional.

for (i <- 0 until 100000000 optimized) {
  ...
}
```
Will get rewritten at compilation time into something like:
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

If you're using `sbt` 0.13.0+, just put the following lines in `build.sbt`:
```scala
scalaVersion := "2.11.6"
// Or:
// scalaVersion := "2.10.4"

// Dependency at compilation-time only (not at runtime).
libraryDependencies += "com.nativelibs4java" %% "scalaxy-loops" % "0.3.3" % "provided"

// If you care about speed, you may want to enable these:
// scalacOptions ++= Seq("-optimise", "-Yclosure-elim", "-Yinline")
```

And append `optimized` to all the ranges you want to optimize:
```scala
for (i <- 0 until n optimized; j <- i until n optimized) {
  ...
}
```

If you like to live on the bleeding edge, try the latest snapshot out:
```scala
libraryDependencies += "com.nativelibs4java" %% "scalaxy-loops" % "0.4-SNAPSHOT" % "provided"

// Scalaxy snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
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
  <dependency>
    <groupId>com.nativelibs4java</groupId>
    <artifactId>scalaxy-loops_2.11</artifactId>
    <!-- Or:
    <artifactId>scalaxy-loops_2.10</artifactId>
    -->
    <version>0.3.3</version>
  </dependency>
</dependencies>
```

If you like to live on the bleeding edge, try the latest snapshot out:
```xml
<dependencies>
  <dependency>
    <groupId>com.nativelibs4java</groupId>
    <artifactId>scalaxy-loops_2.11</artifactId>
    <!-- Or:
    <artifactId>scalaxy-loops_2.10</artifactId>
    -->
    <version>0.4-SNAPSHOT</version>
  </dependency>
</dependencies>

<repositories>
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

  Or for the Scala 2.10.x version:

    ```
    cd Scalaxy
    sbt "project scalaxy-loops-210" "; +clean ; +test"
    ```

# What's next?

No further optimizations will be specifically added to Scalaxy/Loops. Instead, work is now ongoing in [Scalaxy/Streams](https://github.com/ochafik/Scalaxy/blob/master/Streams), which Scalaxy/Loops delegates its optimizations to in its 2.11.x version.

Please [file bugs and enhancement requests here](https://github.com/ochafik/Scalaxy/issues/new).

Any help (testing, patches, bug reports) will be greatly appreciated!
