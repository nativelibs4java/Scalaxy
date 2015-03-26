function registerOnLoad(fn) {
  var prev = window.onload;
  window.onload = !prev ? fn : function() {
    var prevResult = prev();
    return prevResult instanceof Promise ? prevResult.then(fn) : fn();
  };
}

if (!this['Proxy']) {
  var PolyfillProxy = function(target, handlers) {
    Object.defineProperties(this, {
      '__polyfill_proxy_target': {
        value: target,
        enumerable: false,
        writable: false,
        configurable: false,
      },
      '__polyfill_proxy_handlers': {
        value: handlers,
        enumerable: false,
        writable: false,
        configurable: false,
      },
    });
    //return Object.freeze(this);
    var obj = Object.freeze(this);

    var f = function() {
      if (handlers.apply) {
        var args = [];
        for (var i = 0; i < arguments.length; i++) {
          args.push(arguments[i]);
        }
        return handlers.apply.apply(this, [target, args])
      } else {
        throw new TypeError('Proxy object cannot be used as a function as it does not have an "apply" handler.');
      }
    };
    f.__proto__ = obj;
    f.toString = function() { return target.toString(); };
    return f;
  };

  var Proxy = PolyfillProxy;

  PolyfillProxy.interceptProperties = function(props) {
    var descriptors = {};
    props.forEach(function(prop) {
      if (!(prop in PolyfillProxy.prototype)) {
        descriptors[prop] = {
          'get': function() {
            var handlers = this['__polyfill_proxy_handlers'];
            var target = this['__polyfill_proxy_target'];

            if (handlers.get) {
              return handlers.get(target, prop)
            }
            return target[prop];
          },
          'set': function(value) {
            var handlers = this['__polyfill_proxy_handlers'];
            var target = this['__polyfill_proxy_target'];

            if (handlers.set) {
              handlers.set(target, prop, value)
            } else {
              target[prop] = value;
            }
          }
        };
      }
    });
    // window.console.log('Setup interception of ' + Object.keys(descriptors) + ' properties in PolyfillProxy');
    Object.defineProperties(PolyfillProxy.prototype, descriptors)
  };

  /** @return {Promise} */
  PolyfillProxy.scanScripts = function(scripts) {
    if (!scripts) {
      scripts = document.getElementsByTagName('script');
    }
    var getScriptSourcePromise = function(script) {
      if (script.src) {
        return new Promise(function(resolve, reject) {
          var xhr = new XMLHttpRequest();
          xhr.open('GET', script.src, true);
          xhr.onload = function (e) {
            if (this.status === 200) {
              resolve(xhr.responseText);
            }
          };
          xhr.onerror = function (e) {
            reject(e);
          };
          xhr.send();
        })
      } else {
        return script.innerHTML;
      }
    };
    var promises = [];
    for (var i = 0; i < scripts.length; i++) {
      var script = scripts[i];
      if (!script['__polyfill_proxy_scanned']) {
        Object.defineProperty(script, '__polyfill_proxy_scanned', {
          value: true,
          enumerable: false
        });
        promises.push(getScriptSourcePromise(script));
      }
    }
    return Promise.all(promises).then(function(contents) {
      var set = {};
      var identifiers = [];
      contents.forEach(function(content) {
        // Replace block comments `/*...*/`.
        content = content.replace(/\/\*[\s\S]*?\*\//g, '')
        // Replace line comments `//...\n`.
        content = content.replace(/\/\/.*\n/g, '\n')
        var tokens = content.split(/\b/).filter(function(word) {
          return word.match(/\w+/) && !word.match(/\d+/);
        });
        return tokens.forEach(function(token) {
          if (!(token in set) && !token.match(/__polyfill_proxy_.*/)) {
            set[token] = true;
            identifiers.push(token);
          }
        });
      });
      PolyfillProxy.interceptProperties(identifiers);
      return true;
    });
  };

  registerOnLoad(PolyfillProxy.scanScripts);
}
