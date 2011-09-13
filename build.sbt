name := "Omniauth"

version := "2.4-M3-0.2"

organization := "net.liftmodules"
 
scalaVersion := "2.9.0-1" //"2.9.1"
 
crossScalaVersions := Seq("2.9.0-1") //  , "2.9.1")

seq(com.github.siasia.WebPlugin.webSettings :_*)

// If using JRebel
jettyScanDirs := Nil

resolvers += "databinder.net repository" at "http://databinder.net/repo"

libraryDependencies ++= {
  val liftVersion = "2.4-M4" 
  Seq(
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
    "net.databinder" %% "dispatch-http" % "0.8.3",
    "net.databinder" %% "dispatch-twitter" % "0.8.3"
    )
}

 // To publish to the Cloudbees repos:

publishTo := Some("liftmodules repository" at "https://repository-liftmodules.forge.cloudbees.com/release/")
 
credentials += Credentials( file("cloudbees.credentials") )

