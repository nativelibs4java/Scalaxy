scalaVersion := "2.10.3"

// If any library dependency includes a compilet, it will be automatically detected and used.
// If this is not set, compilets must be added explicitly with:
//
//     addCompilets("com.nativelibs4java" %% "some-other-compilets" % "1.0-SNAPSHOT")
//
autoCompilets := true

// Enable Scalaxy's basic loop & numerics rewrites.
addDefaultCompilets()
