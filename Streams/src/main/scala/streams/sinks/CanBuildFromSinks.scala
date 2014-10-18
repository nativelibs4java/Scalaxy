package scalaxy.streams

import scala.collection.generic.CanBuildFrom

private[streams] trait CanBuildFromSinks
    extends BuilderSinks
    with ArrayBuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  class CanBuildFromSink(canBuildFrom: Tree) extends BuilderSink
  {
    val TypeRef(_, _, List(_, _, toTpe: Type)) = {
      val sym = rootMirror.staticClass("scala.collection.generic.CanBuildFrom")
      canBuildFrom.tpe.baseType(sym)
    }

    override def describe = Some(toTpe.typeSymbol.name.toString)

    override def usesSizeHint = true

    override def createBuilder(inputVars: TuploidValue[Tree], typed: Tree => Tree) = {
      typed(q"$canBuildFrom()")
    }
  }

  object CanBuildFromSink
  {
    def unapply(op: StreamOp): Option[CanBuildFromSink] =
      Option(op) collect { case op: CanBuildFromSink => op }

    def apply(canBuildFrom: Tree): StreamSink = {
      val sym = canBuildFrom.symbol
      if (sym != null && sym != NoSymbol &&
          // Note: simple owner comparison doesn't cut it (different mirrors?).
          sym.owner.fullName == definitions.ArrayModule.fullName &&
          sym.name.toString == "canBuildFrom") {
        //scala.this.Array.canBuildFrom[Int]((ClassTag.Int: scala.reflect.ClassTag[Int]))
        ArrayBuilderSink
      } else {
        new CanBuildFromSink(canBuildFrom)
      }
    }
  }
}
