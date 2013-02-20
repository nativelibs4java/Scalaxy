# Scalaxy/MacroExtensions

New *experimental* syntax to define class enrichments as macros ([BSD-licensed](https://github.com/ochafik/Scalaxy/blob/master/LICENSE)).

Some context [on my blog](http://ochafik.com/blog/?p=872), and some [discussion on scala-internals](https://groups.google.com/d/topic/scala-internals/vzfgUskaJ_w/discussion) (+ make sure to read about known issues below).

Scalaxy/MacroExtensions's compiler plugin supports the following syntax:
```scala
@extend(Int) def str1: String = self.toString
@extend(Any) def str2(quote: String): String = quote + self + quote
@extend(Int) def str3: String = macro {
  println("Extension macro is executing!") 
  reify(self.splice.toString)
}
```
Which allows calls such as:
```scala
println(1.str1)
println(2.str2("'"))
println(3.str3)
```
This is done by rewriting the `@extend` declarations above during compilation of the extensions.
In the case of `str2`, this yields the following:
```scala
import scala.language.experimental.macros
implicit class scalaxy$extensions$str2$1(self: Any) {
  def str2(quote: String) = macro scalaxy$extensions$str2$1.str
}
object scalaxy$extensions$str$1 {
  def str2(c: scala.reflect.macros.Context)
          (quote: c.Expr[String]): c.Expr[String] = {
    import c.universe._
    val Apply(_, List(selfTree$1)) = c.prefix.tree
    val self = c.Expr[Any](selfTree$1)
    {
      reify(quote.splice + self.splice + quote.splice)
    }
  }
}
```

# Known Issues

- Annotation is resolved by name: if you redefine an `@extend` annotation, this will break compilation (latest HEAD code uses `@scalaxy.extend` instead, will be published to Maven repo soon).
- Default parameter values are not supported (due to macros not supporting them?)
- Implicit values may not be passed appropriately (maybe another limitation of macros?)
- Doesn't check macro extensions are defined in publicly available static objects (but compiler does)
- No automated test cases yet: this is just an experiment! (see `examples` directory for some constructs to test manually)

# Usage

If you're using `sbt` 0.12.2+, just put the following lines in `build.sbt`:
```scala
// Only works with 2.10.0+
scalaVersion := "2.10.0"

autoCompilerPlugins := true

// Scalaxy/MacroExtensions plugin
addCompilerPlugin("com.nativelibs4java" %% "scalaxy-macro-extensions" % "0.3-SNAPSHOT")

// Ensure Scalaxy/MacroExtensions's plugin is used.
scalacOptions += "-Xplugin-require:scalaxy-extensions"

// Uncomment this to see what's happening:
//scalacOptions += "-Xprint:scalaxy-extensions"

// We're compiling macros, reflection library is needed.
libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" %)

// Scalaxy/MacroExtensions snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
```

General macro rules regarding compilation apply: you cannot use macros from the same compilation unit that defines them.

# Hack

Test with:
```
git clone git://github.com/ochafik/Scalaxy.git
cd Scalaxy
sbt "project scalaxy-extensions" "run examples/TestExtensions.scala -Xprint:scalaxy-extensions"
sbt "project scalaxy-extensions" "run examples/Test.scala"
```

You can also use plain `scalac` directly, once Scalaxy/MacroExtensions's JAR is cached by sbt / Ivy:
```
git clone git://github.com/ochafik/Scalaxy.git
cd Scalaxy
sbt update
cd Extensions
scalac -Xplugin:$HOME/.ivy2/cache/com.nativelibs4java/scalaxy-macro-extensions_2.10/jars/scalaxy-macro-extensions_2.10-0.3-SNAPSHOT.jar examples/TestExtensions.scala -Xprint:scalaxy-extensions
scalac examples/Test.scala
```
