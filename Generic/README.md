Provides a `Generics[A]` typeclass that behaves a bit like `Numeric[A]`, except it allows for any method to be called (using `scala.Dynamic`).

Also provides AST simplifications that let Scalaxy/Reified erase `Numeric[A]` and `Generic[A]` away when `A` is known (which is the case when passing `TypeTag[A]` around. 
