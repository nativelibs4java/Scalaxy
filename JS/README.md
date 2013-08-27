Scalaxy/JS:

experiments of Scala -> JavaScript translation, using a macro annotation + Closure Compiler to generate all the externs, and another macro annotation to mark a compilation unit as to be converted.

For instance, the following file `Foo.scala`:

```scala
package scalaxy.js.example

import scalaxy.js._

// Import default browser externs from Closure Compiler
@JavaScriptExterns()
object Global

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
```

Will yield `Foo.js`:

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
```
