var scalaxy = {};

/**
 * Define a lazy final property (scala object or lazy val).
 *
 * @param {*} obj Object on which the property is defined.
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
