package scalaxy.enums

import org.junit._
import org.junit.Assert._

// object Fruits extends enum {
//   class Fruit(val cost: Int) extends value
//   // override type value = Fruit

//   object Banana extends Fruit(1)
//   object Orange extends Fruit(2)
//   object Apple extends Fruit(3)
// }

object Fruits extends enum()(new EnumValueNames(
  names = Array("Banana", "Orange", "Apple"),
  initializer = e => {
    val f = e.asInstanceOf[Fruits.type]
    Array(f.Banana, f.Orange, f.Apple)
    })) {
  class Fruit(val cost: Int) extends value
  // override type value = Fruit

  object Banana extends Fruit(1)
  object Orange extends Fruit(2)
  object Apple extends Fruit(3)
}

// object Fruits1 extends enum {
//   val Apple, Banana = value
// }


// object Fruits2 extends enum {
//   val Orange, Banana = value
// }


// object Fruits2 extends enum {
//   //val (Apple, Orange) = values
//   val Apple = value
//   val Banana = value {

//   }
//   val Array(
//     apple: Type,
//     banana: Type,
//     orange: Type) = enum values
//     //Array("a", "b", "o")//build//values
// }

class FruitTest {

  // @Test
  // def testSimple {
  //   assertEquals(Set("Apple", "Banana"), Fruits1.values.map(_.name).toSet)
  //   assertEquals(Seq(Fruits1.Apple, Fruits1.Banana), Fruits1.values.toSeq)

  //   assertFalse(Fruits1.Banana == Fruits2.Banana)
  //   assertEquals("Apple", Fruits1.Apple.name)
  //   assertEquals("Banana", Fruits1.Banana.name)

  //   assertEquals("Orange", Fruits2.Orange.name)
  //   assertEquals("Banana", Fruits2.Banana.name)
  //   assertEquals(Fruits1.Banana, Fruits1.valueOf("Banana"))
  // }

  // @Test
  // def testCustomClass {
  //   assertEquals(Seq(Fruits.Banana, Fruits.Orange, Fruits.Apple),
  //     Fruits.values.toSeq)

  //   assertEquals(Set("Banana", "Orange", "Apple"),
  //     Fruits.values.map(_.name).toSet)
  // }
}
