final case class Complex(real: Double, imag: Double) {
  @inline
  def **(y: Complex) =
    Complex(
      this.real * y.real - this.imag * y.imag,
      this.real * y.imag + this.imag * y.real)

  @inline
  def ++(y: Complex) =
    Complex(this.real + y.real, this.imag + y.imag)
}

object NaiveImplicits 
{
  implicit class ComplexOps(self: Complex) {
    def module: Double =
      self.real * self.real + self.imag * self.imag

    def *(y: Complex): Complex =
      Complex(
        self.real * y.real - self.imag * y.imag,
        self.real * y.imag + self.imag * y.real)
  
    def +(y: Complex): Complex =
      Complex(self.real + y.real, self.imag + y.imag)
  }
}

object InlineImplicits 
{
  implicit class ComplexOps(self: Complex) {
    @inline
    def module: Double =
      self.real * self.real + self.imag * self.imag

    @inline
    def *(y: Complex): Complex =
      Complex(
        self.real * y.real - self.imag * y.imag,
        self.real * y.imag + self.imag * y.real)
  
    @inline
    def +(y: Complex): Complex =
      Complex(self.real + y.real, self.imag + y.imag)
  }
}

object MacroImplicits
{
  @scalaxy.extension[Complex]
  def module: Double =
    self.real * self.real + self.imag * self.imag

  @scalaxy.extension[Complex]
  def *(y: Complex): Complex =
    Complex(
      self.real * y.real - self.imag * y.imag,
      self.real * y.imag + self.imag * y.real)

  @scalaxy.extension[Complex]
  def +(y: Complex): Complex =
    Complex(self.real + y.real, self.imag + y.imag)
}

