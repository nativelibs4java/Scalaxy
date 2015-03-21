package plots

import scalaxy.fx._
import java.io.File
import scala.collection.JavaConversions._
import javafx.scene._
import javafx.scene.layout._
import javafx.beans.property._
import javafx.collections.ObservableList
import javafx.beans.binding.ListExpression
import javafx.collections.FXCollections

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
    
  implicit def IntIntXYData(t: (Int, Int)) =
    new XYChart.Data[Number, Number](Integer.valueOf(t._1), Integer.valueOf(t._2))
    
  implicit def IntDoubleXYData(t: (Int, Double)) =
    new XYChart.Data[Number, Number](Integer.valueOf(t._1), java.lang.Double.valueOf(t._2))
}

trait PieChartDataDSL {
  import javafx.scene.chart.PieChart
  implicit def PieChartData(t: (String, Double)) =
    new PieChart.Data(t._1, t._2)
}

trait FileDSL {
  implicit def file(s: String): File = new File(s)
}

trait LayoutDSL {
  def vBox(nodes: Node*) = {
    val b = new VBox()
    b.getChildren.addAll(nodes)
    b
  }
  def hBox(nodes: Node*) = {
    val b = new HBox()
    b.getChildren.addAll(nodes)
    b
  }
}

//trait CommonPropertiesDSL {
//  implicit class HasTextPropertyExtensions(h: { def textProperty(): StringProperty }) {
//    // TODO: As a macro, please!!!
//    def text = h.textProperty.get
//  }
//}
//trait SAMInterfacesDSL {
//  import javafx.event._
//  implicit class EventHandlerFunctionWrapper[T <: Event](f: T => Unit) extends EventHandler[T] {
//    def handle(event: T) {
//      f(event)
//    }
//  }
//}
trait CollectionsDSL {
//  implicit def SeqToObservableList[A](seq: Seq[A]): ObservableList[A] = {
//    val list = FXCollections.observableArrayList[A]()
//    seq.foreach(list.add _)
//    list
//  }
  import javafx.collections._
  import javafx.collections.transformation._


  implicit class ObservableListExtensions[A](col: ObservableList[A]) {
    def map[B](f: A => B): ObservableList[B] = {
      val res = FXCollections.observableArrayList[B]()
      col.addListener(new ListChangeListener[A]() {
        override def onChanged(change: ListChangeListener.Change[A]) {
          while (change.next()) {
            val from = change.getFrom
            val to = change.getTo
            if (change.wasPermutated()) {
              assert(to - from == 2)
              val tmp = res.get(from)
              res.set(from, res.get(to - 1))
              res.set(to - 1, tmp)
//              for (i <- from until to) {
//                //permutate
//              }
            } else if (change.wasUpdated()) {
              for (i <- from until to) {
                res.update(i, f(col.get(i)))
              }
            } else {
              for (removed <- change.getRemoved) {
                // TODO
//                remitem.remove(Outer.this);
              }
              for (added <- change.getAddedSubList) {
                // TODO
//                additem.add(Outer.this);
              }
            }
         }
       }
     })
//      new TransformationList[B, A](col) {
//        override def onSourceChanged(change: ListChangeListener.Change[A]) {
//          fireChange(new ListChangeListener.Change[B](this) {
//            
//          })
//        }
//        override def getSourceIndex(index: Int) = index
//      }
      res
    }  
  }

}

object DSL 
  extends XYDataDSL 
     with PieChartDataDSL 
     with FileDSL
     with LayoutDSL
     with CollectionsDSL
//     with SAMInterfacesDSL
//     with CommonPropertiesDSL
     
