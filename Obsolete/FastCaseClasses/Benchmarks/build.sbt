scalaVersion := "2.11.4"

jmhSettings

autoCompilerPlugins := true

addCompilerPlugin("com.nativelibs4java" %% "scalaxy-fastcaseclasses" % "0.4-SNAPSHOT")

scalacOptions += "-Xplugin-require:scalaxy-fastcaseclasses"

addCompilerPlugin("com.nativelibs4java" %% "scalaxy-streams" % "0.4-SNAPSHOT")

scalacOptions += "-Xplugin-require:scalaxy-streams"

scalacOptions ++= Seq("-optimise", "-Yclosure-elim", "-Yinline")

libraryDependencies += "com.nativelibs4java" %% "scalaxy-streams" % "0.4-SNAPSHOT" % "provided"

// scalacOptions += "-Xprint:scalaxy-streams"

resolvers += Resolver.sonatypeRepo("snapshots")
