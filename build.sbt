lazy val root = project
  .in(file("."))
  .settings(
    name := "nbtscript",
    scalaVersion := "3.0.0-M3",

    libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.11"
  )
