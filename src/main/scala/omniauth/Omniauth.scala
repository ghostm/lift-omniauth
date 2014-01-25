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
import dispatch.classic._
import oauth._
import net.liftweb.http._
import net.liftweb.sitemap.{Menu, Loc}
import Loc._
import net.liftweb.util.Props
import net.liftweb.common._
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair

object Omniauth extends Omniauth

trait Omniauth  {
  val logger = Logger("omniauth.Omniauth")

 /**
  * If in GAE, add dependency to net.databinder:dispatch-gae and override this.
  *  {{{
  *      if (Props.inGAE) { new dispatch.gae.Http } else { new Http } 
  *  }}}
  */
  val http =  new Http
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

  def clearCurrentAuth = authInfo(Empty)

  private def providerListFromProperties():List[OmniauthProvider] = {
    List(getProviderFromProperties(FacebookProvider.providerName, FacebookProvider.providerPropertyKey, FacebookProvider.providerPropertySecret),
    getProviderFromProperties(GithubProvider.providerName, GithubProvider.providerPropertyKey, GithubProvider.providerPropertySecret),
    getProviderFromProperties(TwitterProvider.providerName, TwitterProvider.providerPropertyKey, TwitterProvider.providerPropertySecret),
    getProviderFromProperties(MSLiveProvider.providerName, MSLiveProvider.providerPropertyKey, MSLiveProvider.providerPropertySecret),
    getProviderFromProperties(DropboxProvider.providerName, DropboxProvider.providerPropertyKey, DropboxProvider.providerPropertySecret),
    getProviderFromProperties(GoogleProvider.providerName, GoogleProvider.providerPropertyKey, GoogleProvider.providerPropertySecret),
    getProviderFromProperties(LinkedinProvider.providerName, LinkedinProvider.providerPropertyKey, LinkedinProvider.providerPropertySecret),
    getProviderFromProperties(InstagramProvider.providerName, InstagramProvider.providerPropertyKey, InstagramProvider.providerPropertySecret)
    ).flatten(a => a)
  }

  private def getProviderFromProperties(providerName:String, providerKey:String, providerSecret:String):Box[OmniauthProvider] = {
     Props.get(providerKey) match {
      case Full(pk) => Props.get(providerSecret) match {
        case Full(ps) => {
          providerName match {
            case TwitterProvider.providerName => Full(new TwitterProvider(pk, ps))
            case FacebookProvider.providerName => Full(new FacebookProvider(pk, ps))
            case GithubProvider.providerName => Full(new GithubProvider(pk, ps))
            case MSLiveProvider.providerName => Full(new MSLiveProvider(pk, ps))
            case DropboxProvider.providerName => Full(new DropboxProvider(pk, ps))
            case GoogleProvider.providerName => Full(new GoogleProvider(pk, ps))
            case LinkedinProvider.providerName => Full(new LinkedinProvider(pk, ps))
            case InstagramProvider.providerName => Full(new InstagramProvider(pk, ps))
            case _ => {
              logger.warn("no provider found for "+providerName)
              Empty
            }
          }
        }
        case Empty => logger.warn("getProviderFromProperties: empty secret"); Empty
        case Failure(_,_,_) => logger.warn("getProviderFromProperties: fail secret"); Empty
      }
      case Empty => logger.info("getProviderFromProperties:" + providerKey + " empty key"); Empty
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
    if(providers.size > 0)
      logger.info("Configured "+providers.size+" providers: "+providers.map(_.providerName))
    else
      logger.warn("No providers were configured for Omniauth!")
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

  def validateToken(provider:String, token:AuthToken): Boolean = {
    Omniauth.providers.foreach(p => {
      if(p.providerName.equalsIgnoreCase(provider)){
        return p.validateToken(token)
      }
    })
    false
  }

  def tokenToId(provider:String, token:AuthToken): Box[String] = {
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

case class AuthInfo(provider:String,
                    uid:String,
                    name:String,
                    token:AuthToken,
                    secret:Option[String] = None,
                    nickName:Option[String] = None,
                    email:Option[String] = None,
                    firstName:Option[String]=None,
                    lastName:Option[String]=None)
