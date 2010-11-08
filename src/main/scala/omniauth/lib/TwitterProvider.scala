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

class TwitterProvider(val key:String, val secret:String) extends OmniauthProvider {
  def provider(): String = OmniauthLib.TwitterProviderName
  def signIn(): NodeSeq = doTwitterSignin
  def callback(): NodeSeq = doTwitterCallback
  val consumer = Consumer(key, secret)

  def twitterAuthenticateUrl(token: Token) = OmniauthLib.twitterOauthRequest / "authenticate" <<? token

  def doTwitterSignin () : NodeSeq = {
    println("doTwitterSignin")
    var callbackUrl = OmniauthLib.siteAuthBaseUrl+"auth/twitter/callback"
    var requestToken = OmniauthLib.http(Auth.request_token(consumer, callbackUrl))
    val auth_uri = twitterAuthenticateUrl(requestToken).to_uri
    OmniauthLib.setRequestToken(requestToken)
    S.redirectTo(auth_uri.toString)
  }

  def doTwitterCallback () : NodeSeq = {
    var verifier = S.param("oauth_verifier") openOr S.redirectTo(OmniauthLib.failureRedirect)
    var requestToken = OmniauthLib.currentRequestToken openOr S.redirectTo(OmniauthLib.failureRedirect)
    OmniauthLib.http(Auth.access_token(consumer, requestToken, verifier)) match {
      case (access_tok, tempUid, screen_name) => {
        OmniauthLib.setAccessToken(access_tok)
      }
      case _ => S.redirectTo(OmniauthLib.failureRedirect)
    }
    var verifyCreds = OmniauthLib.TwitterHost / "1/account/verify_credentials.xml" <@ (consumer, OmniauthLib.currentAccessToken.open_!)
    var tempResponse = OmniauthLib.http(verifyCreds <> { _ \\ "user" })
    var twitterAuthMap = Map[String, Any]()
    twitterAuthMap += (OmniauthLib.Provider -> OmniauthLib.TwitterProviderName)
    twitterAuthMap += (OmniauthLib.UID -> (tempResponse \ "id").text)
    var twitterAuthUserInfoMap = Map[String, String]()
    twitterAuthUserInfoMap += (OmniauthLib.Name -> (tempResponse \ "name").text)
    twitterAuthUserInfoMap += (OmniauthLib.Nickname -> (tempResponse \ "screen_name").text)
    twitterAuthMap += (OmniauthLib.UserInfo -> twitterAuthUserInfoMap)
    var twitterAuthCredentialsMap = Map[String, String]()
    twitterAuthCredentialsMap += (OmniauthLib.Token -> OmniauthLib.currentAccessToken.open_!.value)
    twitterAuthCredentialsMap += (OmniauthLib.Secret -> secret)
    twitterAuthMap += (OmniauthLib.Credentials -> twitterAuthCredentialsMap)
    OmniauthLib.setAuthMap(twitterAuthMap)
    S.redirectTo(OmniauthLib.successRedirect)
  }
}