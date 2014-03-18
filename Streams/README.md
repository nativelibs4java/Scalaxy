# Scalaxy/Streams

Scalaxy/Streams makes your Scala collections code faster:
* Fuses collection streams down to while loops
* Avoids unnecessary tuples
* Usable as a compiler plugin (whole project) or as a macro (surgical strikes)

# TODO

* Null tests for tuple unapply withFilter calls
* Fix `a pure expression does nothing in statement position` warnings.
* Publish artifacts
* Performance tests, including peak memory usage measurements:

  ```scala

  import java.lang.management.{ ManagementFactory, MemoryMXBean, MemoryPoolMXBean }
  import collection.JavaConversions._

  for (pool <- ManagementFactory.getMemoryPoolMXBeans) {
    println(String.format("%s: %,d", pool.getName, pool.getPeakUsage.getUsed.asInstanceOf[AnyRef]))
  }
  ```

* Test plugin as well as macros
* Support @optimize(true) / @optimize(false) / -Dscalaxy.streams.optimize=true/false/never/always

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

# Usage with Sbt

If you're using `sbt` 0.13.0+, just put the following lines in `build.sbt`:

* To use the macro (manually decide which parts of your code are optimized):

  ```scala
  // Only works with 2.11.0-RC1
  scalaVersion := "2.11.0-RC1"

  // Dependency at compilation-time only (not at runtime).
  libraryDependencies += "com.nativelibs4java" %% "scalaxy-streams" % "0.3-SNAPSHOT" % "provided"

  // Scalaxy/Streams snapshots are published on the Sonatype repository.
  resolvers += Resolver.sonatypeRepo("snapshots")
  ```

  And wrap some blocks with `optimize`:
  ```scala
  import scalaxy.streams.optimize
  optimize {
    for (i <- 0 until n; j <- i until n; if (i + j)  % 2 == 0) {
      ...
    }
  }
  ``` 
* To use the compiler plugin (optimizes all of your code):

  ```scala
  // Only works with 2.11.0-RC1
  scalaVersion := "2.11.0-RC1"

  autoCompilerPlugins := true

  addCompilerPlugin("com.nativelibs4java" %% "scalaxy-streams" % "0.3-SNAPSHOT")

  scalacOptions += "-Xplugin-require:scalaxy-streams"

  // Scalaxy/Streams snapshots are published on the Sonatype repository.
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
      <artifactId>scalaxy-streams_2.11</artifactId>
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
              <artifactId>scalaxy-streams_${scala.major.minor.version}</artifactId>
              <version>0.3-SNAPSHOT</version>
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

Of course, this assumes you have something like this:
```xml
  <properties>
    <scala.major.minor.version>2.10</scala.major.minor.version>
    <scala.patch.version>0-RC1</scala.patch.version>
    <scala.version>${scala.major.minor.version}.${scala.patch.version}</scala.version>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.scala-lang</groupId>
      <artifactId>scala-library</artifactId>
      <version>${scala.version}</version>
    </dependency>
  </dependencies>
```

# Hacking

If you want to build / test / hack on this project:
- Make sure to use [paulp's sbt script](https://github.com/paulp/sbt-extras) with `sbt` 0.13.0+
- Use the following commands to checkout the sources and build the tests continuously: 

    ```
    git clone git://github.com/ochafik/Scalaxy.git
    cd Scalaxy
    sbt "project scalaxy-streams" "; clean ; ~test"
    ```
