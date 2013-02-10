import sbt._
import Keys._
import sbtassembly.Plugin._ ; import AssemblyKeys._
import ls.Plugin._

object Scalaxy extends Build
{
  lazy val scalaSettings = Seq(
    //exportJars := true, // use jars in classpath
    scalaVersion := "2.10.0",
    //scalaVersion := "2.11.0-SNAPSHOT",
    crossScalaVersions := Seq(
      "2.11.0-SNAPSHOT"))

  lazy val infoSettings = Seq(
    organization := "com.nativelibs4java",
    version := "0.3-SNAPSHOT",
    licenses := Seq("BSD-3-Clause" -> url("http://www.opensource.org/licenses/BSD-3-Clause")),
    homepage := Some(url("https://github.com/ochafik/Scalaxy")),
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <scm>
        <url>git@github.com:ochafik/Scalaxy.git</url>
        <connection>scm:git:git@github.com:ochafik/Scalaxy.git</connection>
      </scm>
      <developers>
        <developer>
          <id>ochafik</id>
          <name>Olivier Chafik</name>
          <url>http://ochafik.com/</url>
        </developer>
      </developers>
    ),
    (LsKeys.docsUrl in LsKeys.lsync) <<= homepage,
    (LsKeys.tags in LsKeys.lsync) := 
       Seq("compiler-plugin", "rewrite", "ast", "transform", "optimization", "optimisation"),
    (description in LsKeys.lsync) :=
      "A scalac compiler plugin that optimizes the code by rewriting for loops on ranges into while loops, avoiding some implicit object creations when using numerics...",
    LsKeys.ghUser := Some("ochafik"),
    LsKeys.ghRepo := Some("Scalaxy"))

  lazy val standardSettings =
    Defaults.defaultSettings ++
    infoSettings ++
    sonatypeSettings ++
    seq(lsSettings: _*) ++
    Seq(
      javacOptions ++= Seq("-Xlint:unchecked"),
      scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
      //scalacOptions in Test ++= Seq("-Xprint:typer"),
      //fork in Test := true,
      fork := true,
      parallelExecution in Test := false,
      libraryDependencies += "junit" % "junit" % "4.10" % "test",
      libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test")

  lazy val reflectSettings =
    standardSettings ++
    scalaSettings ++
    Seq(
      //scalacOptions ++= Seq("-language:experimental.macros"),
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _),
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _))

  lazy val sonatypeSettings = Seq(
    publishMavenStyle := true,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("-SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    })

  override lazy val settings = 
    super.settings ++
    Seq(
      shellPrompt := { s => Project.extract(s).currentProject.id + "> " }
    ) ++ scalaSettings

  lazy val shadeSettings =
    assemblySettings ++ 
    addArtifact(artifact in (Compile, assembly), assembly) ++
    Seq(
      publishArtifact in (Compile, packageBin) := false,
      excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
        // Exclude scala-library and al.
        cp filter { j => {
          val n = j.data.getName
          n.startsWith("scala-") || n.equals("jfxrt.jar")
        } }
      },
      pomPostProcess := { (node: scala.xml.Node) =>
        // Since we publish the assembly (shaded) jar, 
        // remove lagging scalaxy dependencies from pom ourselves.
        import scala.xml._; import scala.xml.transform._
        try {
          new RuleTransformer(new RewriteRule {
            override def transform(n: Node): Seq[Node] ={
              if ((n \ "artifactId" find { _.text.startsWith("scalaxy-") }) != None)
                Array[Node]()
              else
                n
            }
          })(node)
        } catch { case _: Throwable =>
          node
        }
      }
    )
  
  val deploySettings = 
    reflectSettings ++
    shadeSettings ++
    Seq(
      artifact in (Compile, assembly) ~= { _.copy(`classifier` = Some("assembly")) },
      publishArtifact in Test := true)

  lazy val _scalaxy =
    Project(
      id = "scalaxy",
      base = file("."),
      settings =
        standardSettings ++
        shadeSettings ++
        Seq(
          test in assembly := {},
          artifact in (Compile, assembly) ~= { _.copy(`classifier` = None) },
          scalacOptions in console in Compile <+= (packageBin in Compile) map("-Xplugin:" + _)
        )).
    dependsOn(plugin, compilets, api, beans).
    aggregate(plugin, compilets, api, beans)
    
  lazy val plugin =
    Project(id = "scalaxy-plugin", base = file("Plugin"), settings = deploySettings).
    dependsOn(api)

  lazy val compilets =
    Project(id = "scalaxy-compilets", base = file("Compilets"), settings = reflectSettings).
    dependsOn(api, plugin % "test->test")

  lazy val beans =
    Project(id = "scalaxy-beans", base = file("Beans"), settings = deploySettings)

  lazy val fxSettings = reflectSettings ++ Seq(
    unmanagedJars in Compile ++= Seq(
      new File(System.getProperty("java.home")) / "lib" / "jfxrt.jar"
    )
  )
  
  lazy val fxBase =
    Project(id = "scalaxy-fx-base", base = file("Fx"), settings = fxSettings ++ Seq(publish := { })).
    dependsOn(fx, fxRuntime).
    aggregate(fx, fxRuntime)

  lazy val fx =
    Project(id = "scalaxy-fx", base = file("Fx/Macros"), settings = fxSettings).
    dependsOn(fxRuntime)

  lazy val fxRuntime =
    Project(id = "scalaxy-fx-runtime", base = file("Fx/Runtime"), settings = fxSettings)

  lazy val api =
    Project(id = "scalaxy-api", base = file("API"), settings = reflectSettings)
    
  //lazy val scalaxySbtPlugin =
  //  Project(id = "sbt-scalaxy", base = file("Sbt"), settings = standardSettings ++ Seq(
  //    scalaVersion := "2.9.2",
  //    //crossSbtVersions := Seq("0.11.3", "0.11.2" ,"0.12" ,"0.12.1"),
  //    sbtPlugin := true
  //    //CrossBuilding.scriptedSettings, // ?
  //  ))
}
