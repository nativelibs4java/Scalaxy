object TestExtensions 
{  
  @extend(Int) def str: String = macro reify(self.splice.toString)
  /*
  @extend(Int) def str = macro reify(self.splice.toString)
  @extend(Int) def str = macro {
    ...
    reify(self.splice.toString)
  }
  */
  //println(10.str)
}
