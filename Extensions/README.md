Compiler plugin that transforms the following:
```scala
@extend(Int) def str1: String = self.toString
@extend(Int) def str2: String = macro {
  println("Extension macro is executing!") 
  reify(self.splice.toString)
}
```
Into:
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

Test with:
```
sbt "project scalaxy-extensions" "run examples/extension.scala -Xprint:scalaxy-extensions"
sbt "project scalaxy-extensions" "run examples/run.scala"
```

