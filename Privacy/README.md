# Scalaxy/Privacy

Caveat: this is only at the **early stages of experimentation**, and since it **may alter the Scala semantics** of your code, well... you've been warned :-)

Scalaxy/Privacy is a Scala compiler plugin that:
* Changes default visibility from public to `private[this]` (public requires a `@public` annotation).

  ```scala
    @public object Foo {
      val privateByDefault = 10
      @public val explicitlyPublic = 12
    }
  ```

* Warns about non-trivial public methods and values without type annotations

  ```scala
    object Foo {
      // Warning: public `f` method has a non-trivial return type without type annotation.
      @public def f(x: Int) = if (x < 0) "1" else 2
    }
  ```

# Why, oh why??

The default public visibility of Scala definitions is often at odds with the principle of [encapsulation](http://en.wikipedia.org/wiki/Encapsulation_(object-oriented_programming)).
That's why some coding guidelines like Twitter's excellent [Effective Scala](http://twitter.github.io/effectivescala/) recommend [to mark methods as `private[this]`](http://twitter.github.io/effectivescala/#Object oriented programming-Visibility) when they don't need to be public (which may have fringe performance benefits as well).

While this is great practice, it can quickly lead to lots of `private[this]` boilerplate, which this compiler plugin aims to remove the need for. The bet here is that with Scalaxy/Privacy:
* You'll pay more attention to what needs to be `@public`, because you'll need to make that explicit: your code will be better encapsulated throughout.
* You'll type less `@public` annotations than you would've typed `private` or `private[this]` modifiers
* You'll be able to migrate your code away from Scalaxy/Privacy when you're bored of it / whenever I die and henceworth fail to update the plugin on time for the subsequent Scala version.
  (TODO write a migration diff generator)

Also, with the extra warnings about missing type annotations for public members with non-trivial bodies:
* You'll make your code more readable,
* You'll spot unintentional propagations of weird types (no more returning `Unit` or `MyPrivateLocalType` three levels deep by mistake)

Now well... one may consider this as a language fork, which it might well be, somehow, but don't you worry: even if you convert all your code to using `@public` instead of `private`, the plugin will (soon) allow you to migrate all your code back to its equivalent traditional Scala, by generating the diffs you'll need to apply to it.

# Longer examples

```scala
@public object Foo {
  // Any val, def, class or object not marked with @public is assumed to be private[this].
  val privateByDefault = 10

  // The @public annotation is removed by the compiler plugin.
  @public val explicitlyPublic = 12

  // Warning: public `f` method has a non-trivial return type without type annotation.
  @public def f(x: Int) = if (x < 0) "1" else 2

  // Ok.
  @public def g(x: Int): Int = f(x)
}

println(Foo.privateByDefault) // Error: Scalaxy/Privacy make that one private[this].
println(Foo.explicitlyPublic) // Ok.

// Regular Scala visibility rules apply within elements tagged with @noprivacy
@noprivacy object Bar {
  val publicByDefault = 10
  private val explicitlyPrivate = 12
}

println(Foo.publicByDefault)   // Ok.
println(Foo.explicitlyPrivate) // Error.
```

If that wasn't long enough, have a look at the following tests:
* [PrivacyTest.scala](https://github.com/ochafik/Scalaxy/blob/master/Privacy/Plugin/src/test/scala/scalaxy/PrivacyTest.scala)
* [ExplicitTypeAnnotationsTest.scala](https://github.com/ochafik/Scalaxy/blob/master/Privacy/Plugin/src/test/scala/scalaxy/ExplicitTypeAnnotationsTest.scala)

# More details

## Private by default

* `protected`, `private`, `override` and `abstract` definitions are unmodified
* Accessors of case class canonical fields are unmodified:

  ```scala
    case class Foo(thisIsPublic: Int) {
      val thisIsPrivate = 10
    }
  ```

* Code that compiles with the plugin will most likely compile without, unless there's name clashes due to wildcard imports. This means that IDEs can grok code annotated with `@public` and `@noprivacy` without the need to run the plugin, because the code they'll see will also be valid Scala.

## Warnings on missing type annotations for public declarations

* `val`s and `def`s which body is considered _trivial_ are not required to have a type annotation.
  For instance, `def x = List(1, 2)` and `def y = List[Int]()` obviously have a return type of `List[Int]`, so no warning is needed.
  However, `def z = List(1, "2")` is less trivial, so the plugin will warn about it.
  The justification for these special cases (which affect `List`, `Set`, `Seq`, `Array`, `Iterable`, `Traversable` and to a lesser extent, `Map`) is that annotating return type of trivial methods brings no value to users, and the plugin aims to make your code more maintainable while cutting the boilerplate.
* Method and value `override`s don't need to have type annotations: the risk of returning an unexpected type is very low, since the parent definition is likely to have a proper type annotation.

# Usage

If you're using `sbt` 0.13.0+, just put the following lines in `build.sbt`:
```scala
scalaVersion := "2.10.3"

autoCompilerPlugins := true

// Scalaxy/Privacy annotations are only needed to avoid confusing IDEs.
libraryDependencies += "com.nativelibs4java" %% "scalaxy-privacy" % "0.3-SNAPSHOT"

// Scalaxy/Privacy compiler plugin.
addCompilerPlugin("com.nativelibs4java" %% "scalaxy-privacy-plugin" % "0.3-SNAPSHOT")

// Ensure Scalaxy/Privacy's plugin is used.
scalacOptions += "-Xplugin-require:scalaxy-privacy"

// Scalaxy/Privacy snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
```

# TODO

- Tests
- Check there aren't weird corner cases

# Hacking

If you want to build / test / hack on this project:
- Make sure to use [paulp's sbt script](https://github.com/paulp/sbt-extras) with `sbt` 0.12.2+
- Use the following commands to checkout the sources and build the tests continuously:

  ```
  git clone git://github.com/ochafik/Scalaxy.git
  cd Scalaxy
  sbt "project scalaxy-privacy-plugin" "; clean ; ~test"
  ```

- Test with:

  ```
  git clone git://github.com/ochafik/Scalaxy.git
  cd Scalaxy
  sbt "project scalaxy-privacy-plugin" "run examples/Test.scala"
  ```

- You can also use plain `scalac` directly, once Scalaxy/Privacy's JAR is cached by sbt / Ivy:

  ```
  git clone git://github.com/ochafik/Scalaxy.git
  cd Scalaxy
  sbt update
  cd Privacy
  scalac -Xplugin:$HOME/.ivy2/cache/com.nativelibs4java/scalaxy-privacy-plugin_2.10/jars/scalaxy-privacy-plugin_2.10-0.3-SNAPSHOT.jar examples/Test.scala
  scalac examples/Test.scala
  ```
