package plots

import java.io.File

import javafx.application.Application
import javafx.scene._
import javafx.scene.chart._
import javafx.scene.control._
import javafx.scene.layout._
import javafx.stage._

import scala.io.Source
import scala.collection.JavaConversions._

import scalaxy.fx._

// sbt "~run first.csv second.csv"
object Plotter extends App {
  Application.launch(classOf[Plotter], args: _*)
}

class Plotter extends Application {
  override def start(primaryStage: Stage) {
    def scene = primaryStage.getScene
    primaryStage.set(
      title = "Plot",
      scene = new Scene(
        new StackPane() {
          getChildren.add(
            new BorderPane().set(
              center = Contents.buildChart(getParameters.getRaw.map(new File(_))),
              bottom = new Button().set(
                text = "Reload stylesheet",
                onAction = {
                  com.sun.javafx.css.StyleManager.getInstance().reloadStylesheets(scene)
                }
              )
            )
          )
        }, 
        500, 
        250
      )
    )
    scene.getStylesheets.add("Chart.css");
    primaryStage.show()
  }
}
