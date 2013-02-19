object MacroDSL {
  import scala.language.experimental.macros
  import scala.reflect.macros.Context
  implicit class str(self: Int) {
    def str(v: Int) = macro str$.str
  }
  object str$ {
    def str(c: Context)(v: c.Expr[Int]): c.Expr[String] = {
      import c.universe._
      val Apply(_, List(selfTree)) = c.prefix.tree
      val self = c.Expr[String](selfTree)
      reify(self.splice.toString)
    }
  }
}
