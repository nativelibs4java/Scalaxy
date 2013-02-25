package plots

import java.io.File

import javafx.scene._
import javafx.scene.chart._

import scala.collection.JavaConversions._
import scalaxy.fx._

import CSV._
import DSL._
    
object Contents {
  def buildChart(files: Seq[File]): Chart = {
    // These are auto-range axis:
    val xAxis = new NumberAxis().set(label = "Age")
    val yAxis = new NumberAxis().set(label = "Brightness")
    // To use an explicit range / tick unit instead, use:
    // val xAxis = new NumberAxis("Age", 0, 100, 4)
    
    val chart = new LineChart[Number, Number](xAxis,yAxis)
    chart.getData.setAll(
      readXYSeries(files: _*)(file => {
        readCSV(file)("c", "b") {
          case Array(age, brightness) => (age.toDouble, brightness.toDouble)
        }
      })
    )
    chart
  }
}
