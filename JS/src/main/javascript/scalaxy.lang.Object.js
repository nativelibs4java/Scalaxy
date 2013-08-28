goog.provide('scalaxy.lang.Object');
goog.require('scalaxy.lang');

/**
 * All Scala classes must derive from this.
 *
 * @constructor
 */
scalaxy.lang.Object = function() {};

/** @type {!scalaxy.lang.Class} */
scalaxy.lang.Object.prototype.class_;

/**
 * @this {!scalaxy.lang.Object}
 * @param {!scalaxy.lang.Class} type
 * @return {boolean}
 */
scalaxy.lang.Object.prototype.isInstanceOf = function(type) {
  if (this instanceof type) {
    return true;
  }
  var cls = this.getClass();
  return cls['traits'].indexOf(type) >= 0;
};

/**
 * @this {!scalaxy.lang.Object}
 * @return {!scalaxy.lang.Class}
 */
scalaxy.lang.Object.prototype.getClass = function() {
  return this.class_;
};
