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

class Omniauth extends LiftView {

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
    var verifier = S.param("oauth_verifier") openOr S.redirectTo("/")
    var requestToken = Omniauth.currentRequestToken openOr S.redirectTo("/")
    Omniauth.http(Auth.access_token(Omniauth.consumer, requestToken, verifier)) match {
      case (access_tok, tempUid, screen_name) => {
        Omniauth.setAccessToken(access_tok)

        println("Approved "+tempUid+" "+screen_name)
      }
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
    Omniauth.setAuthMap(twitterAuthMap)
    tempResponse
  }

  def doFacebookSignin() : NodeSeq = {
    var requestUrl = "https://graph.facebook.com/oauth/authorize?"
    var urlParameters = Map[String, String]()
    urlParameters += ("client_id" -> Omniauth.facebookClientId)
    urlParameters += ("redirect_uri" -> "http://localhost:8080/auth/facebook/callback")
    requestUrl += Http.q_str(urlParameters)
    println(requestUrl)
    S.redirectTo(requestUrl)
  }

  def doFacebookCallback () : NodeSeq = {
    println("doFacebookCallback")
    var fbCode = S.param("code") openOr S.redirectTo("/")
    var urlParameters = Map[String, String]()
    urlParameters += ("client_id" -> Omniauth.facebookClientId)
    urlParameters += ("redirect_uri" -> "http://localhost:8080/auth/facebook/callback")
    urlParameters += ("client_secret" -> Omniauth.facebookClientSecret)
    urlParameters += ("code" -> fbCode.toString)
    var tempRequest = :/("graph.facebook.com").secure / "oauth/access_token" <<? urlParameters
    var accessToken = Omniauth.http(tempRequest as_str)
    println("doFacebookCallback2")
    println(accessToken)
    <h1>Facebook</h1>
  }

}

object Omniauth {
  val Provider = "Provider"
  val UID = "UID"
  val UserInfo = "UserInfo"
  val Name = "Name"
  val Nickname = "Nickname"
  val twitterKey = ""
  val twitterSecret = ""
  val facebookClientId = ""
  val facebookClientSecret = ""
  var TwitterHost = :/("api.twitter.com").secure
  val twitterOauthRequest = TwitterHost / "oauth"
  val consumer = Consumer(twitterKey, twitterSecret)
  val http = new Http

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
