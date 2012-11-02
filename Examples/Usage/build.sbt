scalaVersion := "2.10.0-RC1"

resolvers += Resolver.sonatypeRepo("snapshots")

autoCompilerPlugins := true

addCompilerPlugin("com.nativelibs4java" %% "scalaxy" % "0.3-SNAPSHOT")

scalacOptions += "-Xplugin-require:Scalaxy"

scalacOptions += "-Xprint:scalaxy-rewriter"

//scalacOptions += "-P:Scalaxy:compilets=scalaxy.compilets.RangeLoops"
