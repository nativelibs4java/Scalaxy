package scalaxy.streams

import scala.collection.generic.CanBuildFrom

private[streams] trait ArrayBuilderSinks extends BuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  case object ArrayBuilderSink extends BuilderSink
  {
    override def describe = Some("Array")

    override def lambdaCount = 0

    override def subTrees = Nil

    override def usesSizeHint = true

    // TODO build array of same size as source collection if it is known.
    override def createBuilder(inputVars: TuploidValue[Tree], typed: Tree => Tree) = {
      val builderModule =
        rootMirror.staticModule("scala.collection.mutable.ArrayBuilder")
      typed(q"$builderModule.make[${inputVars.tpe}]")
    }

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input._

      input.outputSize match {
        case Some(outputSize) =>//if false => // buggy
          // If the output size is known, just create an array.
          // println(s"outputSize in ArraySink is ${input.outputSize}")
          // new RuntimeException(s"outputSize in ArraySink is ${input.outputSize}").printStackTrace()

          require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

          val array = fresh("array")
          val index = fresh("i")

          val componentTpe = normalize(input.vars.tpe)

          val Block(List(
              arrayDecl,
              arrayCreation,
              indexDef,
              append,
              incr), arrayRef) = typed(q"""
            private[this] var $array: Array[$componentTpe] = null;
            $array = new Array[$componentTpe]($outputSize);
            private[this] var $index = 0;
            $array($index) = ${input.vars.alias.get};
            $index += 1;
            $array
          """)

          StreamOutput(
            prelude = List(arrayDecl),
            beforeBody = List(arrayCreation, indexDef),
            body = List(append, incr),
            ending = List(arrayRef))

        // case None =>
        case _ =>
          super.emit(input, outputNeeds, nextOps)
      }
    }
  }
}
