package scalaxy

trait Compilet {
  def runsAfter: Seq[Compilet] = Seq()
  
  def name = getClass.getName.replaceAll("\\$", "")
}

