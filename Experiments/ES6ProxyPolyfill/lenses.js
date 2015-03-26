
function makeLens(symbolName, ignoreUndef) {
  var Identity = {
    'get': function(root, justTestPresence) { return justTestPresence ? !!root : root; },
    'path': symbolName,
    'toString': function() { return this.path; },
  };

  var Handlers;
  Handlers = {
    'apply': function(obj, args) {
      var parentGet = obj.get;
      // TODO if path resolves to a Function in args[0], compose a function call!
      if (args.length == 1) {
        var target = args[0];
        var exists = parentGet(target, true);
        if (exists) {
          var result = parentGet(target, false);
          return result;
        }
      }
      if (ignoreUndef) return undefined;
      throw new TypeError("Does not exist: " + obj);
    },
    'get': function(obj, prop) {
      var parentGet = obj.get;
      var get = function(root, justTestPresence) {
        var prev = parentGet(root);
        var value;
        if (justTestPresence) {
          value = !!prev && prop in prev;
        } else {
          value = ignoreUndef && (typeof prev == 'undefined') ?
            undefined : prev[prop];
        }
        return value;
      };
      var f = function(root) {
        var self = get(root);
        var args = [];
        for (var i = 1; i < arguments.length; i++) {
          args.push(arguments[i]);
        }
        return self.apply(self, args);
      };
      f.get = get;
      f.path = obj.path + '.' + prop;
      f.toString = function() { return this.path; };
      return new Proxy(f, Handlers);
    }
  };
  return new Proxy(Identity, Handlers);
}

//_.foo.bar

var _ = makeLens('_', true);
var __ = makeLens('__', false);
