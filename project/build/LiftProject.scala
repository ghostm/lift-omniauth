import sbt._

class LiftProject(info: ProjectInfo) extends DefaultWebProject(info) {
  val mavenLocal = "Local Maven Repository" at
  "file://"+Path.userHome+"/.m2/repository"

  val scalatools_release = "Scala Tools Snapshot" at
  "http://scala-tools.org/repo-releases/"

  val liftVersion = "2.0-RC2"

  override def libraryDependencies = Set(
    "net.liftweb" % "lift-webkit" % liftVersion % "compile->default",
    "net.liftweb" % "lift-mapper" % liftVersion % "compile->default",
    "net.liftweb" % "lift-testkit" % liftVersion % "compile->default",
    "net.liftweb" % "lift-wizard" % liftVersion % "compile->default",
    "org.mortbay.jetty" % "jetty" % "6.1.22" % "test->default",
    "junit" % "junit" % "4.5" % "test->default",
    "org.scala-tools.testing" % "specs" % "1.6.2.1" % "test->default",
    "com.h2database" % "h2" % "1.2.121"
  ) ++ super.libraryDependencies
}
