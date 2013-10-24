# Scalaxy/Union

Scalaxy/Union provides a type-class-based type union mechanism.
```scala
trait JSONType extends (String | Double | Array[JSONType] | Map[String, JSONType])

def serialize[T: JSONType#union](value: T) = "..."
```

# Whazzat mean?

Scalaxy/Union lets you define and type-check type unions.

It extends the mechanism of `=:=` and `<:<`, which are ways to require proofs that a type matches or derives from another type, by introducing the union-aware type operators `=|=` and `<|<`:
```scala
import scalaxy.union._

type Num = Byte | Short | Int | Long | Float | Double

def takesNums[T](n: T)(implicit evidence: T =|= Num) = ???
def takesStringsOrInts[T](v: T)(implicit evidence: T =|= (String, Int)) = ???
```

What's interesting here is that these evidence params are provided by a macro, which checks that `T` is one of the required types.

But wait, there's better: we can bring type-classes in to simplify the notation!
```scala
import scalaxy.union._

type FloatNumber[N] = N =|= (Float | Double)
def storeFloatingPoint[N: FloatNumber](n: N) = ???

type Payload[T] = T =|= (String | Double)
def take[P: Payload](value: P) = ???
```

And these type-classes can be defined with traits, which allows for recursive definitions:
```scala
import scalaxy.union._

trait JSONType extends (String | Double | Array[JSONType] | Map[String, JSONType])

def serialize[T: JSONType#Union](value: T) = "..."

serialize("blah") // OK
serialize(10.0) // OK
serialize(10) // FAILS
serialize('a') // FAILS
serialize(Array(Map("a" -> 10.0), "foo")) // OK
serialize(Array("a", 10)) // FAILS (10 is an Int) 
```
In this previous example, you might have noticed the `JSONType#Union` thingie. It's a dependent type defined on trait `|[A, B]` that makes this `serialize` declaration above equivalent to the following:
```scala
def serialize[T](value: T)
                (implicit ev: T =|= (String | Double | Array[JSONType] | Map[String, JSONType])) = "..."
```

Although this way of implementing union types has flaws (and not all "type types" are handled yet), it might prove handy for simple cases... and suggestions / comments are highly welcome :-)
