# Scalaxy/Streams

Scalaxy/Streams makes your Scala collections code faster:
* Fuses collection streams down to while loops
* Avoids unnecessary tuples
* Usable as a compiler plugin (whole project) or as a macro (surgical strikes)

# TODO

* Transmit and use collection size though to builder hintSize
* Performance tests
* Test plugin as well as macros
* Support @optimize(true) / @optimize(false) / -Dscalaxy.streams.optimize=true/false/never/always

