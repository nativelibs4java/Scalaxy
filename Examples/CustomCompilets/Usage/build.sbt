scalaVersion := "2.10.0-RC1"

resolvers += Resolver.sonatypeRepo("snapshots")

autoCompilets := true

addCompilet("com.nativelibs4java" %% "custom-compilets-example" % "1.0-SNAPSHOT")

scalaxySettings

