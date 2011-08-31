

import sbt._
import de.element34.sbteclipsify._

class LiftProject(info: ProjectInfo) extends DefaultProject(info) with Eclipsify {
  val liftVersion = "2.4-M3"

  // uncomment the following if you want to use the snapshot repo
  //val scalatoolsSnapshot = ScalaToolsSnapshots

  // If you're using JRebel for Lift development, uncomment
  // this line
  // override def scanDirectories = Nil

  val databinder_net = "databinder.net repository" at "http://databinder.net/repo"
  val dispatch = "net.databinder" %% "dispatch-http" % "0.8.3"
  val dispatchTwitter = "net.databinder" %% "dispatch-twitter" % "0.8.3"

  override def libraryDependencies = Set(
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
    
    "org.specs2" %% "specs2" % "1.5"  % "test->default",
    "com.h2database" % "h2" % "1.2.138",
    "ch.qos.logback" % "logback-classic" % "0.9.26"
  ) ++ super.libraryDependencies
}
