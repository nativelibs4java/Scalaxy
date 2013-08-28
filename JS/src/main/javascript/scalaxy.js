goog.provide('scalaxy');

var scalaxy = {};

/**
 * Define a lazy final property (scala object or lazy val).
 *
 * @param {!Object} obj Object on which the property is defined.
 * @param {string} name Name of the property.
 * @param {function(): Object} builder Function that builds the property value.
 */
scalaxy.defineLazyFinalProperty = function(obj, name, builder) {
  Object.defineProperty(obj, name, {
    configurable: true,
    enumerable: true,
    get: function() {
      var value = builder();
      Object.defineProperty(obj, name, {
        configurable: false,
        enumerable: true,
        value: value
      });
      return value;
    }
  });
};

/**
 * @this {*}
 * @param {!Object} source
 */
scalaxy.importProperties = function(source) {
  for (var key in source) {
    if (source.hasOwnProperty(key)) {
      this[key] = source[key];
    }
  }
  return this;
};

/** @constructor */
scalaxy.Tuple = function() {
  this.values = arguments;
};

