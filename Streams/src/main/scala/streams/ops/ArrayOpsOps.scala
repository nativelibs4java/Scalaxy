package scalaxy.streams

private[streams] trait ArrayOpsOps extends StreamComponents with ArrayOpsSinks {
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

  object SomeArrayOpsOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = Option(tree) collect {
      case q"scala.this.Predef.${AnyValArrayOpsName()}($array)" => (array, ArrayOpsOp)
      case q"scala.this.Predef.refArrayOps[${_}]($array)" => (array, ArrayOpsOp)
    }
  }

  case object ArrayOpsOp extends PassThroughStreamOp {
    // override def describe = Some("arrayOps")
    override val sinkOption = Some(ArrayOpsSink)

    override def lambdaCount = 0
  }

}
