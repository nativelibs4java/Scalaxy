package scalaxy.loops

private[loops] trait TransformationClosures extends TuploidValues
{
  val global: scala.reflect.api.Universe
  import global._

  case class TransformationClosure(
      inputs: TuploidValue,
      statements: List[Tree],
      outputs: TuploidValue)
  {
    def hasTupleAliasReferencesInStatements = {
      val tupleAliases = inputs.collectTupleAliases
      statements.exists(_.exists(t => tupleAliases.contains(t.symbol)))
    }
  }

  object TransformationClosure {
    def extract(closure: Tree): Option[TransformationClosure] = {

      val inputValueAndBodyOpt = closure match {
        case q"""($param) => (${paramRef @ Ident(_)}: $_) match { case $singleCase }"""
            if param.name == paramRef.name =>
          singleCase match {
            case CaseTuploidValue(inputValue, body) =>
              Some((inputValue, body))

            case _ =>
              None
          }

        case q"""($param) => $body""" =>
          Some((ScalarValue(param.symbol), body))

        case _ =>
          println("COULDN'T RECOGNIZE closure: " + closure)
          None
      }
      inputValueAndBodyOpt match {
        case Some((inputValue, Block(statements, returnValue))) =>
          returnValue match {
            case TuploidValue(outputValue) =>
              val result = TransformationClosure(inputValue, statements, outputValue)
              Option(result).filterNot(_.hasTupleAliasReferencesInStatements)

            case _ =>
              println("COULDN'T RECOGNIZE return value of body: " + returnValue)
              None
          }

        case Some((inputValue, body)) =>
          println("COULDN'T RECOGNIZE TuploidValue case: (inputValue = " + inputValue + "): " + body)
          None

        case _ =>
          None
      }
    }
  }
  def parseTupleRewiringMapClosure(closure: Tree)
      : Option[(TuploidValue, List[Tree], TuploidValue)] = {

    val inputValueAndBodyOpt = closure match {
      case q"""($param) => (${paramRef @ Ident(_)}: $_) match { case $singleCase }"""
          if param.name == paramRef.name =>
        singleCase match {
          case CaseTuploidValue(inputValue, body) =>
            Some((inputValue, body))

          case _ =>
            None
        }

      case q"""($param) => $body""" =>
        Some((ScalarValue(param.symbol), body))

      case _ =>
        println("COULDN'T RECOGNIZE closure: " + closure)
        None
    }
    inputValueAndBodyOpt match {
      case Some((inputValue, Block(statements, returnValue))) =>
        println("returnValue = " + returnValue)
        val tupleAliases = inputValue.collectTupleAliases

        val hasTupleAliasReferencesInStatements =
          statements.exists(_.exists(t => tupleAliases.contains(t.symbol)))

        if (hasTupleAliasReferencesInStatements) {
          // println("HAS TUPLE ALIAS REFERENCED IN VALDEFS: " + valDefs)
          None
        } else {
          returnValue match {
            case TuploidValue(outputValue) =>
              Some((inputValue, statements, outputValue))

            case _ =>
              println("COULDN'T RECOGNIZE return value of body: " + returnValue)
              None
          }
        }

      case Some((inputValue, body)) =>
        println("COULDN'T RECOGNIZE TuploidValue case: (inputValue = " + inputValue + "): " + body)
        None

      case _ =>
        None
    }
  }
}
