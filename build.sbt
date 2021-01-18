lazy val root = project
  .in(file("."))
  .settings(
    name := "nbtscript",
    scalaVersion := "3.0.0-M3",

    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
    libraryDependencies := libraryDependencies.value.map(_.withDottyCompat(scalaVersion.value))
  )
