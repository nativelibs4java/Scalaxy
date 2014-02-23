package scalaxy.js

import scala.language.implicitConversions
import com.google.javascript.jscomp._

import scala.collection.JavaConversions._

import com.google.javascript.rhino.Node
import com.google.javascript.rhino.JSTypeExpression
import com.google.javascript.rhino.JSDocInfo
import com.google.javascript.rhino.jstype._

class ClosureExterns(compiler: Compiler, val scope: Scope) {
  implicit def getType(texpr: JSTypeExpression): JSType = {
    if (texpr == null)
      null
    else
      texpr.evaluate(scope, compiler.getTypeRegistry)
  }

	implicit class JSDocInfoExtensions(t: JSDocInfo) {
		def parameters: List[(String, JSType)] = {
			t.getParameterNames.toList.map(n => n -> (t.getParameterType(n): JSType))
		}
		def returnType: JSType = t.getReturnType
	}

	implicit class FunctionTypeExtensions(t: FunctionType) {
		def parameters: List[(String, JSType)] = {
      for ((p, i) <- t.getParameters.toList.zipWithIndex) yield {
        val name = p.getString
        (if (name.trim.isEmpty) "param" + i else name) -> p.getJSType
      }
    }
	}
}
