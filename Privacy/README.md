# Scalaxy/Privacy

Change default privacy of vals, vars and defs to private, unless `@public` is used.

```scala
object Foo {
  val privateByDefault = 10
  @public val explicitlyPublic = 12
}

println(Foo.privateByDefault) // Error: Scalaxy/Privacy turned that one private.
```

# Usage

If you're using `sbt` 0.13.0+, just put the following lines in `build.sbt`:
```scala
scalaVersion := "2.10.3"

autoCompilerPlugins := true

// Scalaxy/Privacy plugin
addCompilerPlugin("com.nativelibs4java" %% "scalaxy-privacy" % "0.3-SNAPSHOT")

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
    sbt "project scalaxy-privacy" "; clean ; ~test"
    ```
- Test with:

  ```
  git clone git://github.com/ochafik/Scalaxy.git
  cd Scalaxy
  sbt "project scalaxy-privacy" "run examples/Test.scala"
  ```

- You can also use plain `scalac` directly, once Scalaxy/Privacy's JAR is cached by sbt / Ivy:

  ```
  git clone git://github.com/ochafik/Scalaxy.git
  cd Scalaxy
  sbt update
  cd Privacy
  scalac -Xplugin:$HOME/.ivy2/cache/com.nativelibs4java/scalaxy-privacy_2.10/jars/scalaxy-privacy_2.10-0.3-SNAPSHOT.jar examples/Test.scala
  scalac examples/Test.scala
  ```
