Collection of Scala Macro goodies:
- *[Compilets](https://github.com/ochafik/Scalaxy/tree/master/Compilets)* provide an easy way to express AST rewrites, backed by a compiler and sbt plugins.
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

