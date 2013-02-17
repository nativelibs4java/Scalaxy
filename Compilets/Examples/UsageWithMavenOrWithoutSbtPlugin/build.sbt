name := "scalaxy-example"

organization := "com.nativelibs4java"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.0"

resolvers += Resolver.sonatypeRepo("snapshots")

autoCompilerPlugins := true

addCompilerPlugin("com.nativelibs4java" %% "scalaxy-compilets-plugin" % "0.3-SNAPSHOT")

scalacOptions += "-Xplugin-require:Scalaxy"

scalacOptions += "-Xprint:scalaxy-rewriter"

