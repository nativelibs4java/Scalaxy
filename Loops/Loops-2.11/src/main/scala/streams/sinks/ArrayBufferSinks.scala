package scalaxy.loops

private[loops] trait ArrayBufferSinks extends BuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  case object ArrayBufferSink extends BuilderSink
  {
    override def createBuilder(inputVars: TuploidValue[Tree]) =
      q"scala.collection.mutable.ArrayBuffer[${inputVars.tpe}]()"
  }
}
