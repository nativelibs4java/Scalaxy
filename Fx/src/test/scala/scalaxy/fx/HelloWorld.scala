package scalaxy.helloworld

import scala.language.reflectiveCalls

import org.junit._
import org.junit.Assert._

import scalaxy.fx._

import javafx.application.Application
import javafx.scene._
import javafx.scene.control._
import javafx.scene.layout._
import javafx.stage._

/*
import scalaxy.fx._

import javafx._
import javafx.event._

object HelloWorld extends App {
  Application.launch(classOf[HelloWorld])
}

class HelloWorld extends Application {
  override def start(primaryStage: Stage) {
    primaryStage.set(
      title = "Hello World!",
      scene = new Scene(
        new StackPane() {
          getChildren.add(
            new Button().set(
              text = "Say 'Hello World'",
              onAction = {
                println("Hello World!")
              }
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

*/

class HelloWorldTest {
  @Test
  def launch {
    //Application.launch(classOf[HelloWorld])
  }
}

object HelloWorld extends App {
  Application.launch(classOf[HelloWorld])
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
                tooltip = new Tooltip("Hover me"),
                onAction = {
                  println("Hello World!")
                }
              ),
              center = slider,
              top = new Label().set(
                text = bind(s"Slider is at ${slider.getValue.toInt}")
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
