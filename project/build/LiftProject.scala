

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

  override def libraryDependencies = Set(
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
    "net.databinder" %% "dispatch-http" % "0.8.3",
    "net.databinder" %% "dispatch-twitter" % "0.8.3"
  ) ++ super.libraryDependencies
  
// To publish to the Cloudbees repos:

  val publishTo = "liftmodules repository" at "https://repository-liftmodules.forge.cloudbees.com/release/"
 
  Credentials( new java.io.File( "/private/liftmodules/cloudbees.credentials"),log)
  
}
