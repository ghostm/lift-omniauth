name := "Omniauth"

version := "2.4-0.7"

organization := "net.liftmodules"

scalaVersion := "2.10.0" 
 
crossScalaVersions := Seq("2.10.0", "2.9.2", "2.9.1-1", "2.9.1")

resolvers += "databinder.net repository" at "http://databinder.net/repo"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= {
  val liftVersion = "2.5-SNAPSHOT" 
  Seq(
     "net.liftweb"   %% "lift-webkit"  % liftVersion  % "compile->default",
	"net.databinder" %% "dispatch-core" % "0.8.9",    
    "net.databinder" %% "dispatch-http" % "0.8.9",
    "net.databinder" %% "dispatch-oauth" % "0.8.9",    
    "net.databinder" %% "dispatch-gae" % "0.8.9",    
    "net.databinder" %% "dispatch-http-json" % "0.8.9"
    )
}

//scalacOptions ++= Seq("-feature")

 // To publish to the Cloudbees repos:

publishTo := Some("liftmodules repository" at "https://repository-liftmodules.forge.cloudbees.com/release/")
 
credentials += Credentials( file("/private/liftmodules/cloudbees.credentials") ) 


