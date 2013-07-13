# Scalaxy/Debug

Useful debug macros.

Package `scalaxy.debug` provides macro alternatives to `Predef.{assert, assume, require}` that automatically add a sensible failure message, making them more similar to [C asserts](http://en.wikipedia.org/wiki/Assert.h):
```scala
import scalaxy.debug._

val a = 10
val aa = a
val b = 12
val condition = a == b

// Asserts and their corresponding message:
assert(a == b)    // "assertion failed: a == b (10 != 12)"
assert(a != aa)   // "assertion failed: a != aa (a == aa == 10)" 
assert(a != 12)   // "assertion failed: a != 12"
assert(condition) // "assertion failed: condition"
```

All of this is done during macro-expansion, so there's no runtime overhead.
For instance, the following:
```scala
assert(a == b)    // "assertion failed: a == b (10 != 12)"
```
Is expanded to:
```scala
{ 
  val left = a
  val right = b
  assert(left == right, s"a == b ($left != $right)")
}
```

These macros don't bring any runtime dependency: you can just kiss most `assert`, `assume` and `require` messages goodbye (and still have informative failure messages).

If you knew about `assert` but unsure what `assume` and `require` are, please [Assert, Require and Assume](http://daily-scala.blogspot.co.uk/2010/03/assert-require-assume.html) (on [Daily Scala](http://daily-scala.blogspot.co.uk/))

# Usage

If you're using `sbt` 0.12.2+, just put the following lines in `build.sbt`:
```scala
// Only works with 2.10.0+
scalaVersion := "2.10.0"

// Dependency at compilation-time only (not at runtime).
libraryDependencies += "com.nativelibs4java" %% "scalaxy-debug" % "0.3-SNAPSHOT" % "provided"

// Scalaxy/Debug snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
```
    
# Hacking

If you want to build / test / hack on this project:
- Make sure to use [paulp's sbt script](https://github.com/paulp/sbt-extras) with `sbt` 0.12.2+
- Use the following commands to checkout the sources and build the tests continuously: 

    ```
    git clone git://github.com/ochafik/Scalaxy.git
    cd Scalaxy
    sbt "project scalaxy-debug" "; clean ; ~test"
    ```

