goog.require('scalaxy.lang.Class');
goog.require('scalaxy.lang.Object');

//goog.require('goog.assert');

/*
  trait A { def foo = "A" }
  trait B { def foo = "B" }

  class MixedA extends A
  class MixedSuperA extends A { override def foo = "super = " + super.foo }
  class MixedSuperAB_A extends A with B { override def foo = "super = " + super[A].foo }
  class MixedSuperAB_B extends A with B { override def foo = "super = " + super[B].foo }

  assertEquals("A",
    (new A {}).foo)

  assertEquals("super = A",
    (new A { override def foo = "super = " + super.foo }).foo)

  assertEquals("C",
    (new A with B { override def foo = "C" }).foo)

  assertEquals("super = A",
    (new A with B { override def foo = "super = " + super[A].foo }).foo)

  assertEquals("super = B",
    (new A with B { override def foo = "super = " + super[B].foo }).foo)
*/

/** @interface */
var A = function() {};
/**
 * @this {A}
 * @return {string}
 */
A.prototype.foo = function() {};
/** @type {function(): string} */
A.foo = function() { return "A"; };
scalaxy.lang.Class.defineClass({ name: "A", constructor: A });

/** @interface */
var B = function() {};
/**
 * @this {B}
 * @return {string}
 */
B.prototype.foo = function() {};
/** @type {function(): string} */
B.foo = function() { return "B"; };
scalaxy.lang.Class.defineClass({ name: "B", constructor: B });

/**
 * @constructor
 * @extends {scalaxy.lang.Object}
 * @implements {A}
 */
var mixedA = function() {
  goog.base(this);
};
goog.inherits(mixedA, scalaxy.lang.Object);
scalaxy.lang.Class.defineClass({
  name: "mixedA",
  constructor: mixedA,
  traits: [A],
  base: scalaxy.lang.Object
});

/**
 * @this {mixedA}
 * @return {string}
 */
mixedA.prototype.foo = A.foo;

/**
 * @constructor
 * @extends {scalaxy.lang.Object}
 * @implements {A}
 * @implements {B}
 */
var mixedAB = function() {
  goog.base(this);
};
goog.inherits(mixedAB, scalaxy.lang.Object);
scalaxy.lang.Class.defineClass({
  name: "mixedAB",
  constructor: mixedAB,
  traits: [A, B],
  base: scalaxy.lang.Object
});

/**
 * @this {mixedAB}
 * @return {string}
 */
mixedAB.prototype.foo = function() {
  return "super = " + A.foo.apply(this, []);
};

var ma = new mixedA()
window.console.log(ma.foo());
window.console.log('ma.isInstanceOf(A) = ' + ma.isInstanceOf(scalaxy.lang.Class.forName('A')));
window.console.log('ma.isInstanceOf(B) = ' + ma.isInstanceOf(scalaxy.lang.Class.forName('B')));

var mab = new mixedAB()
window.console.log(mab.foo());
window.console.log('mab.isInstanceOf(A) = ' + mab.isInstanceOf(scalaxy.lang.Class.forName('A')));
window.console.log('mab.isInstanceOf(B) = ' + mab.isInstanceOf(scalaxy.lang.Class.forName('B')));
