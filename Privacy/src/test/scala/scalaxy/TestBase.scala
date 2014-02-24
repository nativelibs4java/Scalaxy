package scalaxy.privacy.test

import scala.language.existentials

import scalaxy.privacy.PrivacyCompiler
import scala.tools.nsc.Global
import scala.reflect.internal.util.Position
import scala.tools.nsc.reporters.StoreReporter
import scala.tools.nsc.plugins.PluginComponent
import org.junit._
import org.junit.Assert._

trait TestBase {

  def getInternalPhases(global: Global): List[PluginComponent]

  def compile(src: String): List[String] = {
    import java.io._
    val f = File.createTempFile("test", ".scala")
    try {
      val out = new PrintWriter(f)
      out.print(src)
      out.close()

      val reporter: StoreReporter =
        PrivacyCompiler.compile(
          Array(f.toString, "-d", f.getParent),
          settings => new StoreReporter {
            override protected def info0(pos: Position, msg: String, severity: Severity, force: Boolean) {
              super.info0(pos, msg, severity, force)
              println(Position.formatMessage(pos, msg, shortenFile = true))
            }
          },
          getInternalPhases _)
      // println(reporter.infos.mkString("\n"))

      reporter.infos.toList.map(_.msg)
    } finally {
      f.delete()
    }
  }
}
