Example of how to use the custom DSL and its compilet.

Notice that we just need to add a dependency to the DSL library, since we set `autoCompilets := true` in the `build.sbt` file (the `sbt-scalaxy` Sbt plugin will automatically detect that this library contains compilets, and will use them during compilation). 
