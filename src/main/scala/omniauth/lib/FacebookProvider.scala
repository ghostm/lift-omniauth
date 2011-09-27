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

package omniauth.lib
import omniauth.Omniauth
import dispatch._
import oauth.{Token, Consumer}
import json._
import JsHttp._
import oauth._
import oauth.OAuth._
import xml.{Text, NodeSeq}
import net.liftweb.common.{Full, Empty, Box}
import net.liftweb.json.JsonParser
import net.liftweb.json.JsonAST._
import net.liftweb.http._
import net.liftweb.util.Props
import net.liftweb.sitemap.{Menu, Loc, SiteMap}
import Loc._
import omniauth.AuthInfo


class FacebookProvider(val clientId:String, val secret:String) extends OmniauthProvider{
  def providerName = FacebookProvider.providerName
  def providerPropertyKey = FacebookProvider.providerPropertyKey
  def providerPropertySecret = FacebookProvider.providerPropertySecret

  def facebookPermissions = 
    Props.get("omniauth.facebookpermissions") openOr ""

  def signIn():NodeSeq = doFacebookSignin
  def callback(): NodeSeq = doFacebookCallback
  implicit val formats = net.liftweb.json.DefaultFormats

  def doFacebookSignin() : NodeSeq = {
    var requestUrl = "https://graph.facebook.com/oauth/authorize?"
    val callbackUrl = Omniauth.siteAuthBaseUrl+"auth/"+providerName+"/callback"
    var urlParameters = Map[String, String]()
    urlParameters += ("client_id" -> clientId)
    urlParameters += ("redirect_uri" -> callbackUrl)
    urlParameters += ("scope" -> facebookPermissions)
    requestUrl += Omniauth.q_str(urlParameters)
    S.redirectTo(requestUrl)
  }

  def doFacebookCallback () : NodeSeq = {
    val fbCode = S.param("code") openOr S.redirectTo("/")
    val callbackUrl = Omniauth.siteAuthBaseUrl+"auth/"+providerName+"/callback"
    var urlParameters = Map[String, String]()
    urlParameters += ("client_id" -> clientId)
    urlParameters += ("redirect_uri" -> callbackUrl)
    urlParameters += ("client_secret" -> secret)
    urlParameters += ("code" -> fbCode.toString)
    val tempRequest = :/("graph.facebook.com").secure / "oauth/access_token" <<? urlParameters
    var accessTokenString = Omniauth.http(tempRequest as_str)
    if(accessTokenString.startsWith("access_token=")){
      accessTokenString = accessTokenString.stripPrefix("access_token=")
      val ampIndex = accessTokenString.indexOf("&")
      if(ampIndex >= 0){
        accessTokenString = accessTokenString.take(ampIndex)
      }
      if(validateToken(accessTokenString)){
        S.redirectTo(Omniauth.successRedirect)
      }else{
        S.redirectTo(Omniauth.failureRedirect)
      }
    }else{
      println("didn't find access token")
      S.redirectTo(Omniauth.failureRedirect)
    }
  }

  def validateToken(accessToken:String): Boolean = {
    val tempRequest = :/("graph.facebook.com").secure / "me" <<? Map("access_token" -> accessToken)
    try{
      val json = Omniauth.http(tempRequest >- JsonParser.parse)

      val uid =  (json \ "id").extract[String]
      val name =  (json \ "name").extract[String]
     
      val ai = AuthInfo(providerName,uid,name,accessToken,Some(secret))
      Omniauth.setAuthMap(ai)
      logger.debug("Omniauth.setAuthMap(twitterAuthMap) "+ai)

      true
    } catch {
      case _ => false
    }
  }
}

object FacebookProvider {
  val providerName = "facebook"
  val providerPropertyKey = "omniauth.facebookkey"
  val providerPropertySecret = "omniauth.facebooksecret"
}

