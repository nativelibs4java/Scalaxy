package plots

import scalaxy.fx._
import java.io.File

import scala.collection.JavaConversions._

trait XYDataDSL {
  import javafx.scene.chart.XYChart
  
  def buildXYSeries
        (files: Seq[File])
        (f: File => Seq[XYChart.Data[Number, Number]]) 
      : Seq[XYChart.Series[Number, Number]] = 
  {
    for (file <- files) yield {
      val series = new XYChart.Series[Number, Number]()
      series.setName(file.getName.replaceAll("\\.[^.]*$", ""))
      series.getData.addAll(f(file))
      series
    }
  }
  
  implicit def XYData[X, Y, A <% X, B <% Y](t: (A, B)) =
    new XYChart.Data[X, Y](t._1, t._2)
}

trait PieChartDataDSL {
  import javafx.scene.chart.PieChart
  implicit def PieChartData(t: (String, Double)) =
    new PieChart.Data(t._1, t._2)
}

trait FileDSL {
  implicit def file(s: String): File = new File(s)
}

object DSL 
  extends XYDataDSL 
     with PieChartDataDSL 
     with FileDSL
     
