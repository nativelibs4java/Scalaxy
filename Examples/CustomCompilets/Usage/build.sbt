scalaVersion := "2.10.0-RC1"

autoCompilets := true

addDefaultCompilets()

//resolvers += Resolver.sonatypeRepo("snapshots")

addCompilets("com.nativelibs4java" %% "custom-compilets-example" % "1.0-SNAPSHOT")

