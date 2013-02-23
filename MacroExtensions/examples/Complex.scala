//package complex {
  case class Complex(real: Double, imag: Double)
//}

object comp 
{
  @scalaxy.extension[void]
  def *(y: Complex): Complex =
    Complex(
      self.real * y.real - self.imag * y.imag,
      self.real * y.imag + self.imag * y.real)

  @scalaxy.extension[void]
  def +(y: Complex): Complex =
    Complex(self.real + y.real, self.imag + y.imag)
}
/*
  implicit class ComplexOps(x: Complex) {
    @scalaxy.macro
    def *(y: Complex) =
      Complex(
        x.real * y.real - x.imag * y.imag,
        x.real * y.imag + x.imag * y.real)

    @scalaxy.macro
    def +(y: Complex) =
      Complex(x.real + y.real, x.imag + y.imag)
  }
*/

