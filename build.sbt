val scala3Version = "3.8.3"

lazy val root = project
  .in(file("."))
  .settings(
    name := "president",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.jline" % "jline-terminal" % "4.0.12",
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.20",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.20" % "test"

  )
