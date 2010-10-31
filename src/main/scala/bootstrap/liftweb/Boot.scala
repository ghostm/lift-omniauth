package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._
import mapper._

import code.model._


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor = 
	new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
			     Props.get("db.url") openOr 
			     "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
			     Props.get("db.user"), Props.get("db.password"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    // Use Lift's Mapper ORM to populate the database
    // you don't need to use Mapper to use Lift... use
    // any ORM you want
    Schemifier.schemify(true, Schemifier.infoF _, User)

    // where to search snippet
    LiftRules.addToPackages("code")

    // Build SiteMap
    val entries = List(
      Menu.i("Home") / "index", // the simple way to declare a menu

      //Omniauth site menu items
      Menu(Loc("AuthCallback", List("omniauth","callback"), "AuthCallback", Hidden)),
      Menu(Loc("AuthSignin", List("omniauth", "signin"), "AuthSignin", Hidden)),

      // more complex because this menu allows anything in the
      // /static path to be visible
      Menu(Loc("Static", Link(List("static"), true, "/static/index"), 
	       "Static Content"))) :::
    // the User management menu items
    User.sitemap

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMap(SiteMap(entries:_*))

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // What is the function to test if a user is logged in?
    LiftRules.loggedInTest = Full(() => User.loggedIn_?)

    //Omniauth request rewrites
    LiftRules.statelessRewrite.prepend {
      case RewriteRequest(ParsePath(List("auth", providerName, "signin"), _, _, _), _, _) =>
        RewriteResponse("omniauth"::"signin" :: Nil, Map("provider" -> providerName))
    }
    LiftRules.statelessRewrite.prepend {
      case RewriteRequest(ParsePath(List("auth", providerName, "callback"), _, _, _), _, _) =>
        RewriteResponse("omniauth"::"callback":: Nil, Map("provider" -> providerName))
    }


    // Make a transaction span the whole HTTP request
    S.addAround(DB.buildLoanWrapper)
  }
}
