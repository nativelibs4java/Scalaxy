package scalaxy.streams

private[streams] trait OptionOps
    extends UnusableSinks
    with OptionSinks
    with Streams
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeOptionOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.get" =>
        (target, OptionGetOrElseOp("get", q"""
          throw new NoSuchElementException("None.get")
        """))

      case q"$target.orNull[${_}](${_})" =>
        (target, OptionGetOrElseOp("orNull", q"null"))

      case q"$target.getOrElse[${_}]($v)" =>
        (target, OptionGetOrElseOp("getOrElse", v))

      case q"$target.orElse[$tpt]($orElseValue)" =>
        (target, OptionOrElseOp(componentTpe = tpt.tpe, orElseValue))
    }
  }

  case class OptionGetOrElseOp(name: String, defaultValue: Tree) extends StreamOp {
    override def lambdaCount = 1
    override def sinkOption = Some(ScalarSink)
    override def canAlterSize = true
    override def describe = Some(name)
    override def subTrees = Nil
    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
      Set(RootTuploidPath) // TODO: refine this.

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input._

      // TODO: remove this to unlock flatMap
      val List((ScalarSink, _)) = nextOps

      val value = fresh("value")
      val nonEmpty = fresh("nonEmpty")
      require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

      val Block(List(
          valueDef,
          nonEmptyVarDef,
          assignment), result) = typed(q"""
        ${newVar(value, input.vars.tpe)};
        private[this] var $nonEmpty = false;
        {
          $value = ${input.vars.alias.get};
          $nonEmpty = true;
        };
        if ($nonEmpty) $value else $defaultValue
      """)

      StreamOutput(
        prelude = List(valueDef, nonEmptyVarDef),
        body = List(assignment),
        ending = List(result))
    }
  }



  case class OptionOrElseOp(componentTpe: Type, orElseValue: Tree)
      extends StreamOp
  {
    val nestedStream: Option[Stream] = Option(orElseValue) collect {
      case SomeStream(stream) =>//if stream.sink.canBeElided =>
        stream
    }

    override def describe = Some(
      "orElse" + nestedStream.map("(" + _.describe(describeSink = false) + ")").getOrElse(""))

    override def lambdaCount = 1 + nestedStream.map(_.lambdaCount).getOrElse(0)

    override def subTrees: List[Tree] =
      nestedStream.map(_.subTrees).
        getOrElse(List(orElseValue))

    override def closureSideEffectss =
      nestedStream.map(_.closureSideEffectss).
        getOrElse(super.closureSideEffectss)

    // Do not leak the interruptibility out of a flatMap.
    override def canInterruptLoop = false//nestedStream.map(_.ops.exists(_.canInterruptLoop)).getOrElse(false)

    override def canAlterSize = true

    override def sinkOption: Option[StreamSink] =
      Some(OptionSink)

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
      Set(RootTuploidPath) // TODO: refine this.

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ fresh, transform, typed, currentOwner }

      nestedStream match {
        case Some(stream) =>

          val value = fresh("value")
          val nonEmpty = fresh("nonEmpty")
          require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

          // val tpe = componentTpe.getOrElse(input.vars.tpe)
          val Block(List(
              valueVarDef,
              valueVarRef,
              nonEmptyVarDef), nonEmptyVarRef) = typed(q"""
            ${newVar(value, componentTpe)};
            $value;
            private[this] var $nonEmpty = false;
            $nonEmpty
          """)

          val stagedSink = StagedOptionSink(valueVarDef.symbol, nonEmptyVarDef.symbol)

          // Expand the orElse value's nested stream.
          val subNested = stream.copy(sink = stagedSink).emitStream(
            fresh,
            transform,
            currentOwner = currentOwner,
            typed = typed,
            loopInterruptor = input.loopInterruptor)//.map(replacer)

          // The rest of the outer stream.
          val post = emitSub(
            input.copy(
              vars = ScalarValue(componentTpe, alias = Some(valueVarRef))),
            nextOps)

          // Compose the two expansions.
          StreamOutput(
            prelude = List(valueVarDef, nonEmptyVarDef),
            body = List(typed(q"""
              $nonEmptyVarRef = true;
              $valueVarRef = ${input.vars.alias.get};
            """)),
            afterBody = List(typed(q"""
              if (!$nonEmptyVarRef) {
                ..${subNested.flatten};
              }
            """)),
            // afterBody = List(typed(q"""
            //   ..${subNested.prelude}; // TODO: move this inside if below?
            //   if (!$nonEmptyVarRef) {
            //     ..${subNested.flatten};
            //     ..${subNested.beforeBody};
            //     ..${subNested.body};
            //     ..${subNested.afterBody}
            //   }
            //   ..${subNested.ending} // TODO: move this inside if above?
            // """)),
            ending = List(typed(q"""
              ..${post.prelude};
              if ($nonEmptyVarRef) {
                ..${post.beforeBody};
                ..${post.body};
                ..${post.afterBody}
              }
              ..${post.ending}
            """))
          )

        case _ =>
          ???
          // val (replacedStatements, outputVars) =
          //   transformationClosure.replaceClosureBody(
          //     input.copy(
          //       outputSize = None,
          //       index = None),
          //     outputNeeds)

          // val itemVal = fresh("item")
          // val Function(List(itemValDef @ ValDef(_, _, _, _)), itemValRef @ Ident(_)) = typed(q"""
          //   ($itemVal: $tpe) => $itemVal
          // """)

          // val sub = emitSub(
          //   input.copy(
          //     vars = ScalarValue(tpe, alias = Some(itemValRef)),
          //     outputSize = None,
          //     index = None),
          //   nextOps)
          // sub.copy(body = List(typed(q"""
          //   ..$replacedStatements;
          //   // TODO: plug that lambda's symbol as the new owner of sub.body's decls.
          //   ${outputVars.alias.get}.foreach(($itemValDef) => {
          //     ..${sub.body};
          //   })
          // """)))
      }
    }
  }
}
