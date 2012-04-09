import sbt._
import Keys._

object ScalaxyBuild extends Build 
{
  override lazy val settings = super.settings :+
    (shellPrompt := { s => Project.extract(s).currentProject.id + "> " })

  lazy val standardSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.nativelibs4java",
    version := "0.3-SNAPSHOT",
    
    scalaVersion := "2.10.0-SNAPSHOT",
    //scalaVersion := "2.10.0-M2",
    
    resolvers += Resolver.sonatypeRepo("snapshots"),
    
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
    javacOptions ++= Seq("-Xlint:unchecked")
  )
 
  lazy val scalaxy = 
    Project(id = "scalaxy", base = file("."), settings = standardSettings ++ Seq(
      scalacOptions in console in Compile <+= (packageBin in Compile) map("-Xplugin:" + _)
    )).
    dependsOn(core, macros).
    aggregate(core, macros)

  lazy val core = 
    Project(id = "scalaxy-core", base = file("Core"), settings = standardSettings ++ Seq(
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)
    )).
    dependsOn(macros)
                 
  lazy val macros = 
    Project(id = "scalaxy-macros", base = file("Macros"), settings = standardSettings ++ Seq(
      scalacOptions += "-Xmacros"
    )) 
}
