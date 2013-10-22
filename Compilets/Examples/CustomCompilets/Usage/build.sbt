scalaVersion := "2.10.3"

autoCompilets := true

addDefaultCompilets()

//resolvers += Resolver.sonatypeRepo("snapshots")

addCompilets("com.nativelibs4java" %% "custom-compilets-example" % "1.0-SNAPSHOT")

scalacOptions += "-Xplugin-require:Scalaxy"

scalacOptions += "-Xprint:scalaxy-rewriter"
