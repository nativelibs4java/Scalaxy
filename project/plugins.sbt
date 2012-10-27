//resolvers += Resolver.url(
//  "sbt-plugin-releases", 
//  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/")
//)(Resolver.ivyStylePatterns)

//addSbtPlugin("com.github.retronym" % "sbt-onejar" % "0.7")

//addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.6")

resolvers += Resolver.url("scalasbt", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")

//addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.4")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0")

