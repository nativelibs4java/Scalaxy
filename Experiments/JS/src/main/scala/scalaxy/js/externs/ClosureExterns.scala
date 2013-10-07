package scalaxy.js

import scala.language.implicitConversions
import com.google.javascript.jscomp._

import scala.collection.JavaConversions._

import com.google.javascript.rhino.Node
import com.google.javascript.rhino.JSTypeExpression
import com.google.javascript.rhino.jstype._

class ClosureExterns(compiler: Compiler, val scope: Scope) {
  implicit def getType(texpr: JSTypeExpression): JSType = {
    if (texpr == null)
      null
    else
      texpr.evaluate(scope, compiler.getTypeRegistry)
  }
}
