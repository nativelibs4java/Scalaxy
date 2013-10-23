object RuntimeDSL {
  implicit class str(self: Int) extends AnyVal {
    def str = self.toString
  }
}
