package omniauth.view
import omniauth.lib.OmniauthLib
import dispatch._
import oauth.{Token, Consumer}
import json._
import JsHttp._
import oauth._
import oauth.OAuth._
import twitter.{Status, Twitter, Auth}
import xml.{Text, NodeSeq}
import net.liftweb.common.{Full, Empty, Box}
import net.liftweb.json.JsonParser
import net.liftweb.json.JsonAST._
import net.liftweb.http._
import net.liftweb.sitemap.{Menu, Loc, SiteMap}
import Loc._


class Omniauth extends LiftView {

  override def dispatch = {
    case "signin" => doAuthSignin _
    case "callback" => doAuthCallback _
  }

  def doAuthSignin : NodeSeq = {
    println("doAuthSignin")
    var provider = S.param("provider") openOr S.redirectTo(OmniauthLib.failureRedirect)
    OmniauthLib.providers.foreach(p => {
      if(p.provider.equalsIgnoreCase(provider)){
        println("provider match")
        p.signIn
      }
    })
    S.redirectTo(OmniauthLib.failureRedirect)
  }

  def doAuthCallback () : NodeSeq = {
    var provider = S.param("provider") openOr S.redirectTo(OmniauthLib.failureRedirect)
    OmniauthLib.providers.foreach(p => {
      if(p.provider.equalsIgnoreCase(provider)){
        p.callback
      }
    })
    S.redirectTo(OmniauthLib.failureRedirect)
  }

}





