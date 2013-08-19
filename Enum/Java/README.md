This sub-module attempts to provide a Scala implementation of Java enums.

It's only based on Scala 2.10.x def macros, and as such it is unable to set the ACC_ENUM flag on the enum class, but it provides some of the appearances of enums.

Simple example:
```
object Vegetables extends enum {
  val Tomato, Cauliflower, Potatoe = value
}
```

Example with custom enum value class:
```
object Fruits extends enum {
  class Fruit(val cost: Int) extends value

  object Banana extends Fruit(1)
  object Orange extends Fruit(2)
  object Apple extends Fruit(3)
}
```
