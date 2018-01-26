import com.typesafe.config.ConfigFactory

name := """play-scala-seed"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.+" % Test

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick-codegen" % "3.2.+",
  "com.typesafe.play" %% "play-slick" % "3.0.+",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.+",
  "mysql" % "mysql-connector-java" % "5.1.+"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"

// Slick code generator
sourceGenerators in Compile += slickCodeGen.taskValue // Automatic code generation on build

lazy val appConfig = ConfigFactory.parseFile(new File("./conf/application.conf"))
lazy val slickCodeGen = taskKey[Seq[File]]("Generate Tables.scala")

slickCodeGen := {
  val dir = (sourceManaged in Compile) value
  val outputDir = dir / "slick"
//  val outputDir = "app/"
  val cp = (dependencyClasspath in Compile) value
  val s = streams value

  val slickDriver = appConfig.getString("slick.dbs.default.driver").init
  val jdbcDriver = appConfig.getString("slick.dbs.default.db.driver")
  val url = appConfig.getString("slick.dbs.default.db.url")
  val user = appConfig.getString("slick.dbs.default.db.user")
  val password = appConfig.getString("slick.dbs.default.db.password")
  val pkg = "models"

  runner.value.run("slick.codegen.SourceCodeGenerator",
    cp.files,
    Array(slickDriver, jdbcDriver, url, outputDir.getPath, pkg, user, password),
    s.log).failed foreach (sys error _.getMessage)

  val file = outputDir / pkg / "Tables.scala"

  Seq(file)
}
