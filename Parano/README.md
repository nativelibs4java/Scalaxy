# Scalaxy/Parano

Extra compile-time warnings and error that prevent some stupid mistakes in compilation units where `scalaxy.parano.verify()` is called

Checks:
* Confusing names in case class extractors
* Ambiguous unnamed arguments with same type
* Confusing names in method calls
* (TODO) Potential side-effect free statements (e.g. missing + between multiline concatenations)

```
scalaxy.parano.verify()

case class Foo(a: Int, b: Int)

val foo = Foo(10, 12)
val Foo(b, a) = foo // Error: b used to extract a, a used to extract b.
Foo(b, a) // Error: ident b used for param a, ident a used for param b.
Foo(1, 2) // Error: unnamed params a and b have same type and are ambiguous.
```

# Usage

If you're using `sbt` 0.12.2+, just put the following lines in `build.sbt`:
```scala
// Only works with 2.10.0+
scalaVersion := "2.10.0"

// Dependency at compilation-time only (not at runtime).
libraryDependencies += "com.nativelibs4java" %% "scalaxy-parano" % "0.3-SNAPSHOT" % "provided"

// Scalaxy/Parano snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
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
