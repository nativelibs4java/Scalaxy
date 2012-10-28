scalaVersion := "2.10.0-RC1"

resolvers += Resolver.sonatypeRepo("snapshots")

autoCompilerPlugins := true

addCompilerPlugin("com.nativelibs4java" %% "scalaxy-compiler-plugin" % "0.3-SNAPSHOT" classifier "assembly")

scalacOptions += "-Xplugin-require:Scalaxy"

scalacOptions += "-Xprint:scalaxy-rewriter"
