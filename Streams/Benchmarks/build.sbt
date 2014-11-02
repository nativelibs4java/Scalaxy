scalaVersion := "2.11.4"

jmhSettings

autoCompilerPlugins := true

// addCompilerPlugin("com.nativelibs4java" %% "scalaxy-fastcaseclasses" % "0.4-SNAPSHOT")

// scalacOptions += "-Xplugin-require:scalaxy-fastcaseclasses"

// addCompilerPlugin("com.nativelibs4java" %% "scalaxy-streams" % "0.4-SNAPSHOT")

// scalacOptions += "-Xplugin-require:scalaxy-streams"

libraryDependencies += "com.nativelibs4java" %% "scalaxy-streams" % "0.4-SNAPSHOT"

scalacOptions ++= Seq("-optimise", "-Yclosure-elim", "-Yinline")

// scalacOptions += "-Xprint:cleanup"

// Scalaxy snapshots are published on the Sonatype repository.
resolvers += Resolver.sonatypeRepo("snapshots")
