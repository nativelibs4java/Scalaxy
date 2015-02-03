# Scalaxy/JSON

Macro-based JSON string interpolation, to create JSON objects as well as to extract values from them.

Currently uses [json4s](https://github.com/json4s/json4s), but decoupling is in progress.

```scala
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scalaxy.json.json4s.jackson._

// import org.json4s.native.JsonMethods._
// import scalaxy.json.json4s.native._

val a = 10
val b = "123"

// Two ways to create objects: named params or string interpolation.
// Both are macro expanded into the corresponding JSON creation code.
// No parsing takes place at runtime, it all happens during compilation.
// The resulting JSON is renormalized during compilation, so it's possible to drop some noisy quotes.
val obj = json(x = a, y = b)
val obj2 = json"{ x: $a, y: $b }"
// {
//   "x" : 10.0,
//   "y" : "123"
// }

// Same for arrays:
val arr = json(a, b)
val arr2 = json"[$a, $b]"
// [ 10.0, "123" ]

// Extraction also works, but currently requires some runtime parsing.
// Runtime improvements will come in 2.11 using https://issues.scala-lang.org/browse/SI-5903.
val json"{ x: $x, y: $y }" = obj


// JSON syntax errors are reported as they should: as Scala compilation errors!
json"{ x: $x, y': $y }"
// error: Unexpected character ('''): was expecting a colon to separate field name and value
//             json"{ x: $x, y': $y }"
//                            ^
```

For more examples, [have a look at the tests](https://github.com/ochafik/Scalaxy/blob/master/JSON/src/test/scala/scalaxy/json/JSONTest.scala).

# Features

- `json` string interpolation that is macro-expanded (type-safe and with no runtime parsing)
- `json` string interpolation extractor
- Smart error reporting for `json` string interpolation when using the `json4s.jackson`: JSON parsing errors are seamlessly transformed to Scala compiler errors, with the correct location.

# TODO

- `json` string interpolation should create something between a string and a JSON object (it currently creates an object), so as to optimize the .toString use case.
- Current matching is exact (all the extracted keys must exist, and no other must exist).
  By introducing a `...` notation, it will be possible to accept more unexpected keys.

# Usage

If you're using `sbt` 0.13.0+, just put the following lines in `build.sbt`:
```scala
scalaVersion := "2.11.5"

// Dependency at compilation-time only (not at runtime).
libraryDependencies += "com.nativelibs4java" %% "scalaxy-json-json4s-jackson" % "0.3-SNAPSHOT" % "provided"

// libraryDependencies += "com.nativelibs4java" %% "scalaxy-json-json4s-native" % "0.3-SNAPSHOT" % "provided"

// Scalaxy/JSON snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
```

# Hacking

If you want to build / test / hack on this project:
- Make sure to use [paulp's sbt script](https://github.com/paulp/sbt-extras) with `sbt` 0.12.2+
- Use the following commands to checkout the sources and build the tests continuously:

    ```
    git clone git://github.com/ochafik/Scalaxy.git
    cd Scalaxy
    sbt "project scalaxy-json" "; clean ; ~test"
    ```
