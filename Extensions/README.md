Compiler plugin that transforms the following:
```scala
@extend(Int) def str: String = macro reify(self.splice.toString)
```
Into:
```scala
import scala.language.experimental.macros
implicit class str(self: Int) {
  def str(v: Int) = macro str$.str
}
object str$ {
  def str(c: scala.reflect.macros.Context)
         (v: c.Expr[Int]): c.Expr[String] = {
    import c.universe._
    val Apply(_, List(selfTree)) = c.prefix.tree
    val self = c.Expr[String](selfTree)
    reify(self.splice.toString)
  }
}
```

Test with:
```
sbt "project scalaxy-extensions" "run examples/extension.scala -Xprint:scalaxy-extensions"
sbt "project scalaxy-extensions" "run examples/run.scala"
```

