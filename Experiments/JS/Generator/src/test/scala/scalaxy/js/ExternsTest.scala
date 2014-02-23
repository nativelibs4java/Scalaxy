package scalaxy.js

import org.junit._
import Assert._

import com.google.javascript.rhino.jstype.FunctionType

import com.google.javascript.rhino.Node
import com.google.javascript.jscomp._

class ExternsTest {
  
  def conv(src: String): String =
  	JavaScriptToScalaSignaturesGenerator(
  		List(SourceFile.fromCode("test.js", src)),
  		ownerName = "js",
  		filter = v => v.getInputName.equals("test.js"))

  def checkConversion(externs: String, scalaSource: String) {
  	def rep(s: String) = s.trim.replaceAll("""(?m)^\s+""", "")

  	assertEquals(rep(scalaSource), rep(conv(externs)))
  }
  @Test
  def simple {
  	checkConversion(
  		""" /** @constructor */
          var MyClass = function() {};

	        /** @this {MyClass}
	          * @param a {number}
	          * @param b {string}
	          * @param opt_c {?number}
	          * @return {number}
	          */
	        /** @type {function(number, string, ?number): number}
	          */
	        MyClass.prototype.f = function(a, b, opt_c) {};
      """,
  		""" package js
  				@scalaxy.js.global
	  			class MyClass extends js.Object {
	  				def f(a: Double, b: String, c: java.lang.Double): Double = ???
	  			}
  		""")
  }
  @Test
  def constructors {
  	checkConversion(
  		""" /** @constructor
            * @param a {!Array.<number>}
            */
          var MyClass = function(a) {};
      """,
  		""" package js
	  			@scalaxy.js.global
	  			class MyClass(a: js.Array[Double]) extends js.Object {
	  			}
  		""")
  }
}
