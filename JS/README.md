Scalaxy/JS:

experiments of Scala -> JavaScript translation, using a macro annotation + Closure Compiler to generate all the externs, and another macro annotation to mark a compilation unit as to be converted.

First, we define externs in `Global.scala`:

```scala
package scalaxy.js.example

import scalaxy.js._

// Import default browser externs from Closure Compiler
@JavaScriptExterns()
object Global
```

Then we can write a file `Example.scala`:

```scala
package scalaxy.js.example

import scalaxy.js._

// This annotation actually affects all the code from this compilation unit.
@JavaScript
object Example {
  import Global._

  println("hello, world")
  println("hello, world, again")
  window.alert("I'm here")
  lazy val iAmLazy = {
    println("You've called me!")
    10
  }
  val someProperty = "fooo"
}

@global
object Main {
  println("This is run directly!")
  class Sub {
    println("Creating a sub class")
  }
  println(new Sub)
}

```

Which will be compiled to `Example.js`:

```javascript
if (!scalaxy) var scalaxy = {};
if (!scalaxy.js) scalaxy.js = {};
if (!scalaxy.js.example) scalaxy.js.example = {};
scalaxy.defineLazyFinalProperty(scalaxy.js.example, 'Example', function() {
  return (function() {
    var Example = {};
    (function() {
      window.console.log('hello, world');
      window.console.log('hello, world, again');
      window.alert('I\'m here');
      scalaxy.defineLazyFinalProperty(this, 'iAmLazy ', function() {
        return (function() {
          window.console.log('You\'ve called me!');
          return 10;
        })();
      });
      var someProperty  = 'fooo';
      Example.someProperty = someProperty;
    }).apply(Example);;
    return Example;
  })();
});

window.console.log('This is run directly!')
scalaxy.js.example.Sub = function() {
  window.console.log('Creating a sub class');
};
window.console.log(new scalaxy.js.example.Sub())
```

Note that code completion will work fine, and full type checks are performed on the files.

For better performance, prevent `scalac` from finishing compilation:
```
scalac -Xstop-after:typer -cp scalaxy-js.jar *.scala
```

TODO

- Generate `.set(a: Int = 0, b: Int = 0)` methods for all setters

Hacking

- Get Closure Library:

  ```
  mkdir JS/thirdparty/
  git clone https://code.google.com/p/closure-library JS/thirdparty/closure-library
  ```

- Get Closure Compiler:

  ```
  mkdir JS/thirdparty/
  git clone https://code.google.com/p/closure-compiler JS/thirdparty/closure-compiler
  cd JS/thirdparty/closure-compiler && ant
  ```

- Compile JS runtime (to check for errors and test `JS/example.js`):

  ```
  JS/bin/closure target/javascript/scalaxy.js JS/src/main/javascript/*.js
  ```

- Compile JS runtime with optimizations:

  ```
  OPT=1 JS/bin/closure target/javascript/scalaxy.js JS/src/main/javascript/*.js && ls -l target/javascript/scalaxy.js*
  ```
