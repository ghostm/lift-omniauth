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


class FacebookProvider(val clientId:String, val secret:String)extends OmniauthProvider {
  def provider(): String = OmniauthLib.FacebookProviderName
  def signIn():NodeSeq = doFacebookSignin
  def callback(): NodeSeq = doFacebookCallback
  implicit val formats = net.liftweb.json.DefaultFormats

  def doFacebookSignin() : NodeSeq = {
    var requestUrl = "https://graph.facebook.com/oauth/authorize?"
    var callbackUrl = OmniauthLib.siteAuthBaseUrl+"auth/facebook/callback"
    var urlParameters = Map[String, String]()
    urlParameters += ("client_id" -> clientId)
    urlParameters += ("redirect_uri" -> callbackUrl)
    requestUrl += Http.q_str(urlParameters)
    S.redirectTo(requestUrl)
  }

  def doFacebookCallback () : NodeSeq = {
    var fbCode = S.param("code") openOr S.redirectTo("/")
    var callbackUrl = OmniauthLib.siteAuthBaseUrl+"auth/facebook/callback"
    var urlParameters = Map[String, String]()
    urlParameters += ("client_id" -> clientId)
    urlParameters += ("redirect_uri" -> callbackUrl)
    urlParameters += ("client_secret" -> secret)
    urlParameters += ("code" -> fbCode.toString)
    var tempRequest = :/("graph.facebook.com").secure / "oauth/access_token" <<? urlParameters
    var accessTokenString = OmniauthLib.http(tempRequest as_str)
    if(accessTokenString.startsWith("access_token=")){
      accessTokenString = accessTokenString.stripPrefix("access_token=")
      var ampIndex = accessTokenString.indexOf("&")
      if(ampIndex >= 0){
        accessTokenString = accessTokenString.take(ampIndex)
      }
      tempRequest = :/("graph.facebook.com").secure / "me" <<? Map("access_token" -> accessTokenString)
      val json = OmniauthLib.http(tempRequest >- JsonParser.parse)
      var fbAuthMap = Map[String, Any]()
      fbAuthMap += (OmniauthLib.Provider -> OmniauthLib.FacebookProviderName)
      fbAuthMap += (OmniauthLib.UID -> (json \\ "id").extract[String])
      var fbAuthUserInfoMap = Map[String, String]()
      fbAuthUserInfoMap += (OmniauthLib.Name -> (json \\ "name").extract[String])
      fbAuthMap += (OmniauthLib.UserInfo -> fbAuthUserInfoMap)
      var fbAuthCredentialsMap = Map[String, String]()
      fbAuthCredentialsMap += (OmniauthLib.Token -> accessTokenString)
      fbAuthMap += (OmniauthLib.Credentials -> fbAuthCredentialsMap)
      OmniauthLib.setAuthMap(fbAuthMap)
      S.redirectTo(OmniauthLib.successRedirect)
    }else{
      println("didn't find access token")
      S.redirectTo(OmniauthLib.failureRedirect)
    }
  }
}
