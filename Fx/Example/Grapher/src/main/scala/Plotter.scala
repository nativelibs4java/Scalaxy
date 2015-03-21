package plots

import java.io.File
import javafx.application.Application
import javafx.scene._
import javafx.scene.chart._
import javafx.scene.control._
import javafx.scene.input._
import javafx.scene.layout._
import javafx.stage._
import scala.io.Source
import javafx.collections.ObservableList
import scala.collection.JavaConversions._
import scalaxy.fx._
import javafx.beans.property.ObjectProperty

// sbt "~run first.csv second.csv"
object Plotter extends App {
  Application.launch(classOf[Plotter], args: _*)
}

object Eval {
  import scala.reflect.runtime.currentMirror
  import scala.tools.reflect.ToolBox
  val toolbox = currentMirror.mkToolBox()

  type DoubleFunction = Double => Double
  def getDoubleFunction(param: String, expr: String): DoubleFunction = {
    val tree = toolbox.parse(s"($param: Double) => $expr")
    toolbox.compile(tree)().asInstanceOf[DoubleFunction]
  }
}

class Plotter extends Application {

  def buildSeries(name: String, data: Seq[XYChart.Data[Number, Number]]) = {
    val series = new XYChart.Series[Number, Number]()
    series.setName(name)
    series.getData.addAll(data)//f(file))
    series
  }

  override def start(primaryStage: Stage) {
    import DSL._
    import Eval._
                
    def scene = primaryStage.getScene
    
    val xAxisField = new TextField("x")
    val yAxisField = new TextField("y")
    val xMinField = new TextField("0")
    val xMaxField = new TextField("10")
    
    val fField = new TextField("x * 2")
    val f = bind {
//      fField.text = "a"
      println("Evaluating: " + fField.text)
      try {
        Some(getDoubleFunction(xAxisField.text, fField.text))
      } catch {
        case ex: Throwable =>
          None
      }
    }
    
    val data = bind {
      println("Computing data")
      (for (x <- xMinField.text.toInt to xMaxField.text.toInt;
            ff <- f.getValue) yield {
        (x, ff(x)): XYChart.Data[Number, Number]
      }): Seq[XYChart.Data[Number, Number]]
    }
    primaryStage.set(
      title = "Plot",
      scene = new Scene(
        new StackPane() {
          getChildren.add(
            new BorderPane().set(
              top = vBox(
                  hBox(new Label("Function:"), fField),
                  hBox(new Label("X axis:"), xAxisField, xMinField, xMaxField),
                  hBox(new Label("Y axis:"), yAxisField)),
              center = {
                val xAxis = new NumberAxis().set(label = bind { xAxisField.text })
                val yAxis = new NumberAxis().set(label = bind { yAxisField.text })
                
                print(xAxis.width)
                // To use an explicit range / tick unit instead, use:
                // val xAxis = new NumberAxis("Age", 0, 100, 4)
                
                //val chart = new ScatterChart[Number, Number](xAxis,yAxis)
                val chart = new LineChart[Number, Number](xAxis,yAxis)
//                chart.set(data = bind {
////                  val xMin = xMinField.text.toInt
////                  val xMax = xMaxField.text.toInt
////                  (for (x <- xMin to xMax; ff <- f.getValue) yield {
//                  ((for (x <- xMinField.text.toInt to xMaxField.text.toInt;
//                        ff <- f.getValue) yield {
//                    (x, ff(x)): XYChart.Data[Number, Number]
//                  }): Seq[XYChart.Data[Number, Number]]): ObservableList[XYChart.Data[Number, Number]]
//                }) 
                data onChange {
                  chart.getData.setAll(
                      List(
                        buildSeries(fField.text, data.getValue)))
                }
                
//                chart.onDragDone = (e: DragEvent) => println(e)                
//                chart.setOnDragDone(e -> System.out.println(e));
//                chart.getData.setAll(List(
//                    buildSeries("f", Seq((0, 0), (1, 20), (10, 30)))//.map(IntXYData))
//                ))
                chart
              },
              bottom = new Button().set(
                text = "Reload stylesheet",
                onAction = {
//                  com.sun.javafx.css.StyleManager.getInstance().reloadStylesheets(scene)
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
