# Scalaxy/MacroExtensions

New trivial syntax to define class enrichments as macros ([BSD-licensed](https://github.com/ochafik/Scalaxy/blob/master/LICENSE).

Scalaxy/MacroExtensions's compiler plugin supports the following syntax:
```scala
@extend(Int) def str1: String = self.toString
@extend(Int) def str2: String = macro {
  println("Extension macro is executing!") 
  reify(self.splice.toString)
}
```
Which allows calls such as:
```scala
println(1.str1)
println(2.str2)
```
This is done by rewriting the `@extend` declarations into:
```scala
import scala.language.experimental.macros
implicit class scalaxy$extensions$str1$1(self: Int) {
  def str1(v: Int) = macro scalaxy$extensions$str1$1.str
}
object scalaxy$extensions$str1$1 {
  def str1(c: scala.reflect.macros.Context)
          (v: c.Expr[Int]): c.Expr[String] = {
    import c.universe._
    val Apply(_, List(selfTree)) = c.prefix.tree
    val self = c.Expr[String](selfTree)
    reify(self.splice.toString)
  }
}

import scala.language.experimental.macros
implicit class scalaxy$extensions$str2$1(self: Int) {
  def str2(v: Int) = macro scalaxy$extensions$str2$1.str
}
object scalaxy$extensions$str$1 {
  def str2(c: scala.reflect.macros.Context)
          (v: c.Expr[Int]): c.Expr[String] = {
    import c.universe._
    val Apply(_, List(selfTree)) = c.prefix.tree
    val self = c.Expr[String](selfTree)
    {
      println("Extension macro is executing!") 
      reify(self.splice.toString)
    }
  }
}
```

# Usage

If you're using `sbt` 0.12.2+, just put the following lines in `build.sbt`:
```scala
// Only works with 2.10.0+
scalaVersion := "2.10.0"

autoCompilerPlugins := true

addCompilerPlugin("com.nativelibs4java" %% "scalaxy-macro-extensions" % "0.3-SNAPSHOT")

// Scalaxy/Extensions snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
```

General macro rules regarding compilation apply: you cannot use macros from the same compilation unit that defines them.

# Hack

Test with:
```
sbt "project scalaxy-extensions" "run examples/extension.scala -Xprint:scalaxy-extensions"
sbt "project scalaxy-extensions" "run examples/run.scala"
```

