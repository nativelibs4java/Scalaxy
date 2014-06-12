# Scalaxy/Streams

Scalaxy/Streams makes your Scala 2.11.x collections code faster (official heir to [ScalaCL](https://code.google.com/p/scalacl/) and [Scalaxy/Loops](https://github.com/ochafik/Scalaxy/tree/master/Loops), by same author):

* Fuses collection streams down to while loops (see [some examples](https://github.com/ochafik/Scalaxy/blob/master/Streams/src/test/scala/MacroIntegrationTest.scala#L55))
* Avoids many unnecessary tuples (for instance, those introduced by `zipWithIndex`).
* Usable as a compiler plugin (whole project) or as a macro (surgical strikes)

  **Watch out**: version 0.2's aggressive optimizations alter Scala semantics (see below).
  
  Use with caution / [report bugs](https://github.com/ochafik/Scalaxy/issues/new) (_safe_ optimizations are on the TODO-list below)

```scala
// For instance, given the following array:
val array = Array(1, 2, 3, 4)

// The following for comprehension:
for ((item, i) <- array.zipWithIndex; if item % 2 == 0) {
  println(s"array[$i] = $item")
}

// Is desugared by Scala to (slightly simplified):
array.zipWithIndex.withFilter((pair: (Int, Int)) => pair match {
  case (item: Int, i: Int) => true
  case _ => false
}).withFilter((pair: (Int, Int)) => pair match {
  case (item, i) =>
    item % 2 == 0
}).foreach((pair: (Int, Int)) => pair match {
  case (item, i) =>
    println(s"array[$i] = $item")
})
// Which will perform as badly and generate as many class files as you might fear.

// Scalaxy/Streams will simply rewrite it to something like:
val array = Array(1, 2, 3, 4)
var i = 0;
val length = array.length
while (i < length) {
  val item = array(i)
  if (item % 2 == 0) {
    println(s"array[$i] = $item")
  }
}
```

**Caveat**: Scalaxy/Streams is an **experimental work in progress**, so:

* Don't use it in production yet. If you insist on doing it, please test your code thoroughly and make sure your tests aren't compiled with it, maybe with something (untested) like this in your `build.sbt`:

        scalacOptions in Test += "-Xplugin-disable:scalaxy-streams"

* Be aware that *optimized code might behave differently* than normal code, especially with regards to side-effects: for instance, streams typically become lazy (akin to chained Iterators), so the optimization might change the number and order of side-effects (if there are any):

    ```scala
      (1 to 2).map(i => { println("first map, " + i); i })
              .map(i => { println("second map, " + i); i })
              .take(1)
      // Without optimizations, this will print:
      //   first map, 1
      //   first map, 2
      //   second map, 1
      //   second map, 2
    
      // With stream optimizations, this could *semantically* amount to the following:
      (1 to 2).toIterator
              .map(i => { println("first map, " + i); i })
              .map(i => { println("second map, " + i); i })
              .take(1)
              .toSeq
      // It will hence print:
      //   first map, 1
      //   second map, 1
    }
    ```
    
  A _safe_ optimization mode that retains the original Scala semantics is in the works, but focus for this release is to find bugs / crashes :-)
  
* If you're unsure about side effects in your streamed operations, just take it easy and introduce Scalaxy/Stream optimizations on a case-per-case basis, using its `optimized` macro (see below).
* If you try and run micro-benchmarks, don't forget to use the following scalac optimization flags:

        -optimise -Yclosure-elim -Yinline

# Scope

Scalaxy/Streams rewrites streams with the following components:

* Stream sources:
  * `Array`,
  * inline `Range`,
  * `Option` (with special case for explicit `Option(x)`),
  * explicit `Seq(a, b, ...)` (array-based rewrite),
  * `List` (with special array-based rewrite for explicit `List(a, b, ...)`)
* Stream operations:
  * `filter`,
  * `filterNot`,
  * `withFilter`,
  * `map`,
  * `flatMap` (with or without nested streams),
  * `zipWithIndex`
  * `sum`, `product`
  * `toList`, `toArray`, `toVector`, `toSet`

The output type of each optimized stream is always the same as the original, but when nested streams are encountered in `flatMap` operations many intermediate outputs can typically be skipped, saving up on memory usage and execution time.

# Usage

You can either use Scalaxy/Streams's compiler plugin to compile your whole project, or use its `optimize` macro to choose specific blocks of code to optimize.

You can always disable loop optimizations by recompiling with the environment variable `SCALAXY_STREAMS_OPTIMIZE=0` or the System property `scalaxy.streams.optimize=false` set:
```
SCALAXY_STREAMS_OPTIMIZE=0 sbt clean compile ...
```
Or if you're not using sbt:
```
scalac -J-Dscalaxy.streams.optimize=false ...
```

## Usage with Sbt

If you're using `sbt` 0.13.0+, just put the following lines in `build.sbt`:

* To use the macro (manually decide which parts of your code are optimized):

  ```scala
  // Note that Scalaxy/Streams 0.1 only works with Scala 2.11.0-RC1.
  scalaVersion := "2.11.1"

  // Dependency at compilation-time only (not at runtime).
  libraryDependencies += "com.nativelibs4java" %% "scalaxy-streams" % "0.2" % "provided"
  ```

  And wrap some code with the `optimize` macro:
  ```scala
  import scalaxy.streams.optimize
  optimize {
      for (i <- 0 until n; j <- i until n; if (i + j)  % 2 == 0) {
        ...
      }
  }
  ```

  You'll need an extra repository resolver for the latest snapshot out:
  ```scala
  libraryDependencies += "com.nativelibs4java" %% "scalaxy-streams" % "0.3-SNAPSHOT" % "provided"

  // Scalaxy snapshots are published on the Sonatype repository.
  resolvers += Resolver.sonatypeRepo("snapshots")
  ```

* To use the compiler plugin (optimizes all of your code):

  ```scala
  // Note that Scalaxy/Streams 0.1 only works with Scala 2.11.0-RC1.
  scalaVersion := "2.11.1"

  autoCompilerPlugins := true

  addCompilerPlugin("com.nativelibs4java" %% "scalaxy-streams" % "0.2")

  scalacOptions += "-Xplugin-require:scalaxy-streams"
  ```

  You'll need an extra repository resolver for the latest snapshot out:
  ```scala
  addCompilerPlugin("com.nativelibs4java" %% "scalaxy-streams" % "0.3-SNAPSHOT")

  // Scalaxy snapshots are published on the Sonatype repository.
  resolvers += Resolver.sonatypeRepo("snapshots")
  ```

And of course, if you're serious about performance you should add the following line to your `build.sbt` file:
```scala
scalacOptions += "-optimise -Yclosure-elim -Yinline"
```

## Usage with Maven

With Maven, you'll need this in your `pom.xml` file:

* To use the macro (manually decide which parts of your code are optimized):

  ```xml
  <dependencies>
    <dependency>
      <groupId>com.nativelibs4java</groupId>
      <artifactId>scalaxy-streams_2.11.1</artifactId>
      <version>0.2</version>
    </dependency>
  </dependencies>
  ```

  And to try the latest snapshot out:
  ```xml
  <dependencies>
    <dependency>
      <groupId>com.nativelibs4java</groupId>
      <artifactId>scalaxy-streams_2.11.1</artifactId>
      <version>0.3-SNAPSHOT</version>
    </dependency>
  </dependencies>

  <repositories>
    <repository>
      <id>sonatype-oss-public</id>
      <url>https://oss.sonatype.org/content/groups/public/</url>
    </repository>
  </repositories>
  ```

* To use the compiler plugin (optimizes all of your code):

  ```xml
  <properties>
    <scala.version>2.11.1</scala.version>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.scala-lang</groupId>
      <artifactId>scala-library</artifactId>
      <version>${scala.version}</version>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>net.alchim31.maven</groupId>
        <artifactId>scala-maven-plugin</artifactId>
        <version>3.1.6</version>
        <executions>
          <execution>
            <goals>
              <goal>compile</goal>
              <goal>testCompile</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <scalaVersion>${scala.version}</scalaVersion>
          <compilerPlugins>
            <compilerPlugin>
              <groupId>com.nativelibs4java</groupId>
              <artifactId>scalaxy-streams_${scala.version}</artifactId>
              <version>0.2</version>
            </compilerPlugin>
          </compilerPlugins>
        </configuration>
      </plugin>
    </plugins>
  </build>
  <repositories>
    <repository>
      <id>sonatype-oss-public</id>
      <name>Sonatype Snapshots</name>
      <url>https://oss.sonatype.org/content/groups/public/</url>
    </repository>
  </repositories>
  ```

## A note on architecture

Scalaxy/Streams is a rewrite of [ScalaCL](https://code.google.com/p/scalacl/) using the awesome new (and experimental) reflection APIs from Scala 2.10, and the awesome [quasiquotes](http://docs.scala-lang.org/overviews/macros/quasiquotes.html) from Scala 2.11.

The architecture is very simple: Scalaxy/Streams deals with... streams. A stream is comprised of:

* A stream source (e.g. `ArrayStreamSource`, `InlineRangeStreamSource`...)
* A list of 1 or more stream operations (e.g. `MapOp`, `FilterOp`...)
* A stream sink (e.g. `ListBufferSink`, `OptionSink`...)
Each of these three kinds of stream components is able to emit the equivalent code of the rest of the stream, and generally has a corresponding extractor to recognize it in a `Tree` (e.g. `SomeArrayStreamSource`, `SomeOptionSink`, `SomeFlatMapOp`...).

One particular operation, `FlatMapOp`, may contain nested streams, which allows for the chaining of complex for comprehensions:
```scala
val n = 20;
// The following for comprehension:
for (i <- 0 to n;
     ii = i * i;
     j <- i to n;
     jj = j * j;
     if (ii - jj) % 2 == 0;
     k <- (i + j) to n)
  yield { (ii, jj, k) }

// Is recognized by Scalaxy/Stream as the following stream:
// Range.map.flatMap(Range.map.withFilter.flatMap(Range.map)) -> IndexedSeq
```

Special care is taken of tuples, by representing input and output values of stream components as *tuploids* (a tuploid is recursively defined as either a scalar or a tuple of tuploids).

Careful tracking of input, transformation and output of tuploids across stream components allows to optimize unneeded tuples away, while materializing or preserving needed ones (making `TransformationClosure` the most complex piece of code of the project).

Finally, the cake pattern is used to assemble the source, ops, sink and stream extractors together with macro or compiler plugin universes.

# Hacking

If you want to build / test / hack on this project:

* Make sure to use [paulp's sbt script](https://github.com/paulp/sbt-extras) with `sbt` 0.13.0+
* Use the following commands to checkout the sources and build the tests continuously: 

        git clone git://github.com/ochafik/Scalaxy.git
        cd Scalaxy
        sbt "project scalaxy-streams" "; clean ; ~test"

* Want to see what's going on when you compile a project with Scalaxy/Streams?

        # Print internal trees before and after Scalaxy/Streams
        SCALAXY_STREAMS_OPTIMIZE=1 sbt 'set scalacOptions ++= Seq("-Xprint:typer", "-Xprint:scalaxy-streams")' clean compile

Found a bug? Please [report it](https://github.com/ochafik/Scalaxy/issues/new) (your help will be much appreciated!).

# Size optimizations

Incidentally, using Scalaxy will reduce the number of classes generated by scalac and will produce an overall smaller code. To witness the difference (68K vs. 172K as of June 12th 2014):

        git clone git://github.com/ochafik/Scalaxy.git
        cd Scalaxy/Example
        SCALAXY_STREAMS_OPTIMIZE=1 sbt clean compile && du -h target/scala-2.11/classes/
        SCALAXY_STREAMS_OPTIMIZE=0 sbt clean compile && du -h target/scala-2.11/classes/

# TODO

* Null tests for tuple unapply withFilter calls
* Implement optimization strategies:
  * By default, the code should be safe (using "at-most-one-lambda" thumb rule or some best-effort side-effect analysis)
  * none, safe, aggressive
  * Provide implicits to alter both plugin and macro when in scope
  * Project-wide defaults: -Dscalaxy.optimizations=[force]none/safe/aggressive
* Make sure scala-library builds with the plugin (safe mode)
* Fix `a pure expression does nothing in statement position` warnings.
* Improve performance tests with compilation time, binary size and peak memory usage measurements:

  ```scala

  import java.lang.management.{ ManagementFactory, MemoryMXBean, MemoryPoolMXBean }
  import collection.JavaConversions._

  for (pool <- ManagementFactory.getMemoryPoolMXBeans) {
    println(String.format("%s: %,d", pool.getName, pool.getPeakUsage.getUsed.asInstanceOf[AnyRef]))
  }
  ```
* Test plugin as well as macros

