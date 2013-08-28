goog.provide('scalaxy.lang.Class');
goog.require('scalaxy.lang');
goog.require('scalaxy.lang.Object');

 // * @param {!Object|string} ownerFullName Full name or value of owner.
 // * @param {string} simpleName
 // * @param {scalaxy.lang.Class} parent
 // * @param {!Array.<!scalaxy.lang.Class>} ownTraits
 // * @param {!Function} constructor
 // * @param {!Object} ownMembers TODO: doesn't work with objects under traits, which need to be unique per resulting class.


/**
 * Representation of a Scala class.
 *
 * @constructor
 * @param {{
 *    owner: (!Object|string),
 *    name: string,
 *    simpleName: string,
 *    parent: scalaxy.lang.Class,
 *    traits: Array.<scalaxy.lang.Class>,
 *    constructor: Function,
 *    members: !Object,
 *    module: scalaxy.lang.Class
 * }} desc Descriptor of the class in JSON form.
 */
scalaxy.lang.Class = function(desc) {

  /** @type {!Object|string} */
  this.ownerFullName = desc.owner;

  /** @type {string} */
  this.fullName = desc.name;

  /** @type {string} */
  this.simpleName = desc.simpleName;

  /**
   * @type {Object}
   * @private
   */
  this.owner_;

  /** @type {scalaxy.lang.Class} */
  this.parent = desc.parent;

  /** @type {Array.<!scalaxy.lang.Class>} */
  this.ownTraits = desc.traits;

  /**
   * @type {!Array.<!scalaxy.lang.Class>|undefined}
   * @private
   */
  this.traits_;

  /**
   * @type {Function}
   * @private
   */
  this.constructor_ = desc.constructor;

  /**
   * @type {boolean}
   * @private
   */
  this.defined_ = false;

  /** @type {!Object} */
  this.ownMembers = desc.members;

  /** @type {scalaxy.lang.Class} */
  this.moduleClass = desc.module;
};

/** @this {!scalaxy.lang.Class} */
scalaxy.lang.Class.prototype.getName = function() {
  return this.fullName;
};

/** @this {!scalaxy.lang.Class} */
scalaxy.lang.Class.prototype.getSimpleName = function() {
  return this.simpleName;
};

/** @this {!scalaxy.lang.Class} */
scalaxy.lang.Class.prototype.defineClass = function() {
  this.defined_ = true;

  /** @type {!scalaxy.lang.Class} */
  var cls = this;
  scalaxy.defineLazyFinalProperty(this['owner'], this.simpleName, function() {
    var proto = cls.constructor.prototype = new cls['parentConstructor']();
    var module = cls.constructor;

    proto.class_ = cls;

    // Import parent and trait methods in linearization order.
    // TODO: check linearization order.
    var parents = [cls.parent].concat(cls['traits'])
    parents.forEach(scalaxy.importProperties, proto);

    scalaxy.importProperties.apply(proto, cls.ownMembers);

    if (goog.isDefAndNotNull(cls.moduleClass)) {
      scalaxy.defineLazyFinalProperty(module, 'module', cls.moduleClass['constructor']);
    }

    return cls.constructor;
  });
};

Object.defineProperty(scalaxy.lang.Class.prototype, 'constructor', {
  /** @this {!scalaxy.lang.Class} */
  get: function() {
    if (!this.defined_) {
      this.defineClass();
    }
    return this.constructor_;
  }
});

Object.defineProperty(scalaxy.lang.Class.prototype, 'owner', {
  /** @this {!scalaxy.lang.Class} */
  get: function() {
    if (!goog.isDef(this.owner_)) {
      if (goog.isString(this.ownerFullName)) {
        this.owner_ = /** @type {Object} */ (eval(this.ownerFullName));
      } else {
        this.owner_ = this.ownerFullName;
      }
    }
    return this.owner_;
  }
});

Object.defineProperty(scalaxy.lang.Class.prototype, 'traits', {
  /** @this {!scalaxy.lang.Class} */
  get: function() {
    if (!this.traits_) {
      var ownTraitsConstructors =  this.ownTraits.map(function(t) { return t.constructor; });
      // TODO: check linearization order.
      this.traits_ = (goog.isDefAndNotNull(this.parent) ? this.parent['traits'] : []).concat(this.ownTraits)
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
scalaxy.lang.Class.declareClass = function(cls) {
  Object.defineProperty(scalaxy.lang.Class.classes_, cls.fullName, {
    value: cls,
    enumerable: true
  });
};

scalaxy.lang.Class.forName = function(name) {
  var cls = scalaxy.lang.Class.classes_[name];
  if (!cls) {
    throw new Error("ClassNotFoundException: " + name)
  }
  return cls;
};

scalaxy.lang.Class.NO_TRAITS = [];
scalaxy.lang.Class.NO_MEMBERS = {};
scalaxy.lang.Class.NO_MODULE = undefined;

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

scalaxy.lang.Class.declareClass(
    scalaxy.lang.Class.Object);

scalaxy.lang.Object.prototype.class_ =
    scalaxy.lang.Class.Object;
