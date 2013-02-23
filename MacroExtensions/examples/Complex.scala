case class Complex(real: Double, imag: Double)/* {
  @scalaxy.macro 
  def *(y: Complex) =
    Complex(
      x.real * y.real - x.imag * y.imag,
      x.real * y.imag + x.imag * y.real)

  @scalaxy.macro
  def +(y: Complex) =
    Complex(x.real + y.real, x.imag + y.imag)
}*/

object ComplexImplicits 
{
  @scalaxy.extension[Complex]
  def *(y: Complex): Complex =
    Complex(
      self.real * y.real - self.imag * y.imag,
      self.real * y.imag + self.imag * y.real)

  @scalaxy.extension[Complex]
  def +(y: Complex): Complex =
    Complex(self.real + y.real, self.imag + y.imag)
}

