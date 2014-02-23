goog.provide('scalaxy.lazy');
goog.require('scalaxy');

scalaxy.lazy = {};

/**
 * @type {Object}
 * @private
 */
scalaxy.lazy.builders_ = {};

/**
 * @type {Object}
 * @private
 */
scalaxy.lazy.values_ = {};

/**
 * @param {string} fullName
 * @param {!Object} owner
 * @param {string} name
 * @param {function(): Object} builder
 */
scalaxy.lazy.declareLazy = function(owner, name, fullName, builder) {
  scalaxy.lazy.builders_[fullName] = builder;
  Object.defineProperty(owner, name, {
    configurable: true,
    enumerable: true,
    get: function() {
      var value = scalaxy.lazy.get(fullName, builder);
      Object.defineProperty(owner, name, {
        enumerable: true,
        value: value
      });
      return value;
    }
  });
};

/**
 * @param {string} fullName
 * @param {(function(): Object)=} opt_builder
 */
scalaxy.lazy.get = function(fullName, opt_builder) {
  var value = scalaxy.lazy.values_[fullName];
  if (!value) {
    var builder = opt_builder || scalaxy.lazy.builders_[fullName];
    if (!builder) {
      throw new Error("No such lazy value: " + fullName);
    } else {
      scalaxy.lazy.builders_[fullName] = undefined;
    }
    value = builder();
    if (!value) {
      throw new Error("Lazy value builders must not evaluate to falsey value.");
    }
    scalaxy.lazy.values_[fullName] = value;
  }
  return value;
};
