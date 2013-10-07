import org.junit._
import Assert._

class JSONTest {
  @Test
  def simple {
    import NewQuasiquotes._
    SomeTree match {
      case nq"$x + $y" => println((x, y))
    }
  }
}
