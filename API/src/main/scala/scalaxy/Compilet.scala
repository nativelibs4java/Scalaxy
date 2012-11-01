package scalaxy

trait Compilet {
  def runsAfter: Seq[Compilet] = Seq()
}

