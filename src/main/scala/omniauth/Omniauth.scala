package omniauth

/*
 * Copyright 2010-2011 Matthew Henderson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import omniauth.lib._
import dispatch._
import oauth.{Token, Consumer}
import json._
import JsHttp._
import oauth._
import oauth.OAuth._
import xml.{Text, NodeSeq}
import net.liftweb.json.JsonParser
import net.liftweb.json.JsonAST._
import net.liftweb.http._
import net.liftweb.sitemap.{Menu, Loc, SiteMap}
import Loc._
import net.liftweb.util.Props
import net.liftweb.common._
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair




object Omniauth  {
  val logger = Logger("omniauth.Omniauth")
  val http = new Http
  var TwitterHost = :/("api.twitter.com").secure
  val twitterOauthRequest = TwitterHost / "oauth"

  var successRedirect = "/"
  var failureRedirect = "/"
  var providers:List[OmniauthProvider] = List[OmniauthProvider]()
  var siteAuthBaseUrl = "http://0.0.0.0:8080/"

  private object curRequestToken extends SessionVar[Box[Token]](Empty)
  def currentRequestToken: Box[Token] = curRequestToken.is
  def setRequestToken(tok:Token){
    curRequestToken(Full(tok))
  }

  private object curAccessToken extends SessionVar[Box[Token]](Empty)
  
  def currentAccessToken: Box[Token] = curAccessToken.is
  
  def setAccessToken(tok:Token){ curAccessToken(Full(tok)) }

  private object authInfo extends SessionVar[Box[AuthInfo]](Empty)
  
  def currentAuth: Box[AuthInfo] = authInfo.is
  
  def setAuthInfo(ai:AuthInfo){authInfo(Full(ai))}

  private def providerListFromProperties():List[OmniauthProvider] = {
    List(getProviderFromProperties(FacebookProvider.providerName, FacebookProvider.providerPropertyKey, FacebookProvider.providerPropertySecret),
    getProviderFromProperties(GithubProvider.providerName, GithubProvider.providerPropertyKey, GithubProvider.providerPropertySecret),
    getProviderFromProperties(TwitterProvider.providerName, TwitterProvider.providerPropertyKey, TwitterProvider.providerPropertySecret)).flatten(a => a)
  }

  private def getProviderFromProperties(providerName:String, providerKey:String, providerSecret:String):Box[OmniauthProvider] = {
     Props.get(providerKey) match {
      case Full(pk) => Props.get(providerSecret) match {
        case Full(ps) => {
          providerName match {
            case TwitterProvider.providerName => Full(new TwitterProvider(pk, ps))
            case FacebookProvider.providerName => Full(new FacebookProvider(pk, ps))
            case GithubProvider.providerName => Full(new GithubProvider(pk, ps))
            case _ => {
              logger.warn("no provider found for "+providerName)
              Empty
            }
          }
        }
        case Empty => logger.warn("getProviderFromProperties: empty secret"); Empty
        case Failure(_,_,_) => logger.warn("getProviderFromProperties: fail secret"); Empty
      }
      case Empty => logger.warn("getProviderFromProperties: empty key"); Empty
      case Failure(_,_,_) => logger.warn("getProviderFromProperties: fail key"); Empty
    }
  }

  private def commonInit = {
    
    ResourceServer.allow({
    	case "img" ::  img  => true
    })       
    
    siteAuthBaseUrl = Props.get("omniauth.baseurl") openOr "http://0.0.0.0:8080/"
    successRedirect = Props.get("omniauth.successurl") openOr "/"
    failureRedirect = Props.get("omniauth.failureurl") openOr "/"

    LiftRules.addToPackages("omniauth")

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

  def init = {
    providers = providerListFromProperties()
    commonInit
  }

  def initWithProviders(newProviders:List[OmniauthProvider]) = {
    providers = newProviders
    commonInit
  }

  def callbackMenuLoc: Box[Menu] =
    Full(Menu(Loc("AuthCallback", List("omniauth","callback"), "AuthCallback", Hidden)))

  def signinMenuLoc: Box[Menu] =
    Full(Menu(Loc("AuthSignin", List("omniauth", "signin"), "AuthSignin", Hidden)))

  lazy val sitemap: List[Menu] =
    List(callbackMenuLoc, signinMenuLoc).flatten(a => a)

  def validateToken(provider:String, token:String): Boolean = {
    Omniauth.providers.foreach(p => {
      if(p.providerName.equalsIgnoreCase(provider)){
        return p.validateToken(token)
      }
    })
    false
  }

  def tokenToId(provider:String, token:String): Box[String] = {
    Omniauth.providers.foreach(p => {
      if(p.providerName.equalsIgnoreCase(provider)){
        return p.tokenToId(token)
      }
    })
    Empty
  }

  def map2ee(values: Map[String, Any]) = java.util.Arrays asList (
    values.toSeq map { case (k, v) => new BasicNameValuePair(k, v.toString) } toArray : _*
  )
  def q_str (values: Map[String, Any]) = URLEncodedUtils.format(map2ee(values), Request.factoryCharset)
}
case class AuthInfo(provider:String,uid:String,name:String,token:String,secret:Option[String] = None,nickName:Option[String] = None)
