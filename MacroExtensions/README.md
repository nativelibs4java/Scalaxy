# Scalaxy/MacroExtensions

New *experimental* syntax to define class enrichments as macros ([BSD-licensed](https://github.com/ochafik/Scalaxy/blob/master/LICENSE)).

There's some context [on my blog](http://ochafik.com/blog/?p=872), and a short [discussion on scala-internals](https://groups.google.com/d/topic/scala-internals/vzfgUskaJ_w/discussion) (+ make sure to read about known issues below).

Scalaxy/MacroExtensions's compiler plugin supports the following syntax:
```scala
@scalaxy.extension[Any] 
def quoted(quote: String): String = 
  quote + self + quote
  
@scalaxy.extension[Int] 
def copiesOf[T : ClassTag](generator: => T): Array[T] = 
  Array.fill[T](self)(generator)

@scalaxy.extension[Array[A]] 
def tup[A, B](b: B): (A, B) = macro { 
  // Explicit macro.
  println("Extension macro is executing!")
  reify((self.splice.head, b.splice))
}
```
Which allows calls such as:
```scala
println(10.quoted("'"))
// macro-expanded to `"'" + 10 + "'"`

println(10 copiesOf new Entity)
// macro-expanded to `Array.fill(3)(new Entity)`

println(Array(3).tup(1.0)) 
// macro-expanded to `(Array(3).head, 1.0)` (and prints a message during compilation)
```
This is done by rewriting the `@scalaxy.extension[T]` declarations above during compilation of the extensions.
In the case of `str2`, this gives the following:
```scala
import scala.language.experimental.macros
implicit class scalaxy$extensions$str2$1(self: Any) {
  def str2(quote$Expr$1: String) = 
    macro scalaxy$extensions$str2$1.str2
}
object scalaxy$extensions$str2$1 {
  def str2(c: scala.reflect.macros.Context)
          (quote$Expr$1: c.Expr[String]): 
      c.Expr[String] = 
  {
    import c.universe._
    val Apply(_, List(selfTree$1)) = c.prefix.tree
    val self$Expr$1 = c.Expr[Any](selfTree$1)
    reify({
      val self = self$Expr$1.splice
      val quote = quote$Expr$1.splice
      quote + self + quote
    })
  }
}
```

# Known Issues

- Annotation is resolved by name: if you redefine an `@scalaxy.extension` annotation, this will break compilation.
- Default parameter values are not supported (due to macros not supporting them?)
- Doesn't check macro extensions are defined in publicly available static objects (but compiler does)

# Usage

If you're using `sbt` 0.13.0+, just put the following lines in `build.sbt`:
```scala
// Only works with 2.10.0+
scalaVersion := "2.10.3"

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
