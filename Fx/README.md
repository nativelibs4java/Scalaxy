# Scalaxy/Fx: JavaFX eye-candy experiment for Scala 2.10

Minimal set of Scala 2.10 macros, dynamics and implicits for maximal JavaFX eye-candy!
(does not depend on the rest of Scalaxy)

_Important_: this "library" was designed as a DSL with no runtime dependency (with the exception of [one class](https://github.com/ochafik/Scalaxy/blob/master/Fx/Runtime/src/main/scala/scalaxy/fx/runtime/ScalaChangeListener.scala) I couldn't get rid of yet, because of a [bug / limitation of Scala macros](https://issues.scala-lang.org/browse/SI-6386)).

This means that all the methods defined in the `scalaxy.fx` package are macros that rewrite the code to something pure-JavaFX, with no reference to any additional class. If you want to learn how to write non-trivial macros, [have a look at the code](https://github.com/ochafik/Scalaxy/tree/master/Fx/Macros/src/main/scala/scalaxy/fx)!

As a result, you may say think of `Scalaxy/Fx` as a compiler plugin rather than a library (but technically, it *is* just a library of macros).

# Disclaimer

This library is a _very limited proof of concept without proper tests_ (should work mostly well, though).

If you're looking for a complete and supported JavaFX experience in Scala, please use [ScalaFX](http://code.google.com/p/scalafx/) (great mature library written by [Stephen Chin](https://twitter.com/steveonjava/) and other committers, although it doesn't use macros as of yet and hence has some more exotic syntax for bindings).

# Example

```scala
import scalaxy.fx._

import javafx._
import javafx.event._

object HelloWorld extends App {
  Application.launch(classOf[HelloWorld], args: _*)
}

class HelloWorld extends Application {
  override def start(primaryStage: Stage) {
    val slider = new Slider().set(
      min = 0,
      max = 100,
      blockIncrement = 1,
      value = 50
    )
    slider.valueProperty onChange {
      println("Slider changed")
    }
    primaryStage.set(
      title = "Hello World!",
      scene = new Scene(
        new StackPane() {
          getChildren.add(
            new BorderPane().set(
              bottom = new Button().set(
                text = "Say 'Hello World'",
                onAction = {
                  println("Hello World!")
                }
              ),
              center = slider,
              top = new Label().set(
                text = bind {
                  s"Slider is at ${slider.getValue.toInt}"
                }
              )
            )
          )
        }, 
        300, 
        250
      )
    )
    primaryStage.show()
  }
}
```
    
# Usage

To use with `sbt` 0.12.2+, please have a look at the [HelloWorld example](https://github.com/ochafik/Scalaxy/blob/master/Fx/Example) and make your `build.sbt` file look like:

```scala
// Only works with 2.10.0+
scalaVersion := "2.10.0"

// Add JavaFX Runtime as an unmanaged dependency, hoping to find it in the JRE's library folder.
unmanagedJars in Compile ++= Seq(new File(System.getProperty("java.home")) / "lib" / "jfxrt.jar")

// This is the bulk of Scalaxy/Fx, needed only during compilation (no runtime dependency here).
libraryDependencies += "com.nativelibs4java" %% "scalaxy-fx" % "0.3-SNAPSHOT" % "provided"

// This runtime library contains only one class needed for the `onChange { ... }` syntax.
// You can just remove it if you don't use that syntax.
libraryDependencies += "com.nativelibs4java" %% "scalaxy-fx-runtime" % "0.3-SNAPSHOT" 

// JavaFX doesn't cleanup everything well, need to fork tests / runs.
fork := true

// Scalaxy snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
```
    
# Features

The syntactic facilities available so far are:
- JavaFX Script-like syntax for setters (without any runtime penalty or loss of type-safetiness): 

    ```scala
    button.set(
      text = "Say 'Hello World'",
      tooltip = new Tooltip("Hover me"),
      minHeight = 300,
      minHidth = 200
    )
    ```
        
- More natural bindings:

    ```scala
    val moo = newProperty(10)
    val foo = bind { Math.sqrt(moo.get) }
    val button = new Button().set(
      text = bind { s"Foo is ${foo.get}" },
      cancelButton = true
    )
    ```
        
  Instead of:
  
    ```scala
    val moo = new SimpleIntegerProperty(10)
    val foo = new DoubleBinding() {
      super.bind(moo)
      override def computeValue() = 
        Math.sqrt(moo.get)
    }
    val button = new Button()
    button.textProperty.bind(
      new StringBinding() {
        super.bind(foo)
        override def computeValue() = 
          s"Foo is ${foo.get}"
      }
    ),
    button.setCancelButton(true)
    ```
      
- Simpler syntax for event handlers, with or without the event parameter:

    ```scala
    button1.set(
      text = "Click me!",
      onAction = println("clicked")
    )
    
    button2.set(
      text = "Click me!",
      onAction = (event: ActionEvent) => {
        println(s"clicked: $event")
      }
    )
    
    button2.maxWidthProperty onChange {
      println("Constraint changed!")
    }
    ```
        
  Instead of:
  
    ```scala
    button3.setText("Click me!")
    button3.setOnAction(new EventHandler[ActionEvent]() {
      override def handle(event: ActionEvent) {
        println(s"clicked: $event")
      }
    }
    button3.maxWidthProperty.addListener(new ChangeListener[Double]() {
      override def changed(observable: ObservableValue[_ <: Double], oldValue: Double, newValue: Double) {
        println("Constraint changed!")
      }
    }
    ```
    
# Internals

`Scalaxy/Fx` uses some interesting techniques:
- Combination of `Dynamic` and macros for a type-safe setters syntax for Java beans, that uses named parameters.
  (see [Scalaxy/Beans](https://github.com/ochafik/Scalaxy/tree/master/Beans) and [my blog post](http://ochafik.com/blog/?p=803) on the matter for more details on this technique)
- [CanBuildFrom-style implicits](https://github.com/ochafik/Scalaxy/blob/master/Fx/Macros/src/main/scala/scalaxy/fx/GenericTypes.scala) to associate value types `T` to their `Binding[T]` or `Property[T]` subclasses.
  What's interesting here is that there is no implementation of these evidence objects, which only serve to the typer and are thrown away by the macros.
  
  As a result, the following property and binding are correctly typed to their concrete implementation:
  
    ```scala
    import scalaxy.fx._
    
    val p: SimpleIntegerProperty = newProperty(10)
    val b: IntegerBinding = bind { p.get + 10 }
    ```
  
- Despite it not being officially supported, creates anonymous handler classes from inside macros.
  (see [EventHandlerMacros.scala](https://github.com/ochafik/Scalaxy/blob/master/Fx/Macros/src/main/scala/scalaxy/fx/impl/EventHandlerMacros.scala))
- Macros all over, for virtually no runtime dependency.
  The only exception is that it's not currently possible to have unbound types in macros due to [a bug in reifiers](https://issues.scala-lang.org/browse/SI-6386), so the following will crash compilation because of the `[_ <: T]` part:
  
    ```scala
    val valueExpr = c.Expr[ObservableValue[T]](value)
    reify(
      valueExpr.splice.addListener(
        new ChangeListener[T]() {
          override def changed(observable: ObservableValue[_ <: T], oldValue: T, newValue: T) {
            f.splice(oldValue, newValue)
          }
        }
      )
    )
    ```
    
# Hacking

If you want to build / test / hack on this project:
- Make sure to use [paulp's sbt script](https://github.com/paulp/sbt-extras) with `sbt` 0.12.2+
- Install Oracle's JDK + JavaFX and make sure the `java` command in the path points to that version
- Use the following commands to checkout the sources and build the tests continuously: 

    ```
    git clone git://github.com/ochafik/Scalaxy.git
    cd Scalaxy
    sbt "project scalaxy-fx" "; clean ; ~test"
    ```

