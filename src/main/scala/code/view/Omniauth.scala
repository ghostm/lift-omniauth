package code.view

import dispatch._
import oauth.{Token, Consumer}
import json._
import JsHttp._
import oauth._
import oauth.OAuth._
import twitter.{Status, Twitter, Auth}
import xml.{Text, NodeSeq}
import net.liftweb.http._
import net.liftweb.common.{Full, Empty, Box}
import net.liftweb.json.JsonParser
import net.liftweb.json.JsonAST._

class Omniauth extends LiftView {
  implicit val formats = net.liftweb.json.DefaultFormats
  
  override def dispatch = {
    case "signin" => doAuthSignin _
    case "callback" => doAuthCallback _
  }

  def doAuthSignin : NodeSeq = {
    var provider = S.param("provider") openOr S.redirectTo("/")
    provider match {
      case "twitter" => doTwitterSignin
      case "facebook" => doFacebookSignin
    }
  }

  def doAuthCallback () : NodeSeq = {
    var provider = S.param("provider") openOr S.redirectTo("/")
    provider match {
      case "twitter" => doTwitterCallback
      case "facebook" => doFacebookCallback
    }
  }

  def twitterAuthenticateUrl(token: Token) = Omniauth.twitterOauthRequest / "authenticate" <<? token

  def doTwitterSignin () : NodeSeq = {
    var requestToken = Omniauth.http(Auth.request_token(Omniauth.consumer, "http://localhost:8080/auth/twitter/callback"))
    val auth_uri = twitterAuthenticateUrl(requestToken).to_uri
    Omniauth.setRequestToken(requestToken)
    S.redirectTo(auth_uri.toString)
  }

  def doTwitterCallback () : NodeSeq = {
    var verifier = S.param("oauth_verifier") openOr S.redirectTo(Omniauth.failureRedirect)
    var requestToken = Omniauth.currentRequestToken openOr S.redirectTo(Omniauth.failureRedirect)
    Omniauth.http(Auth.access_token(Omniauth.consumer, requestToken, verifier)) match {
      case (access_tok, tempUid, screen_name) => {
        Omniauth.setAccessToken(access_tok)
      }
      case _ => S.redirectTo(Omniauth.failureRedirect)
    }
    var verifyCreds = Omniauth.TwitterHost / "1/account/verify_credentials.xml" <@ (Omniauth.consumer, Omniauth.currentAccessToken.open_!)
    var tempResponse = Omniauth.http(verifyCreds <> { _ \\ "user" })
    var twitterAuthMap = Map[String, Any]()
    twitterAuthMap += (Omniauth.Provider -> "twitter")
    twitterAuthMap += (Omniauth.UID -> (tempResponse \ "id").text)
    var twitterAuthUserInfoMap = Map[String, String]()
    twitterAuthUserInfoMap += (Omniauth.Name -> (tempResponse \ "name").text)
    twitterAuthUserInfoMap += (Omniauth.Nickname -> (tempResponse \ "screen_name").text)
    twitterAuthMap += (Omniauth.UserInfo -> twitterAuthUserInfoMap)
    var twitterAuthCredentialsMap = Map[String, String]()
    twitterAuthCredentialsMap += (Omniauth.Token -> Omniauth.currentAccessToken.open_!.value)
    twitterAuthCredentialsMap += (Omniauth.Secret -> Omniauth.twitterSecret)
    twitterAuthMap += (Omniauth.Credentials -> twitterAuthCredentialsMap)
    Omniauth.setAuthMap(twitterAuthMap)
    S.redirectTo(Omniauth.successRedirect)
  }

  def doFacebookSignin() : NodeSeq = {
    var requestUrl = "https://graph.facebook.com/oauth/authorize?"
    var urlParameters = Map[String, String]()
    urlParameters += ("client_id" -> Omniauth.facebookClientId)
    urlParameters += ("redirect_uri" -> "http://localhost:8080/auth/facebook/callback")
    requestUrl += Http.q_str(urlParameters)
    S.redirectTo(requestUrl)
  }

  def doFacebookCallback () : NodeSeq = {
    var fbCode = S.param("code") openOr S.redirectTo("/")
    var urlParameters = Map[String, String]()
    urlParameters += ("client_id" -> Omniauth.facebookClientId)
    urlParameters += ("redirect_uri" -> "http://localhost:8080/auth/facebook/callback")
    urlParameters += ("client_secret" -> Omniauth.facebookClientSecret)
    urlParameters += ("code" -> fbCode.toString)
    var tempRequest = :/("graph.facebook.com").secure / "oauth/access_token" <<? urlParameters
    var accessTokenString = Omniauth.http(tempRequest as_str)
    if(accessTokenString.startsWith("access_token=")){
      accessTokenString = accessTokenString.stripPrefix("access_token=")
      var ampIndex = accessTokenString.indexOf("&")
      if(ampIndex >= 0){
        accessTokenString = accessTokenString.take(ampIndex)
      }
      tempRequest = :/("graph.facebook.com").secure / "me" <<? Map("access_token" -> accessTokenString)
      val json = Omniauth.http(tempRequest >- JsonParser.parse)
      var fbAuthMap = Map[String, Any]()
      fbAuthMap += (Omniauth.Provider -> "facebook")
      fbAuthMap += (Omniauth.UID -> (json \\ "id").extract[String])
      var fbAuthUserInfoMap = Map[String, String]()
      fbAuthUserInfoMap += (Omniauth.Name -> (json \\ "name").extract[String])
      fbAuthMap += (Omniauth.UserInfo -> fbAuthUserInfoMap)
      var fbAuthCredentialsMap = Map[String, String]()
      fbAuthCredentialsMap += (Omniauth.Token -> accessTokenString)
      fbAuthMap += (Omniauth.Credentials -> fbAuthCredentialsMap)
      Omniauth.setAuthMap(fbAuthMap)
      S.redirectTo(Omniauth.successRedirect)
    }else{
      println("didn't find access token")
      S.redirectTo(Omniauth.failureRedirect)
    }
  }

}

object Omniauth {
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
  val twitterKey = ""
  val twitterSecret = ""
  val facebookClientId = ""
  val facebookClientSecret = ""
  val consumer = Consumer(twitterKey, twitterSecret)
  var successRedirect = "/"
  var failureRedirect = "/"

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
}
