Collection of Scala Macro goodies ([BSD-licensed](https://github.com/ochafik/Scalaxy/blob/master/LICENSE))
- *[Loops](https://github.com/ochafik/Scalaxy/tree/master/Loops)* provide a macro that optimizes simple foreach loops by rewriting them to an equivalent while loop:

    ```scala
    import scalaxy.loops._
    
    for (i <- 0 until 100000000 optimized) { ... }
    ```
- *[Debug](https://github.com/ochafik/Scalaxy/tree/master/Debug)* provides `assert`, `require` and `assume` macros that automatically add a useful message to the regular [Predef](http://www.scala-lang.org/api/current/index.html#scala.Predef$) calls.
- *[Extensions](https://github.com/ochafik/Scalaxy/tree/master/Extensions)* provides an extremely simple syntax to define extensions methods as macros:

    ```scala
    @extend(Int) def str: String = macro reify(self.splice.toString)
    ...
    println(10.str) // this is macro-expanded to `10.toString`
    ```

- *[Compilets](https://github.com/ochafik/Scalaxy/tree/master/Compilets)* provide an easy way to express AST rewrites, backed by a compiler plugin and an sbt plugin.
- *[Beans](https://github.com/ochafik/Scalaxy/tree/master/Beans)* are a nifty combination of Dynamics and macros that provide a type-safe eye-candy syntax to set fields of regular Java Beans in a Scala way (without any runtime dependency at all!):

    ```scala
    import scalaxy.beans._
    
    new MyBean().set(foo = 10, bar = 12)
    ```

- *[Fx](https://github.com/ochafik/Scalaxy/tree/master/Fx)* contains an experimental JavaFX DSL (with virtually no runtime dependency) that makes it easy to build objects and define event handlers:

    ```scala
    new Button().set(
      text = bind {
        s"Hello, ${textField.getText}"
      },
      onAction = {
        println("Hello World!")
      }
    )
    ```

# Discuss

If you have suggestions / questions:
- [@ochafik on Twitter](http://twitter.com/ochafik)
- [NativeLibs4Java mailing-list](groups.google.com/group/nativelibs4java)

You can also [file bugs and enhancement requests here](https://github.com/ochafik/Scalaxy/issues/new).

Any help (testing, patches, bug reports) will be greatly appreciated!
