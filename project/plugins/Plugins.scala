import sbt._

 class MySbtProjectPlugins(info: ProjectInfo) extends PluginDefinition(info) {

	lazy val eclipse = "de.element34" % "sbt-eclipsify" % "0.7.0"
    
     
 }


		