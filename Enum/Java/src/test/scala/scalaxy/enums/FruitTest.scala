package scalaxy.enums

import org.junit._
import org.junit.Assert._

object Fruits extends enum {
  class Fruit(val cost: Int) extends value
  // override type value = Fruit

  object Banana extends Fruit(1)
  object Orange extends Fruit(2)
  object Apple extends Fruit(3)
}

// object Fruits extends enum()(new EnumValueNames(
//   names = Array("Banana", "Orange", "Apple"),
//   initializer = e => {
//     // println("Init WITH " + e)
//     val f = e.asInstanceOf[Fruits.type]
//     Array(f.Banana, f.Orange, f.Apple)
//     })) {
//   class Fruit(val cost: Int) extends value
//   // override type value = Fruit

//   object Banana extends Fruit(1)
//   object Orange extends Fruit(2)
//   object Apple extends Fruit(3)
// }

object Fruits1 extends enum { val Apple, Banana = value }

object Fruits2 extends enum { val Orange, Banana = value }

class FruitTest {

  @Test
  def testSimple {
    assertEquals(Set("Apple", "Banana"), Fruits1.values.map(_.name).toSet)
    assertEquals(Seq(Fruits1.Apple, Fruits1.Banana), Fruits1.values.toSeq)

    assertFalse(Fruits1.Banana == Fruits2.Banana)
    assertEquals("Apple", Fruits1.Apple.name)
    assertEquals("Banana", Fruits1.Banana.name)

    assertEquals("Orange", Fruits2.Orange.name)
    assertEquals("Banana", Fruits2.Banana.name)
    assertEquals(Fruits1.Banana, Fruits1.valueOf("Banana"))

    // val values: Array[Fruits.Fruit] = Fruits.values
  }

  def serialize(v: AnyRef): Array[Byte] = {
    import java.io._
    val b = new ByteArrayOutputStream
    val o = new ObjectOutputStream(b)
    o.writeObject(v)
    o.close()
    b.toByteArray
  }

  def deserialize(arr: Array[Byte]): AnyRef = {
    import java.io._
    val b = new ByteArrayInputStream(arr)
    val o = new ObjectInputStream(b)
    o.readObject()
  }

  @Ignore // Blocked by ACC_ENUM flag.
  @Test
  def testClass {
    val c = classOf[Fruits1.value]
    assertNotNull("Null value class", c)
    assertTrue("value class is not enum", c.isEnum())
    val constants = c.getEnumConstants
    assertNotNull("No enum constants found", constants)
    assertEquals(Seq(Fruits1.Apple, Fruits1.Banana),
      constants.toSeq)
  }

  @Ignore // Blocked by ACC_ENUM flag.
  @Test
  def serializationTest {
    object E extends enum { val A, B = value }

    val a = serialize(E.B)
    assertSame(E.B, deserialize(a))
  }

  @Test
  def testCustomClass {

    assertEquals("Banana", Fruits.Banana.name)
    // println("GOT BANANA")

    assertEquals("Orange", Fruits.valueOf("Orange").name)
    
    assertEquals(Set("Banana", "Orange", "Apple"),
      Fruits.values.map(_.name).toSet)

    assertEquals(Seq(Fruits.Banana, Fruits.Orange, Fruits.Apple),
      Fruits.values.toSeq)
  }
}
