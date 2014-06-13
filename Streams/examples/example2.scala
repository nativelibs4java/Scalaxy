package example

object Example2 extends App {
  val n = 20;
  println {
    for (i <- 0 to n;
         ii = i * i;
         j <- i to n;
         jj = j * j;
         if (ii - jj) % 2 == 0;
         k <- (i + j) to n)
      yield { (ii, jj, k) }
  }
}
