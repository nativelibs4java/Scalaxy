package scalaxy.native

import java.io._

private[native] trait IOUtils
{
  def withFileOutputStream[A](file: File, f: FileOutputStream => A): A = {
    val out = new FileOutputStream(file)
    try {
      f(out)
    } finally {
      out.close()
    }
  }
  def writeFile(file: File, src: String) {
    withFileOutputStream(file, out => new PrintStream(out).print(src))
  }

  def readFile(file: File): String =
    io.Source.fromFile(file).mkString

  def withTempFile[A](prefix: String, suffix: String, f: File => A): A = {
    val file = File.createTempFile(prefix, suffix)
    file.deleteOnExit()
    try {
      f(file)
    } finally {
      file.exists() && file.delete()
    }
  }
}
