import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "immovables_notifier"
  val appVersion      = "1.0-SNAPSHOT"
  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    "postgresql" % "postgresql" % "9.1-901.jdbc4",
    "com.typesafe.play" %% "play-slick" % "0.4.0",
    "org.webjars" % "webjars-play" % "2.1.0-1",
    "org.webjars" % "bootstrap" % "3.0.3",
    "org.webjars" % "jquery" % "1.9.1",
    "org.jsoup" % "jsoup" % "1.7.3",
    "com.ning" % "async-http-client" % "1.8.2"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    Keys.fork in (Test) := false,
    Keys.javaOptions in (Test) += "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9998"
  )

}