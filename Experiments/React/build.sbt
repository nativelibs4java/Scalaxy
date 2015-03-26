//import com.lihaoyi.workbench.Plugin._

enablePlugins(ScalaJSPlugin)

name := "Scala.js Tutorial"

scalaVersion := "2.11.6" // or any other Scala version >= 2.10.2

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.0"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.3"

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)

// libraryDependencies += "org.scalajs" %%% "scalajs-pickling" % "0.3"

// resolvers += Resolver.url("scala-js-releases",
//     url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(
//     Resolver.ivyStylePatterns)

// bootSnippet := "example.ScalaJSExample().main(document.getElementById('canvas'));"

//updateBrowsers <<= updateBrowsers.triggeredBy(fastOptJS in Compile)

//jsDependencies += "org.webjars" % "react" % "0.12.2" / "react-with-addons.js"
// 
jsDependencies += "org.webjars" % "react" % "0.12.2" / "react.js"

// commonJSName "React"

// jsDependencies += "org.webjars" % "react-async" % "1.0.2"

skip in packageJSDependencies := false

//jsDependencies += ProvidedJS / "myJSLibrary.js"
