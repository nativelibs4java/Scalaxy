scalaVersion := "2.11.2"

//autoCompilerPlugins := true

// addCompilerPlugin("com.nativelibs4java" %% "scalaxy-streams" % "0.3.0")

// scalacOptions += "-Xplugin-require:scalaxy-streams"
// 
// scalacOptions += "-Xprint:scalaxy-streams"

scalacOptions += "-Xprint:patmat"


//libraryDependencies += "com.nativelibs4java" %% "scalaxy-streams" % "0.3.0"
libraryDependencies += "com.nativelibs4java" %% "scalaxy-streams" % "0.3.3" % "provided"

scalacOptions ++= Seq("-optimise", "-Yclosure-elim", "-Yinline")

// Scalaxy/Streams snapshots are published on the //Sonatype repository.
// resolvers += Resolver.sonatypeRepo("snapshots")
// resolvers += Resolver.sonatypeRepo("releases")

//resolvers += Resolver.defaultLocal

//resolvers += "New Release" at "https://oss.sonatype.org/content/repositories/comnativelibs4java-1023/"
