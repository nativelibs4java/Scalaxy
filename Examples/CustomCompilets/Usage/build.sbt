scalaVersion := "2.10.0-RC1"

resolvers += Resolver.sonatypeRepo("snapshots")

autoCompilerPlugins := true

addCompilerPlugin("com.nativelibs4java" %% "scalaxy" % "0.3-SNAPSHOT")

addCompilerPlugin("com.nativelibs4java" %% "custom-compilets-example" % "1.0-SNAPSHOT")

scalacOptions += "-Xplugin-require:Scalaxy"

scalacOptions += "-Xprint:scalaxy-rewriter"
