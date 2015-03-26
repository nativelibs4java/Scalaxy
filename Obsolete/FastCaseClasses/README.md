[Scala 2.11 introduced name-based extractors](http://hseeberger.github.io/blog/2013/10/04/name-based-extractors-in-scala-2-dot-11/), which means we can avoid creating lots of tuples, options and boxed primitives.

This simple experiment makes all case classes use that new trick, thanks to a compiler plugin!

It can be combined with [Scalaxy/Streams](https://github.com/ochafik/Scalaxy/tree/master/Streams) (which avoids creating many tuples and intermediate collections in for-comprehensions), getting you one step closer to GC paradise :-)

# Usage

    scalaVersion := "2.11.6"

    resolvers += Resolver.sonatypeRepo("snapshots")

    autoCompilerPlugins := true

    addCompilerPlugin("com.nativelibs4java" %% "scalaxy-fastcaseclasses" % "0.4-SNAPSHOT")

    scalacOptions += "-Xplugin-require:scalaxy-fastcaseclasses"

    // Uncomment these lines to use Scalaxy/Streams.
    // addCompilerPlugin("com.nativelibs4java" %% "scalaxy-streams" % "0.4-SNAPSHOT")

    // scalacOptions += "-Xplugin-require:scalaxy-streams"

# What does it do?

Given the following example:

    case class Point(x: Int, y: Int)

Scalac produces code like the following:

    class Point(val x: Int, val y: Int) {
      // ...
      // (toString, hashCode, productIterator...)
    }

    object Point {
      def apply(x: Int, y: Int) = new Point(x, y)
      def unapply(p: Point) = new Option((Int.box(x), Int.box(y)))
      // ...
      // (toString...)x
    }

Scalaxy/FastCaseClasses tweaks the code to avoid creating the `(x, y)` tuple, avoid boxing x and y into Integers, and avoid creating the `Option` in `Point.unapply`:

    class Point(val x: Int, val y: Int) {
      // ...
      def isEmpty = false
      def get = this
      def _1 = x
      def _2 = y
    }

    object Point {
      def apply(x: Int, y: Int) = new Point(x, y)
      def unapply(p: Point) = p
      // ...
      // (toString...)x
    }

(That's 4 allocations less at each extraction. Combine this with Scalaxy/Streams to avoid tuples and intermediate collections in for-comprehensions, and you're getting one step closer to GC paradise)
Note: when there's only one value to extract, the code looks a bit different:

    case class Foo(x: Int) extends AnyVal

    ->

    class Foo(val x: Int) extends AnyVal {
      // ...
      def isEmpty = false
      def get = x
    }

    object Foo {
      def apply(x: Int) = new Foo(x)
      def unapply(p: Foo) = p
      // ...
    }

# Limitations

The hack has its limits and won't be applied in the following cases:

* No covariant generic types:

    case class Foo[+A](a: Int)

* No existing `isEmpty`, `get` or `_1` methods:

    trait Base {
      def isEmpty: true
    }
    case class Foo(x: Int) extends Trait

# How does it work?

This is an old-style compiler plugin, which does not use the (amazing) macro paradise library. This way we can reach to all case classes without being restricted to those annotated with a macro annotation.

The first proof-of-concept was running before the typer `typer`, to avoid having to fiddle with symbols. Since synthetic methods of case classes are added during that phase, this means I had to synthetize those methods myself. In the end it worked well, but was unable to detect cases where the rewrite doesn't work (e.g. when the class inherits from a class with an existing `isEmpty` method).

The current code runs after the `typer` phase and tries to group case classes' class and object companions, then surgically adds the methods needed (and swaps out the existing `unapply` method). It creates / enters all the symbols explicitly to avoid any typer issue.
