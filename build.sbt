name := "Omniauth"

version := "2.4-0.5"

organization := "net.liftmodules"

scalaVersion := "2.9.1" 
 
crossScalaVersions := Seq("2.8.1","2.9.0-1", "2.9.1")

seq(com.github.siasia.WebPlugin.webSettings :_*)

// If using JRebel
jettyScanDirs := Nil

resolvers += "databinder.net repository" at "http://databinder.net/repo"

libraryDependencies ++= {
  val liftVersion = "2.4" 
  Seq(
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
	  "net.databinder" %% "dispatch-core" % "0.8.5",    
    "net.databinder" %% "dispatch-http" % "0.8.5",
    "net.databinder" %% "dispatch-oauth" % "0.8.5",    
    "net.databinder" %% "dispatch-gae" % "0.8.5",    
    "net.databinder" %% "dispatch-http-json" % "0.8.5"
    )
}

 // To publish to the Cloudbees repos:

publishTo := Some("liftmodules repository" at "https://repository-liftmodules.forge.cloudbees.com/release/")
 
credentials += Credentials( file("/private/liftmodules/cloudbees.credentials") ) 


