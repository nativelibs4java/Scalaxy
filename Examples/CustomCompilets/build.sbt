name := "custom-compilets-example"

organization := "com.nativelibs4java"

version := "1.0-SNAPSHOT"
    
scalaVersion := "2.10.0-RC1"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies += "com.nativelibs4java" %% "scalaxy-api" % "0.3-SNAPSHOT"

libraryDependencies += "com.nativelibs4java" %% "scalaxy-plugin" % "0.3-SNAPSHOT" % "test" classifier("test")

libraryDependencies += "com.nativelibs4java" %% "scalaxy-plugin" % "0.3-SNAPSHOT" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test"
