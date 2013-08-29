goog.provide('scalaxy.lang.Class');
goog.require('scalaxy.lang');
goog.require('scalaxy.lang.Object');

/**
 * Runtime representation of a Java / Scala class.
 *
 * @constructor
 * @param {{
 *    owner: Object,
 *    name: string,
 *    simpleName: string,
 *    parent: scalaxy.lang.Class,
 *    traits: Array.<scalaxy.lang.Class>,
 *    constructor: Function,
 *    module: scalaxy.lang.Class
 * }} desc Descriptor of the class in JSON form.
 */
scalaxy.lang.Class = function(desc) {

  /** @type {!Object} */
  this.owner = desc.owner || goog.global;

  /** @type {string} */
  this.name = desc.name;

  /** @type {string} */
  this.simpleName = desc.simpleName;

  /** @type {scalaxy.lang.Class} */
  this.parent = desc.parent;

  /** @type {Array.<!scalaxy.lang.Class>} */
  this.ownTraits = desc.traits;

  /**
   * @type {!Array.<!scalaxy.lang.Class>|undefined}
   * @private
   */
  this.traits_;

  /** @type {Function} */
  this.constructor = desc.constructor;

  /** @type {scalaxy.lang.Class} */
  this.moduleClass = desc.module;
};

/** @this {!scalaxy.lang.Class} */
scalaxy.lang.Class.prototype.getName = function() {
  return this.name;
};

/** @this {!scalaxy.lang.Class} */
scalaxy.lang.Class.prototype.getSimpleName = function() {
  return this.simpleName;
};

Object.defineProperty(scalaxy.lang.Class.prototype, 'traits', {
  /** @this {!scalaxy.lang.Class} */
  get: function() {
    if (!goog.isDef(this.traits_)) {
      var ownTraitsConstructors =  this.ownTraits.map(function(t) { return t.constructor; });
      // TODO: check linearization order.
      this.traits_ = (goog.isDefAndNotNull(this.parent) ? this.parent['traits'] : []).concat(this.ownTraits);
      // Mix trait methods in.
      // TODO: check linearization order + what happens of super calls.
      var existingMembers = {};
      Object.keys(this.constructor.prototype).forEach(function(k) {
        existingMembers[k] = true;
      });
      this.traits_.forEach(function(t) {
        for (var key in t) {
          if (!existingMembers[key]) {
            this.constructor.prototype[key] = t[key];
          }
        }
      })
    }
    return this.traits_;
  }
});

/**
 * Class declarations
 *
 * @private
 */
scalaxy.lang.Class.classes_ = {};

/**
 * Declare a class
 *
 * @param {!scalaxy.lang.Class} cls
 */
scalaxy.lang.Class.defineClass = function(cls) {
  Object.defineProperty(scalaxy.lang.Class.classes_, cls.getName(), {
    value: cls,
    enumerable: true
  });

  if (goog.isDefAndNotNull(cls.moduleClass)) {
    scalaxy.defineLazyFinalProperty(
        /** @type {!Object} */ (cls.constructor),
        'companion',
        /** @type {function()} */ (cls.moduleClass.constructor));
  }
  cls.constructor.prototype[scalaxy.CLASS_FIELD] = cls;
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

/**
 * Shortcut for Class.forName("java.lang.Object")
 */
scalaxy.lang.Class.Object =
    new scalaxy.lang.Class({
      owner: scalaxy.lang,
      name: "scalaxy.lang.Object",
      simpleName: "Object",
      parent: null,
      traits: [],
      constructor: scalaxy.lang.Object,
      members: {},
      module: null
    });

scalaxy.lang.Class.defineClass(scalaxy.lang.Class.Object);
