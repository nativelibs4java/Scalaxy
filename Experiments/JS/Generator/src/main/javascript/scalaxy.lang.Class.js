goog.provide('scalaxy.lang.Class');
goog.require('scalaxy.lang');
goog.require('scalaxy.lang.Object');

/**
 * @typedef {{
 *    name: string,
 *    constructor: Function,
 *    base: (Object|undefined),
 *    traits: (Array.<Function>|undefined),
 *    module: (scalaxy.lang.Class|undefined)
 * }}
 */
scalaxy.lang.ClassDescriptor;

/**
 * Runtime representation of a Java / Scala class.
 *
 * @constructor
 * @param {scalaxy.lang.ClassDescriptor} desc Descriptor of the class in JSON form.
 */
scalaxy.lang.Class = function(desc) {

  /** @type {string} */
  this.name = desc.name;

  /** @type {Function} */
  this.constructor = desc.constructor;

  /** @type {Object} */
  this.base = desc.base || null;

  /** @type {scalaxy.lang.Class|undefined} */
  this.superclass_;

  /** @type {Array.<Function>} */
  this.traits = desc.traits || [];
  this.traits.forEach(function(t) {
    if (t instanceof scalaxy.lang.Object)
      throw new Error("Not a trait: " + t);
  })

  /** @type {scalaxy.lang.Class} */
  this.moduleClass = desc.module || null;
};

/**
 * @this {!scalaxy.lang.Class}
 * @return {string}
 */
scalaxy.lang.Class.prototype.getName = function() {
  return this.name;
};

/**
 * @this {!scalaxy.lang.Class}
 * @return {string}
 */
scalaxy.lang.Class.prototype.getSimpleName = function() {
  return this.name.split('.').splice(-1)[0];
};

/**
 * @this {!scalaxy.lang.Class}
 * @return {scalaxy.lang.Class}
 */
scalaxy.lang.Class.prototype.getSuperclass = function() {
  if (this.base) {
    this.superclass_ = this.base[scalaxy.CLASS_FIELD] || null;
  }
  return this.superclass_;
};

/**
 * @this {!scalaxy.lang.Class}
 * @return {boolean}
 */
scalaxy.lang.Class.prototype.isTrait = function() {
  // TODO: better define this?
  return !(this.constructor instanceof scalaxy.lang.Object);
};

/**
 * @this {!scalaxy.lang.Class}
 * @return {boolean}
 */
scalaxy.lang.Class.prototype.isModule = function() {
  // TODO: better define this?
  return !this.isTrait() && !goog.isDef(this.constructor.prototype);
};

/**
 * @this {!scalaxy.lang.Class}
 * @param {scalaxy.lang.Object} obj
 * @return {boolean}
 */
scalaxy.lang.Class.prototype.isInstance = function(obj) {
  if (!obj) {
    return false;
  }

  if (this.isTrait()) {
    var cls = obj.getClass();
    while (cls) {
      if (cls.traits.indexOf(this.constructor) >= 0) {
        return true;
      }
      cls = cls.getSuperclass();
    }
    return false;
  } else {
    return obj instanceof this.constructor
  }
};

/**
 * Class declarations
 *
 * @private
 */
scalaxy.lang.Class.classes_ = {};

/**
 * Declare a class
 *
 * @param {!scalaxy.lang.ClassDescriptor} desc
 * @return {!scalaxy.lang.Class}
 */
scalaxy.lang.Class.defineClass = function(desc) {
  var cls = new scalaxy.lang.Class(desc);

  Object.defineProperty(scalaxy.lang.Class.classes_, cls.getName(), {
    value: cls,
    enumerable: true
  });

  if (goog.isDefAndNotNull(cls.moduleClass)) {
    scalaxy.defineLazyFinalProperty(
        /** @type {!Object} */ (cls.constructor),
        scalaxy.COMPANION,
        /** @type {function()} */ (cls.moduleClass.constructor));
  }
  cls.constructor.prototype[scalaxy.CLASS_FIELD] = cls;
  return cls;
};

scalaxy.lang.Class.forName = function(name) {
  var cls = scalaxy.lang.Class.classes_[name];
  if (!cls) {
    throw new Error("ClassNotFoundException: " + name)
  }
  return cls;
};

/**
 * Equivalent to `classOf[type]` in Scala.
 *
 * @param {!Function} type
 */
scalaxy.lang.Class.classOf = function(type) {
  var cls = type[scalaxy.CLASS_FIELD];
  if (cls instanceof scalaxy.lang.Class) {
    return cls;
  }
  throw new Error("Not found: type " + type)
}

/** Shortcut */
scalaxy.lang.Class.Object = scalaxy.lang.Class.defineClass({
  name: "scalaxy.lang.Object",
  constructor: scalaxy.lang.Object,
  lazyBase: function() { return Object; }
});
