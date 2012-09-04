import sbt._
import Keys._

object Scalaxy extends Build 
{
  override lazy val settings = super.settings ++ Seq(
    shellPrompt := { s => Project.extract(s).currentProject.id + "> " }
  ) ++ scalaSettings

  lazy val standardSettings =
    Defaults.defaultSettings ++ 
    infoSettings ++
    compilationSettings ++ 
    mavenSettings ++
    scalaSettings ++
    commonDepsSettings
    
  lazy val infoSettings = Seq(
    organization := "com.nativelibs4java",
    version := "0.3-SNAPSHOT",
    licenses := Seq("BSD-3-Clause" -> url("http://www.opensource.org/licenses/BSD-3-Clause")),
    homepage := Some(url("http://ochafik.com/blog/"))
  )
  lazy val compilationSettings = Seq(
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
    javacOptions ++= Seq("-Xlint:unchecked") 
  )
  lazy val mavenSettings = Seq(
    publishMavenStyle := true,
    
    //publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("-SNAPSHOT")) 
        Some("snapshots" at nexus + "content/repositories/snapshots") 
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { _ => false }
  )
  lazy val scalaSettings = Seq(
    //scalaVersion := "2.10.0-SNAPSHOT",
    //crossScalaVersions := Seq("2.10.0-M2"),
    scalaVersion := "2.10.0-M7"
    //exportJars := true, // use jars in classpath
  )
  lazy val commonDepsSettings = Seq(
    libraryDependencies += "junit" % "junit" % "4.10" % "test",
    libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test"
    //libraryDependencies <+= scalaVersion(v => "org.scalatest" % ("scalatest_2.9.1"/* + v*/) % "1.7.1" % "test")
    //libraryDependencies += "org.scala-tools.testing" %% "scalacheck" % "1.9" % "test"
  )
  
  lazy val scalaxy = 
    Project(id = "scalaxy", base = file("."), settings = standardSettings ++ Seq(
      scalacOptions in console in Compile <+= (packageBin in compilerPlugin in Compile) map("-Xplugin:" + _)
    )).
    dependsOn(core, macros, compilets, compilerPlugin).
    aggregate(core, macros, compilets, compilerPlugin)

  lazy val compilerPlugin = 
    Project(id = "scalaxy-compiler-plugin", base = file("Compiler"), settings = standardSettings ++ Seq(
      scalacOptions in console in Compile <+= (packageBin in Compile) map("-Xplugin:" + _)
      //,libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)
    )).
    dependsOn(core, macros, compilets)
    
  lazy val core = 
    Project(id = "scalaxy-core", base = file("Core"), settings = standardSettings ++ Seq(
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)
      //,libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)
    )).
    dependsOn(macros)
                 
  lazy val compilets = 
    Project(id = "scalaxy-compilets", base = file("Compilets"), settings = standardSettings).
    dependsOn(macros)
                 
  lazy val macros = 
    Project(id = "scalaxy-macros", base = file("Macros"), settings = standardSettings ++ Seq(
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _),
      //libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
      scalacOptions ++= Seq("-language:experimental.macros")
    )) 
}
