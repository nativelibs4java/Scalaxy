goog.provide('scalaxy.lang.Object');
goog.require('scalaxy.lang');

/**
 * All Scala classes must derive from this.
 *
 * @constructor
 */
scalaxy.lang.Object = function() {
  var cls = this.getClass();
  // Call trait constructors on this.
  cls.traits.forEach(function(t) { t.apply(this); }, this);
};
goog.inherits(scalaxy.lang.Object, Object);

/**
 * Equivalent to `obj.isInstanceOf[type]` in Scala.
 *
 * @this {!scalaxy.lang.Object}
 * @param {!scalaxy.lang.Class} type
 * @return {boolean}
 */
scalaxy.lang.Object.prototype.isInstanceOf = function(type) {
  return type.isInstance(this);
};

/**
 * @this {!scalaxy.lang.Object}
 * @return {!scalaxy.lang.Class}
 */
scalaxy.lang.Object.prototype.getClass = function() {
  return this[scalaxy.CLASS_FIELD];
};
