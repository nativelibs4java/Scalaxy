package scalaxy.streams

private[streams] trait ArrayOps {
  val global: scala.reflect.api.Universe
  import global._

  object AnyValArrayOpsName {
    def unapply(name: Name): Boolean = String.valueOf(name) match {
      case "intArrayOps" | "longArrayOps" | "byteArrayOps" | "shortArrayOps" |
        "charArrayOps" | "booleanArrayOps" | "floatArrayOps" | "doubleArrayOps" =>
        true

      case _ =>
        false
    }
  }

  object SomeArrayOps {
    def unapply(tree: Tree): Option[Tree] = Option(tree) collect {
      case q"scala.this.Predef.${AnyValArrayOpsName()}($a)" => a
      case q"scala.this.Predef.refArrayOps[${_}]($a)"         => a
    }
  }
}
