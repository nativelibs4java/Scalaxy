
import scala.collection.generic.CanBuildFrom

object TestRun extends App {

  import scalaxy.reified._
  import scalaxy.reified.stream._

  val src = Seq(1, 2, 3)

  val s = Stream[Int, Seq[Int], Seq[Int]](src, IdOpList())

  println(s)

  val ts = TraversableStream[Int, Seq[Int], Int, Seq[Int]](s)
  implicit val cbf = implicitly[CanBuildFrom[Seq[Int], Int, Seq[Int]]]: Reified[CanBuildFrom[Seq[Int], Int, Seq[Int]]]
  // val s2 = ts.map[Int, Seq[Int]]((_: Int) * 2)
  // val s3 = s.map(_ * 2)

  // println(s2)
  //    val stream: Stream[Int, Seq[Int], Seq[Int]] = Stream(src)
  //    stream: 

}
