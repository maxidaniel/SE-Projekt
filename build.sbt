import scoverage.ScoverageKeys.coverageExcludedPackages

val scala3Version = "3.8.3"

lazy val root = project
  .in(file("."))
  .settings(
    name := "president",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies += "org.jline" % "jline-terminal" % "4.3.1",
    libraryDependencies += "org.jline" % "jline-reader" % "4.3.1",
    libraryDependencies += "org.jline" % "jline-console-ui" % "4.3.1",
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.20",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.20" % "test",
    libraryDependencies += "org.scalafx" %% "scalafx" % "21.0.0-R32",
    libraryDependencies += "net.codingwell" %% "scala-guice" % "7.0.0",

    // IO
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
    libraryDependencies += "org.playframework" %% "play-json" % "3.0.6"
  )

coverageExcludedPackages := "<empty>;.*ui.*"
coverageExcludedFiles := "<empty>;.*President.*;.*ui.*"
