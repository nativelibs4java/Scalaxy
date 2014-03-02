package scalaxy.loops

private[loops] trait Utils {

  def trySome[T](v: => T): Option[T] =
    try {
      Some(v)
    } catch { case ex: Throwable =>
      ex.printStackTrace
      None
    }

}
