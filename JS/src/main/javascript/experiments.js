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

//goog.provide("test.Test");
scalaxy.lang.Class.declareClass(
    new scalaxy.lang.Class({
      owner: test,
      name: "test.Test",
      simpleName: "Test",
      parent: scalaxy.lang.Class.Object,
      traits: scalaxy.lang.Class.NO_TRAITS,
      constructor: function(x) {
        this.x = x;
        window.console.log('Constructing Test with x = ' + this.x)
      },
      members: {
        blah: function() {
          window.console.log('blah ' + this.x)
        }
      },
      module: null
    }));
