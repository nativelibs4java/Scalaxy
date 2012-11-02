This contains some standard compilets and their tests.
*   `RangeLoops` rewrites `foreach` operations on ranges into much faster `while` loops (some limitations apply, compared to [ScalaCL](http://code.google.com/p/scalacl/): no support of filters yet, restricted to constant steps).
*   `ArrayLoops` rewrites `foreach` and `map` operations on `Array[A <: AnyRef]` and `Array[Int]` into much faster `while` loops.
*   `Numerics` optimizes some `Numeric[T]` DSL use-cases (optimizes the infixNumericOps object creation away). 
