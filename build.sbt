import scoverage.ScoverageKeys.coverageExcludedPackages

val scala3Version = "3.8.3"

lazy val root = project
  .in(file("."))
  .settings(
    name := "president",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.jline" % "jline-terminal" % "4.1.3",
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.20",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.20" % "test",
    libraryDependencies += "org.scalafx" %% "scalafx" % "21.0.0-R32",
    libraryDependencies += "net.codingwell" %% "scala-guice" % "7.0.0",

    libraryDependencies ++= {
      // Determine OS version of JavaFX binaries
      lazy val osName = System.getProperty("os.name") match {
        case n if n.startsWith("Linux") => "linux"
        case n if n.startsWith("Mac") => "mac"
        case n if n.startsWith("Windows") => "win"
        case _ => throw new Exception("Unknown platform!")
      }
      Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
        .map(m => "org.openjfx" % s"javafx-$m" % "21" classifier osName)
    }
  )

coverageExcludedPackages := "<empty>;.*ui.*"
coverageExcludedFiles := "<empty>;.*President.*;.*ui.*"
