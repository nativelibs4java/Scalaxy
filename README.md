Collection of Scala Macro goodies ([BSD-licensed](https://github.com/ochafik/Scalaxy/blob/master/LICENSE))
- *[Compilets](https://github.com/ochafik/Scalaxy/tree/master/Compilets)* provide an easy way to express AST rewrites, backed by a compiler plugin and an sbt plugin.
- *[Loops](https://github.com/ochafik/Scalaxy/tree/master/Loops)* provide a macro that optimizes simple foreach loops by rewriting them to an equivalent while loop:

    ```scala
    import scalaxy.loops._
    
    for (i <- 0 until 100000000 optimized) { ... }
    ```

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
