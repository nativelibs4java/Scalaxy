package scalaxy
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

object GenericsAlgo extends App
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
    val values: Array[T]
  )
  
  object Matrix {
    def apply[T : Numeric : ClassTag](rows: Int, columns: Int): Matrix[T] =
      new Matrix[T](rows, columns, new Array[T](rows * columns))
  }
      
  @scalaxy.extension[Matrix[T]]
  def apply[T : Numeric : ClassTag](row: Int, col: Int): T =
    this.values(row * columns + col)
  
  @scalaxy.extension[Matrix[T]]
  def update[T : Numeric : ClassTag](row: Int, col: Int, value: T) {
    this.values(row * columns + col) = value
  }
  
  @scalaxy.extension[Matrix[T]]
  def *[T : Numeric : ClassTag](other: Matrix[T]): Matrix[T] = {
    assert(this.columns == other.columns, s"Mismatching sizes: $this * $other")
    val out = Matrix[T](this.rows, other.columns)
    for (i <- 0 until this.rows) {
      for (j <- 0 until other.columns) {
        var sum = 0.asInstanceOf[T]
        for (k <- 0 until this.columns) {
          sum += this(i, k) * other(k, j)
        }
        out(i, j) = sum
      }
    }
    out
  }
}
