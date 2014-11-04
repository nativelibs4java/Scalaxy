package scalaxy.streams

import scala.collection.generic.CanBuildFrom

private[streams] trait ArrayOpsSinks extends ArrayBuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  case object ArrayOpsSink extends StreamSink
  {
    override def isFinalOnly = true
    override def isJustAWrapper = true
    override def describe = Some("ArrayOps")
    override def lambdaCount = 0
    override def subTrees = Nil

    private[this] val arrayOpsClass = "scala.collection.mutable.ArrayOps"
    private[this] lazy val anyValOpsClassNameByType: Map[Type, String] = Map(
      typeOf[Boolean] -> (arrayOpsClass + ".ofBoolean"),
      typeOf[Byte] -> (arrayOpsClass + ".ofByte"),
      typeOf[Char] -> (arrayOpsClass + ".ofChar"),
      typeOf[Double] -> (arrayOpsClass + ".ofDouble"),
      typeOf[Float] -> (arrayOpsClass + ".ofFloat"),
      typeOf[Int] -> (arrayOpsClass + ".ofInt"),
      typeOf[Long] -> (arrayOpsClass + ".ofLong"),
      typeOf[Short] -> (arrayOpsClass + ".ofShort"),
      typeOf[Unit] -> (arrayOpsClass + ".ofUnit")
    )

    private def replaceLast[A](list: List[A], f: A => A): List[A] = {
      val last :: reversedRest = list.reverse
      (f(last) :: reversedRest).reverse
    }

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input._

      val arrayOutput = ArrayBuilderSink.emit(input, outputNeeds, nextOps)
      val componentTpe = input.vars.tpe.dealias

      def getResult(array: Tree) = typed(
        anyValOpsClassNameByType.get(componentTpe) match {
          case Some(primitiveOpsClass) =>
            q"new ${rootMirror.staticClass(primitiveOpsClass)}($array)"
          case _ if componentTpe <:< typeOf[AnyRef] =>
            q"new scala.collection.mutable.ArrayOps.ofRef[$componentTpe]($array)"
          case _ =>
            q"genericArrayOps[$componentTpe]($array)"
        }
      )

      arrayOutput.copy(ending = replaceLast[Tree](arrayOutput.ending, getResult(_)))
    }
  }
}
