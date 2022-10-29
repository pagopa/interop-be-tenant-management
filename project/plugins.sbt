addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.4")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.11")

addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.1")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.3")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.12"

ThisBuild / libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
