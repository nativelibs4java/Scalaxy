package scalaxy.streams

private[streams] trait StreamTransforms
  extends Streams
  with StreamSources
  with StreamSinks
  with StreamOps
  with SideEffectsDetection
