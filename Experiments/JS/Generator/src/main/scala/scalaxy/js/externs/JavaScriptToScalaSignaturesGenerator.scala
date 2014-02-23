package scalaxy.js

import scala.reflect.api.Universe

import com.google.javascript.jscomp.ScalaxyClosureUtils
import com.google.javascript.jscomp._

import scala.collection.JavaConversions._

class JavaScriptToScalaSignaturesGenerator(val global: Universe) extends TreeGenerators {

  import global._

  def generateSignatures[T <: Universe#Tree](sources: List[SourceFile], ownerName: String,  filter: Scope.Var => Boolean = null): List[T] = {

    implicit val externs = ScalaxyClosureUtils.scanExterns(sources)
    val globalVars = ExternsAnalysis.analyze(externs, filter)
    val generatedDecls = globalVars.classes.flatMap(classVars => {
      generateClass(classVars, ownerName: TermName)
    })

    generatedDecls.map(_.asInstanceOf[T])
  }
}

object JavaScriptToScalaSignaturesGenerator {
	def apply(sources: List[SourceFile], ownerName: String = "js", filter: Scope.Var => Boolean = null): String = {

		import scala.reflect.runtime.{ universe => ru }
	  val generator = new JavaScriptToScalaSignaturesGenerator(ru)

	  val sigs = generator.generateSignatures[ru.Tree](sources, ownerName, filter)

	  val src = "package " + ownerName + "\n" +
		  sigs.mkString("\n")
		  	.replaceAll("extends scala.AnyRef ", "")
		  	.replaceAll("@new ", "@")
		  	.replaceAll("""(?m)(?s)(class|trait) (\w+) ([^{]*\{).*?def (?:<init>|\$init\$)\(([^)]*)\) = \{[^}]*};?""",
		  		"$1 $2($4) $3")
		  	.replaceAll("""\(\) (class|object|abstract trait)""", "\n$1")
		  	.replaceAll("abstract trait", "trait")
		  	.replaceAll("""= \$qmark\$qmark\$qmark;?""", "= ???")
		  	.replaceAll("""= _;""", "= _")
		  	.replaceAll("""(trait|class) (\w+)\(\) """, "$1 $2 ")
		  	.replaceAll("""(va[rl]) (_\w+)\b""", "$1 `$2`")
		  	// .replaceAll(": Unit = ???;", "")
		  	.replaceAll("""(?m)(?s)def <init>\(\) = \{.*?\};""", "")
		  	.replaceAll("""(?m)(?s)def \$init\$\(\) = \{.*?\};""", "")
		  	.replaceAll("""[:,] (Array)\[""", """: scala.$1[""")
		  	.replaceAll("""\bOption\[""", """scala.Option[""")

	  // println(src)
	  src
	}
}
