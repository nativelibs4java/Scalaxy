package scalaxy
import scalaxy.loops._
import scala.language.postfixOps // Optional.

package object generics
{
  @scalaxy.extension[T] 
  def +[T : Numeric](other: T): T = 
    this + other
  
  @scalaxy.extension[T] 
  def -[T : Numeric](other: T): T = 
    this - other
    
  @scalaxy.extension[T] 
  def *[T : Numeric](other: T): T = 
    this * other
    
  @scalaxy.extension[T] 
  def /[T : Numeric](other: T): T = 
    this / other
  
  @scalaxy.extension[T] 
  def %[T : Numeric](other: T): T = 
    (this % other).asInstanceOf[T]
    
  @scalaxy.extension[T] 
  def &[T : Numeric](other: T): T = 
    (this & other).asInstanceOf[T]
    
  @scalaxy.extension[T] 
  def |[T : Numeric](other: T): T = 
    (this | other).asInstanceOf[T]
  
  @scalaxy.extension[T] 
  def <[T : Ordering](other: T): Boolean = 
    this < other
  
  @scalaxy.extension[T] 
  def <=[T : Ordering](other: T): Boolean = 
    this <= other
  
  @scalaxy.extension[T] 
  def >[T : Ordering](other: T): Boolean = 
    this > other
  
  @scalaxy.extension[T] 
  def >=[T : Ordering](other: T): Boolean = 
    this >= other
}

object ExampleAlgo
{
  import generics._
  
  // This leverages some of the macros above.
  @scalaxy.extension[T] 
  def divAddMul[T : Numeric](div: T, add: T, mul: T): T =
    (this / div + add) * mul
}

object Matrices {
  import generics._
  import scala.reflect.ClassTag
  
  final class Matrix[T : Numeric : ClassTag](
    val rows: Int, 
    val columns: Int, 
    val values: Array[T])
  {
    private def get(row: Int, col: Int): T =
      this.values(row * columns + col)
      
    override def toString: String = {
      val b = new StringBuilder()
      for (i <- 0 until rows optimized) {
        b ++= "{ "
        for (j <- 0 until columns optimized) {
          b ++= get(i, j).toString
          if (j != columns - 1)
            b ++= ", "
        }
        b ++= "}"
        if (i != rows - 1)
          b ++= ","
        b ++= "\n"
      }
      b.toString
    }
  }
  
  object Matrix {
    def apply[T : Numeric : ClassTag](rows: Int, columns: Int): Matrix[T] =
      new Matrix[T](rows, columns, new Array[T](rows * columns))
  }
      
  @scalaxy.extension[Matrix[T]]
  def apply[T : Numeric : ClassTag](row: Int, col: Int): T =
    this.values(row * this.columns + col)
  
  @scalaxy.extension[Matrix[T]]
  def update[T : Numeric : ClassTag](row: Int, col: Int, value: T) {
    this.values(row * this.columns + col) = value
  }
  
  @scalaxy.extension[Matrix[T]]
  def *[T : Numeric : ClassTag](other: Matrix[T]): Matrix[T] = {
    require(
      this.columns == other.rows, 
      s"Mismatching sizes: (${this.rows} x ${this.columns}) * (${other.rows} x ${other.columns})")

    //TODO: debug: val out: Matrix[T] = ... 
    val out = Matrix[T](this.rows, other.columns)
    for (i <- 0 until this.rows optimized) {
      for (j <- 0 until other.columns optimized) {
        var sum = 0.asInstanceOf[T]
        for (k <- 0 until this.columns optimized) {
          sum += this(i, k) * other(k, j)
        }
        out(i, j) = sum
      }
    }
    out
  }
}
