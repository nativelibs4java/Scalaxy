package example

object Example2 extends App {
  // val n = 20;
  // println {
  //   for (i <- 0 to n;
  //        ii = i * i;
  //        j <- i to n;
  //        jj = j * j;
  //        if (ii - jj) % 2 == 0;
  //        k <- (i + j) to n)
  //     yield { (ii, jj, k) }
  // }
  

  // val col = (0 to 10).toList
  // val arr = col.map(_ * 2).toArray
  (0 to 10).map(_ * 2).toList
  // (0 to 10).toList
  // val n = 5
  // println(for (i <- 0 until n; v <- (i to 0 by -1).toArray; j <- 0 until v) yield {
  //   (i, v, j)
  // })
  // print(arr.mkString)
}
