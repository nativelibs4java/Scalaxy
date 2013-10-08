import sbt._
import Keys._
import sbtassembly.Plugin._ ; import AssemblyKeys._
import ls.Plugin._

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.scalariformSettings
import com.typesafe.sbt.SbtScalariform._
import com.typesafe.sbt.SbtGit.GitKeys._
import com.typesafe.sbt.SbtSite._
import com.typesafe.sbt.SbtSite.SiteKeys._
import com.typesafe.sbt.SbtGhPages._

/*
Scalaxy/Reified:
  - Normal tests:
    sbt 'project scalaxy-reified' ~test

  - Remote-debug tests on port 5005:
    sbt 'project scalaxy-reified' 'set javaOptions in Test ++= Seq("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")' ~test

*/
object Scalaxy extends Build {
  // See https://github.com/mdr/scalariform
  ScalariformKeys.preferences := FormattingPreferences()
    .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
    .setPreference(PreserveDanglingCloseParenthesis, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(PreserveDanglingCloseParenthesis, false)

  lazy val scalaSettings = Seq(
    //exportJars := true, // use jars in classpath
    scalaVersion := "2.10.3",
    //scalaVersion := "2.11.0-M4",
    //scalaVersion := "2.11.0-SNAPSHOT",
    crossScalaVersions := Seq(
      "2.11.0-SNAPSHOT"))

  lazy val infoSettings = Seq(
    organization := "com.nativelibs4java",
    version := "0.3-SNAPSHOT",
    licenses := Seq("BSD-3-Clause" -> url("http://www.opensource.org/licenses/BSD-3-Clause")),
    homepage := Some(url("https://github.com/ochafik/Scalaxy")),
    gitRemoteRepo := "git@github.com:ochafik/Scalaxy.git",
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

  lazy val docSettings =
    Seq(
      scalacOptions in (Compile, doc) <++= (name, baseDirectory, description, version, sourceDirectory) map {
        case (name, base, description, version, sourceDirectory) =>
          Opts.doc.title(name + ": " + description) ++
          Opts.doc.version(version) ++
          //Seq("-doc-source-url", "https://github.com/ochafik/Scalaxy/blob/master/Reified/Base/src/main/scala") ++
          Seq("-doc-root-content", (sourceDirectory / "main" / "rootdoc.txt").getAbsolutePath)
      }
    )

  lazy val standardSettings =
    Defaults.defaultSettings ++
    infoSettings ++
    sonatypeSettings ++
    docSettings ++
    seq(lsSettings: _*) ++
    Seq(
      javacOptions ++= Seq("-Xlint:unchecked"),
      scalacOptions ++= Seq(
        "-encoding", "UTF-8",
        "-optimise",
        "-deprecation",
        // "-Yinfer-debug",
        //"-Xlog-implicits",
        //"-Ymacro-debug-lite", "-Ydebug",
        // "-Ymacro-debug-verbose",
        "-feature",
        "-unchecked"
      ),
      //scalacOptions in Test ++= Seq("-Xprint:typer"),
      //fork in Test := true,
      fork := true,
      parallelExecution in Test := false,
      libraryDependencies ++= Seq(
        "junit" % "junit" % "4.10" % "test",
        "com.novocode" % "junit-interface" % "0.8" % "test"
      )
    )

  lazy val reflectSettings =
    standardSettings ++
    scalaSettings ++
    Seq(
      //scalacOptions ++= Seq("-language:experimental.macros"),
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _),
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)
      // libraryDependencies <+= scalaVersion("org.scala-lang.macro-paradise" % "scala-reflect" % _)
      // libraryDependencies += "org.scala-lang.macro-paradise" % "scala-reflect" % "2.10.3-SNAPSHOT"
    )

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

  lazy val _scalaxy =
    Project(
      id = "scalaxy",
      base = file("."),
      settings =
        standardSettings ++
        Seq(publish := { }))
    .aggregate(integration, loops, compilets, fx, json, beans, components, debug, extensions, reified, scalaxyDoc, parano)

  lazy val integration =
    Project(
      id = "scalaxy-integration",
      base = file("Integration"),
      settings =
        standardSettings ++
        Seq(publish := { }))
    .dependsOn(loops, compilets, fx, json, beans, components, debug, extensions, reified, parano)

  lazy val docProjects = Map(
    "Compilets" -> compilets,
    "Fx" -> fx,
    "Beans" -> beans,
    "JS" -> js,
    "JSON" -> json,
    // "Parano" -> parano,
    "Loops" -> loops,
    //"Components" -> components,
    "Debug" -> debug,
    //"MacroExtensions" -> extensions,
    "Reified" -> reifiedDoc)

  lazy val scalaxyDoc =
    Project(
      id = "scalaxy-doc",
      base = file("Doc"),
      settings =
        reflectSettings ++
        site.settings ++
        site.publishSite ++
        ghpages.settings ++
        //site.jekyllSupport() ++
        Seq(
          //aggregate in ghpages-push-site := true,
          //com.typesafe.sbt.site.JekyllSupport.RequiredGems := Map(
          //  "jekyll" -> "1.0.3",
          //  "liquid" -> "2.5.0"),
          siteMappings ++= {
            val rd = "README.md"
            val target = "index.md"
            (
              docProjects.keys.map(dirName =>
                file(dirName + "/" + rd) -> (dirName + "/" + target)) ++
              Set(file(rd) -> target)
            ).filter(_._1.exists).toSeq
          },
          scalacOptions in (Compile, doc) <++= (name, baseDirectory, description, version, sourceDirectory) map {
            case (name, base, description, version, sourceDirectory) =>
              Opts.doc.title(name + ": " + description) ++
              Opts.doc.version(version) ++
              //Seq("-doc-source-url", "https://github.com/ochafik/Scalaxy/blob/master/Reified/Base/src/main/scala") ++
              Seq("-doc-root-content", (sourceDirectory / "main" / "rootdoc.txt").getAbsolutePath)
          }
        ) ++
        //site.includeScaladoc() ++//"alternative/directory") ++
        docProjects.flatMap({ case (dirName, project) =>
          Seq(
            siteMappings <++= (mappings in packageDoc in project in Compile, name in project, version in project, baseDirectory in project).map({
              case (mappings, id, version, base) =>
                for((f, d) <- mappings) yield {
                  val versionString = if (version.endsWith("-SNAPSHOT")) "latest" else version
                  (f, dirName + "/" + versionString + "/api/" + d)
                }
            })
          )
        })
    ).dependsOn(docProjects.values.map(p => p: ClasspathDep[ProjectReference]).toSeq: _*)
    .aggregate(docProjects.values.toSeq.map(p => p: sbt.ProjectReference): _*)

  lazy val compilets =
    Project(
      id = "scalaxy-compilets",
      base = file("Compilets"),
      settings =
        standardSettings ++
        shadeSettings ++
        Seq(
          // Assembly artifact is just here to accommodate console mode, it's not to be published.
          publish := { },
          test in assembly := {},
          artifact in (Compile, assembly) ~= { _.copy(`classifier` = None) },
          scalacOptions in console in Compile <+= (packageBin in Compile) map("-Xplugin:" + _)
        )
    )
    .dependsOn(compiletsPlugin, compiletsApi, defaultCompilets)
    .aggregate(compiletsPlugin, compiletsApi, defaultCompilets)

  lazy val compiletsApi =
    Project(
      id = "scalaxy-compilets-api",
      base = file("Compilets/API"),
      settings = reflectSettings ++ Seq(
        scalacOptions ++= Seq(
          "-language:experimental.macros"
        )
      ))

  lazy val components =
    Project(id = "scalaxy-components", base = file("Components"), settings = reflectSettings ++ scalariformSettings)

  lazy val compiletsPlugin =
    Project(
      id = "scalaxy-compilets-plugin",
      base = file("Compilets/Plugin"),
      settings =
        reflectSettings ++
        shadeSettings ++
        Seq(
          artifact in (Compile, assembly) ~= {
            _.copy(`classifier` = None)//Some("assembly"))
          },
          publishArtifact in Test := true))
    .dependsOn(compiletsApi)

  lazy val defaultCompilets =
    Project(
      id = "scalaxy-default-compilets",
      base = file("Compilets/DefaultCompilets"),
      settings = reflectSettings)
    .dependsOn(compiletsApi, compiletsPlugin % "test->test")

  lazy val extensions =
    Project(id = "scalaxy-macro-extensions", base = file("MacroExtensions"), settings = reflectSettings ++ Seq(
      watchSources <+= baseDirectory map { _ / "examples" }
    ))
    .dependsOn(debug)

  lazy val js =
    Project(id = "scalaxy-js", base = file("Experiments/JS"), settings = reflectSettings ++ Seq(
      // addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise_2.10.3-RC1" % "2.0.0-SNAPSHOT"),
      // addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise_2.10.2" % "2.0.0-SNAPSHOT"),
      libraryDependencies += "com.google.javascript" % "closure-compiler" % "v20130722"
      // scalaVersion := "2.10.3-RC1"

    ))

  lazy val json =
    Project(id = "scalaxy-json", base = file("JSON"), settings = reflectSettings ++ Seq(
      // libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala" % "2.2.2"
      // libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.3"
      libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.5",
      libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.5"
    ))

  lazy val beans =
    Project(id = "scalaxy-beans", base = file("Beans"), settings = reflectSettings)

  lazy val loops =
    Project(id = "scalaxy-loops", base = file("Loops"), settings = reflectSettings)

  lazy val debug =
    Project(id = "scalaxy-debug", base = file("Debug"), settings = reflectSettings)

  lazy val enum =
    Project(id = "scalaxy-enum", base = file("Enum"), settings = reflectSettings)

  lazy val union =
    Project(id = "scalaxy-union", base = file("Union"), settings = reflectSettings ++ scalariformSettings)
    .dependsOn(debug)

  lazy val parano =
    Project(id = "scalaxy-parano", base = file("Parano"), settings = reflectSettings ++ scalariformSettings ++ Seq(
      watchSources <+= baseDirectory map { _ / "examples" }))

  lazy val generic =
    Project(id = "scalaxy-generic", base = file("Generic"), settings = reflectSettings ++ scalariformSettings)
    .dependsOn(debug)

  lazy val reifiedBase =
    Project(id = "scalaxy-reified-base", base = file("Reified/Base"), settings = reflectSettings ++ scalariformSettings)
    .dependsOn(debug, union, generic)

  lazy val reified =
    Project(id = "scalaxy-reified", base = file("Reified"), settings = reflectSettings ++ scalariformSettings ++ Seq(
      fork in Test := true,
      //javaOptions in Test ++= Seq("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
      scalacOptions in Test ++= Seq(
        "-optimise",
        "-Yclosure-elim",
        "-Yinline"
      )
    ))
    .dependsOn(reifiedBase)
    .aggregate(reifiedBase, union, generic)

  lazy val reifiedDoc =
    Project(id = "scalaxy-reified-doc", base = file("Reified/Doc"), settings = reflectSettings ++
      Seq(
        publish := { },
        (skip in compile) := true,
        //site.siteMappings <++= Seq(file1 -> "location.html", file2 -> "image.png"),
        unmanagedSourceDirectories in Compile <<= (
          (Seq(reified, reifiedBase) map (unmanagedSourceDirectories in _ in Compile)).join.apply {
            (s) => s.flatten.toSeq
          }
        )
      )
  ).dependsOn(reified, reifiedBase)

  lazy val fxSettings = reflectSettings ++ Seq(
    unmanagedJars in Compile ++= Seq(
      new File(System.getProperty("java.home")) / "lib" / "jfxrt.jar"
    )
  )

  lazy val fx =
    Project(
      id = "scalaxy-fx",
      base = file("Fx"),
      settings = fxSettings)
}
