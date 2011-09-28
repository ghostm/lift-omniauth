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
import twitter.{Auth}
import xml.{Text, NodeSeq}
import net.liftweb.common.{Full, Empty, Box}
import net.liftweb.json.JsonParser
import net.liftweb.json.JsonAST._
import net.liftweb.http._
import net.liftweb.sitemap.{Menu, Loc, SiteMap}
import Loc._
import net.liftweb.common._


class TwitterProvider(val key:String, val secret:String) extends OmniauthProvider {
  def providerName = TwitterProvider.providerName
  def providerPropertyKey = TwitterProvider.providerPropertyKey
  def providerPropertySecret = TwitterProvider.providerPropertySecret

  def signIn(): NodeSeq = doTwitterSignin
  def callback(): NodeSeq = doTwitterCallback

  val consumer = Consumer(key, secret)

  val logger = Logger("omniauth.TwitterProvider")

  def twitterAuthenticateUrl(token: Token) = Omniauth.twitterOauthRequest / "authenticate" with_token token

  def doTwitterSignin () : NodeSeq = {
    logger.debug("doTwitterSignin")
    logger.debug(consumer)
    var callbackUrl = Omniauth.siteAuthBaseUrl+"auth/"+providerName+"/callback"
    logger.debug(callbackUrl)
    var requestToken = Omniauth.http(Auth.request_token(consumer, callbackUrl))
    val auth_uri = twitterAuthenticateUrl(requestToken).to_uri
    logger.debug(auth_uri.toString)
    Omniauth.setRequestToken(requestToken)
    S.redirectTo(auth_uri.toString)
  }

  def doTwitterCallback () : NodeSeq = {
    logger.debug("doTwitterCallback")
    var verifier = S.param("oauth_verifier") openOr S.redirectTo(Omniauth.failureRedirect)
    var requestToken = Omniauth.currentRequestToken openOr S.redirectTo(Omniauth.failureRedirect)
    Omniauth.http(Auth.access_token(consumer, requestToken, verifier)) match {
      case (access_tok, tempUid, screen_name) => {
        if(validateToken(""+access_tok.value+","+access_tok.secret)){
          S.redirectTo(Omniauth.successRedirect)
        }else{
          S.redirectTo(Omniauth.failureRedirect)
        }
      }
      case _ => S.redirectTo(Omniauth.failureRedirect)
    }
  }

  //Token must be of the format value,secret
  def validateToken(accessToken:String): Boolean = {
    val tokenParts = accessToken.split(",")
    if(tokenParts.length != 2){
      logger.debug("tokenParts.length != 2: "+accessToken)
      return false
    }
    val authToken = Token(tokenParts(0), tokenParts(1))
    logger.debug("authToken "+authToken)
    var verifyCreds = Omniauth.TwitterHost / "1/account/verify_credentials.xml" <@ (consumer, authToken)
    try{
      var tempResponse = Omniauth.http(verifyCreds <> { _ \\ "user" })
      var twitterAuthMap = Map[String, Any]()
      twitterAuthMap += (Omniauth.Provider -> providerName)
      twitterAuthMap += (Omniauth.UID -> (tempResponse \ "id").text)
      var twitterAuthUserInfoMap = Map[String, String]()
      twitterAuthUserInfoMap += (Omniauth.Name -> (tempResponse \ "name").text)
      twitterAuthUserInfoMap += (Omniauth.Nickname -> (tempResponse \ "screen_name").text)
      twitterAuthMap += (Omniauth.UserInfo -> twitterAuthUserInfoMap)
      var twitterAuthCredentialsMap = Map[String, String]()
      twitterAuthCredentialsMap += (Omniauth.Token -> authToken.value)
      twitterAuthCredentialsMap += (Omniauth.Secret -> authToken.secret)
      twitterAuthMap += (Omniauth.Credentials -> twitterAuthCredentialsMap)
      Omniauth.setAuthMap(twitterAuthMap)
      logger.debug("Omniauth.setAuthMap(twitterAuthMap) "+twitterAuthMap)
      true
    } catch {
      case e:Exception => logger.debug("Exception= "+e);false;
    }
  }

  def tokenToId(accessToken:String): Box[String] = {
    val tokenParts = accessToken.split(",")
    if(tokenParts.length != 2){
      logger.debug("tokenParts.length != 2: "+accessToken)
      return Empty
    }
    val authToken = Token(tokenParts(0), tokenParts(1))
    logger.debug("authToken "+authToken)
    var verifyCreds = Omniauth.TwitterHost / "1/account/verify_credentials.xml" <@ (consumer, authToken)
    try{
      var tempResponse = Omniauth.http(verifyCreds <> { _ \\ "user" })
      Full((tempResponse \ "id").text)
    }catch {
      case e:Exception => logger.debug("Exception= "+e);Empty;
    }
  }
}

object TwitterProvider{
  val providerName = "twitter"
  val providerPropertyKey = "omniauth.twitterkey"
  val providerPropertySecret = "omniauth.twittersecret"
}
