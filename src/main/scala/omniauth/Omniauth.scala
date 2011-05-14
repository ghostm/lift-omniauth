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



object Omniauth  {
  val logger = Logger("omniauth.Omniauth")
  val http = new Http
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

  def init = {
    providers = providerListFromProperties()
    siteAuthBaseUrl = Props.get("omniauth.baseurl") openOr "http://localhost:8080/"
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

  def callbackMenuLoc: Box[Menu] =
    Full(Menu(Loc("AuthCallback", List("omniauth","callback"), "AuthCallback", Hidden)))

  def signinMenuLoc: Box[Menu] =
    Full(Menu(Loc("AuthSignin", List("omniauth", "signin"), "AuthSignin", Hidden)))

  lazy val sitemap: List[Menu] =
    List(callbackMenuLoc, signinMenuLoc).flatten(a => a)
}