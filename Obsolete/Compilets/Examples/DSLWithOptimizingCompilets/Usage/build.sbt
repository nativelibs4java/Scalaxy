scalaVersion := "2.10.3"

autoCompilets := true

// We don't care to rewrite loops in this example:
//addDefaultCompilets()

// Since this specific compilet will rewrite the DSL away, we mark the DSL library dependency as "provided" (it's not needed at run-time).
libraryDependencies += "com.nativelibs4java" %% "scalaxy-dsl-example" % "1.0-SNAPSHOT" % "provided"

