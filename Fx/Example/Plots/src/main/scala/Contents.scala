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
    
    val chart = new ScatterChart[Number, Number](xAxis,yAxis)

    chart.getData.setAll(
      buildXYSeries(files)(file => {
        // CSV file with column names in first line:
        if (file.getName.matches(".*?\\.(csv|tsv)")) {
          readCSV(file)("c", "b") {
            case Array(age, brightness) => (age.toDouble, brightness.toDouble)
          }
        } else {
          // Space-separated fields with % comments:
          // readFields(file) {
          //   case Array(a, b, c, d) => (age.toDouble, brightness.toDouble)
          // }
          readFields(file) {
             case a =>
                (a(0).toDouble, a(1).toDouble)
          }
        }
      })
    )
    chart
  }
}
