name := "scalaxy-dsl-example"

organization := "com.nativelibs4java"

version := "1.0-SNAPSHOT"
    
scalaVersion := "2.10.0-RC1"

// Tell sbt-scalaxy that we're defining compilets in this project (that will add all the dependencies we need, including for tests).
scalaxyCompilets := true
