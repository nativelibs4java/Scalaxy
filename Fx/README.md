# Scalaxy/Fx: JavaFX eye-candy experiment for Scala 2.10

Minimal set of Scala 2.10 macros, dynamics and implicits for maximal JavaFX eye-candy!

_Important_: this "library" was designed as a DSL with no runtime dependency (with the exception of [one class](https://github.com/ochafik/Scalaxy/blob/master/Fx/Runtime/src/main/scala/scalaxy/fx/runtime/ScalaChangeListener.scala) I couldn't get rid of yet, because of a [bug / limitation of Scala macros](https://issues.scala-lang.org/browse/SI-6386)).

This means that all the methods defined in the `scalaxy.fx` package are macros that rewrite the code to something pure-JavaFX, with no reference to any additional class. If you want to learn how to write non-trivial macros, [have a look at the code](https://github.com/ochafik/Scalaxy/tree/master/Fx/Macros/src/main/scala/scalaxy/fx)!

As a result, you may say think of `Scalaxy/Fx` as a compiler plugin rather than a library (but technically, it *is* just a library).

# Disclaimer

This library is a _very partial proof of concept_ (should work well, though).

If you're looking for a complete and supported JavaFX experience in Scala, please use [ScalaFX](http://code.google.com/p/scalafx/) (great mature library, although it doesn't use macros as of yet: its binding syntax may look a bit weird at times).

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
    val moo: ObservableDoubleValue = ...
    val foo = bind {
      Math.sqrt(moo.getValue)
    }
    val button = new Button().set(
      text = bind {
        s"Foo is ${foo.get}"
      },
      cancelButton = true
    )
    ```
        
  Instead of:
  
    ```scala
    val moo: ObservableDoubleValue = ...
    val foo = new DoubleBinding() {
      super.bind(moo)
      override def computeValue() = 
        Math.sqrt(moo.getValue)
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
      override def changed(observable: ObservableValue[Double], oldValue: Double, newValue: Double) {
        println("Constraint changed!")
      }
    }
    ```
