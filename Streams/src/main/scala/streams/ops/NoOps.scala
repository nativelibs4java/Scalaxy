package scalaxy.streams

private[streams] trait NoOps extends StreamComponents with Strippers
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeNoOp {
    def unapply(tree: Tree): Option[(Tree, StreamOp)] = Option(tree) collect {
      case q"$target.withFilter(${Strip(Function(List(param), body))})" =>
        // TODO
        q"""
          (item2: (Array[Int], Int)) => (item2: (Array[Int], Int) @unchecked) match {
            case ((a @ _), (i @ _)) => true
            case _ => false
          }
        """
        ???
    }
  }
}
