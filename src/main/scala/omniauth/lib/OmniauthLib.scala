package omniauth.lib

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

object OmniauthLib  {
  val http = new Http
  val TwitterProviderName = "twitter"
  val FacebookProviderName = "facebook"
  val Provider = "Provider"
  val UID = "UID"
  val UserInfo = "UserInfo"
  val Name = "Name"
  val Nickname = "Nickname"
  val Credentials = "Credentials"
  val Token = "Token"
  val Secret = "Secret"
  var TwitterHost = :/("api.twitter.com").secure
  val twitterOauthRequest = TwitterHost / "oauth"

  var successRedirect = "/"
  var failureRedirect = "/"
  var providers:List[OmniauthProvider] = List[OmniauthProvider]()
  var siteAuthBaseUrl = "http://localhost:8080/"

  private object curRequestToken extends SessionVar[Box[Token]](Empty)
  def currentRequestToken: Box[Token] = curRequestToken.is
  def setRequestToken(tok:Token){
    curRequestToken(Full(tok))
  }

  private object curAccessToken extends SessionVar[Box[Token]](Empty)
  def currentAccessToken: Box[Token] = curAccessToken.is
  def setAccessToken(tok:Token){
    curAccessToken(Full(tok))
  }

  private object curAuthMap extends SessionVar[Box[Map[String, Any]]](Empty)
  def currentAuthMap: Box[Map[String, Any]] = curAuthMap.is
  def setAuthMap(m:Map[String, Any]){
    curAuthMap(Full(m))
  }

  def init(providerList:List[OmniauthProvider], baseUrl:String = "http://localhost:8080/", successUrl:String = "/", failureUrl:String = "/") = {
    providers = providerList
    siteAuthBaseUrl = baseUrl
    successRedirect = successUrl
    failureRedirect = failureUrl
    LiftRules.addToPackages("omniauth")
    var tempSiteMap = LiftRules.siteMap openOr SiteMap()
    var siteMapKids:Seq[Menu] = tempSiteMap.kids
    siteMapKids = siteMapKids :+ Menu(Loc("AuthCallback", List("omniauth","callback"), "AuthCallback", Hidden))
    siteMapKids = siteMapKids :+ Menu(Loc("AuthSignin", List("omniauth", "signin"), "AuthSignin", Hidden))
    LiftRules.setSiteMap(SiteMap(siteMapKids:_*))

    //Omniauth request rewrites
    LiftRules.statelessRewrite.prepend {
      case RewriteRequest(ParsePath(List("auth", providerName, "signin"), _, _, _), _, _) =>
        RewriteResponse("omniauth"::"signin" :: Nil, Map("provider" -> providerName))
    }
    LiftRules.statelessRewrite.prepend {
      case RewriteRequest(ParsePath(List("auth", providerName, "callback"), _, _, _), _, _) =>
        RewriteResponse("omniauth"::"callback":: Nil, Map("provider" -> providerName))
    }
  }
}