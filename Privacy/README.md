# Scalaxy/Privacy

Compiler plugin that changes default privacy of vals, vars and defs to private, unless `@public` is used, and warns about non-trivial public defs and vals that lack type annotations.

This is only at the **early stages of experimentation**, and since it **may alter the Scala semantics** of your code, well... you've been warned :-)

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

# More details

* `protected`, `private`, `override` and `abstract` definitions are unmodified
* Accessors of case class canonical fields are unmodified:
  ```scala
    case class Foo(thisIsPublic: Int) {
      val thisIsPrivate = 10
    }
  ```
* Code that compiles with the plugin will most likely compile without, unless there's name clashes due to wildcards

# Usage

If you're using `sbt` 0.13.0+, just put the following lines in `build.sbt`:
```scala
scalaVersion := "2.10.3"

autoCompilerPlugins := true

// Scalaxy/Privacy plugin
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
