goog.require("scalaxy.lang.Class");
goog.require("scalaxy.lang.Object");


/*
package test
class Test(val x: Int) [extends AnyRef] {
  def blah = println("blah " + x)

  println("Constructing Test with x = " + x)
}
*/

if (!test) var test = {};

/**
 * @constructor
 * @extends {scalaxy.lang.Object} 
 * @param {number} x
 */
test.Test = function(x) {
  goog.base(this);
  this.x = x;
  window.console.log('Constructing Test with x = ' + this.x)
};
goog.inherits(test.Test, scalaxy.lang.Object);

scalaxy.lang.Class.defineClass({
  name: "test.Test",
  constructor: test.Test,
  base: scalaxy.lang.Object
});

/**
 * @this {!test.Test}
 */
test.Test.prototype.blah = function() {
  window.console.log('blah ' + this.x)
};
