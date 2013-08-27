var defineLazyFinalProperty = function(obj, name, builder) {
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

defineLazyFinalProperty(this, "Sing3", function() {
  window.console.log("BUILDING");
  var Sing3 = {};
  Sing3.foo = 'bar';
  return Sing3;
});
