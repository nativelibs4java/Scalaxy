# Scalaxy/Parano

Extra compile-time checks that prevent some stupid mistakes.

Can be used as a compiler plugin (preferred method), as a standalone compiler (`scalaxy.parano.ParanoCompiler`) or by calling a macro (`scalaxy.parano.verify()`).

Checks:
* Confusing names in case class extractors
* Ambiguous unnamed arguments with same type (e.g. `Int` parameters that could be inadvertently swapped)
* Confusing names in method calls
* (TODO) Potential side-effect free statements (e.g. missing + between multiline concatenations)

```scala
case class Foo(theFirst: Int, second: Int)

val foo = Foo(10, 12)                // Error: unnamed params theFirst and second have same type
                                     //        and are ambiguous.
val foo2 = Foo(10, second = 12)      // Fine.
val foo3 = Foo(theFirst = 10, 12)    // Fine.

val Foo(someFirst, someSecond) = foo // Fine.

val Foo(theSecond, first) = foo      // Error: theSecond used to extract Foo.theFirst,
                                     //        first used to extract Foo.second

val foo5 = Foo(theSecond, first)     // Error: ident theSecond used for param theFirst,
                                     //        ident first used for param second
```

# Usage

If you're using `sbt` 0.13.0+, just put the following lines in `build.sbt`:
```scala
scalaVersion := "2.11.6"

autoCompilerPlugins := true

// Scalaxy/Parano plugin
addCompilerPlugin("com.nativelibs4java" %% "scalaxy-parano" % "0.3-SNAPSHOT")

// Ensure Scalaxy/Parano's plugin is used.
scalacOptions += "-Xplugin-require:scalaxy-parano"

// Scalaxy/Parano snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
```

# TODO

Ideas
- Make an exception for natural progressions: `x, y, z`, `a, b, c, d`, `i1, i2, i3`... (they still need mismatching names check, but no ambiguity check)
- Tuple return types in extractors: require an apply companion method with symmetric signature, take names from it and propagate accross matches:

  ```scala
  object MyExtractor {
    def apply(a: Int, b: Int) = ???
    def unapply(v: Any): Option[(Int, Int)] = v match {
      case ... =>
        val a = ...
        val b = ...
        (b, a) // Error: potential naming confusion (names of tuple are (a, b)).
    }
  }
  ```

# Hacking

If you want to build / test / hack on this project:
- Make sure to use [paulp's sbt script](https://github.com/paulp/sbt-extras) with `sbt` 0.12.2+
- Use the following commands to checkout the sources and build the tests continuously:

    ```
    git clone git://github.com/ochafik/Scalaxy.git
    cd Scalaxy
    sbt "project scalaxy-parano" "; clean ; ~test"
    ```
- Test with:

  ```
  git clone git://github.com/ochafik/Scalaxy.git
  cd Scalaxy
  sbt "project scalaxy-parano" "run examples/Test.scala"
  ```

- You can also use plain `scalac` directly, once Scalaxy/Parano's JAR is cached by sbt / Ivy:

  ```
  git clone git://github.com/ochafik/Scalaxy.git
  cd Scalaxy
  sbt update
  cd Parano
  scalac -Xplugin:$HOME/.ivy2/cache/com.nativelibs4java/scalaxy-parano_2.10/jars/scalaxy-parano_2.10-0.3-SNAPSHOT.jar examples/Test.scala
  scalac examples/Test.scala
  ```
