package scalaxy.js

import scala.reflect.api.Universe

import com.google.javascript.jscomp.ScalaxyClosureUtils
import com.google.javascript.jscomp._

import scala.collection.JavaConversions._

class JavaScriptToScalaSignaturesGenerator(val global: Universe) extends TreeGenerators {

  import global._

  def generateSignatures[T <: Universe#Tree](sources: List[SourceFile], ownerName: String): List[T] = {

    val externs = ScalaxyClosureUtils.scanExterns(sources)
    val globalVars = ExternsAnalysis.analyze(externs)
    val generatedDecls = globalVars.classes.flatMap(classVars => {
      generateClass(classVars, externs, ownerName: TermName)
    })

    generatedDecls.map(_.asInstanceOf[T])
  }
}
