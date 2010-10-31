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
    case "callback" => doTwitterCallback _
  }

  def doAuthSignin : NodeSeq = {
    var provider = S.param("provider") openOr S.redirectTo("/")
    provider match {
      case "twitter" => doTwitterSignin
    }
  }

  def doAuthCallback () : NodeSeq = {
    var provider = S.param("provider") openOr S.redirectTo("/")
    provider match {
      case "twitter" => doTwitterCallback
    }
  }

  def twitterAuthenticateUrl(token: Token) = Omniauth.twitterOauthRequest / "authenticate" <<? token

  def doTwitterSignin () : NodeSeq = {
    var requestToken = Omniauth.http(Auth.request_token(Omniauth.consumer, "http://localhost:8080/auth/twitter/callback"))
    println("doTwitterSignin1")
    val auth_uri = twitterAuthenticateUrl(requestToken).to_uri
    println("doTwitterSignin2")
    Omniauth.setRequestToken(requestToken)
    println("doTwitterSignin3 "+Omniauth.currentRequestToken.open_!)
    S.redirectTo(auth_uri.toString)
  }

  def doTwitterCallback () : NodeSeq = {
    println("doTwitterCallback")
    var verifier = S.param("oauth_verifier") openOr S.redirectTo("/")
    println("doTwitterCallback2")
    var requestToken = Omniauth.currentRequestToken openOr S.redirectTo("/")
    println("doTwitterCallback3")
    Omniauth.http(Auth.access_token(Omniauth.consumer, requestToken, verifier)) match {
      case (access_tok, tempUid, screen_name) => {
        Omniauth.setAccessToken(access_tok)
        println("Approved "+tempUid+" "+screen_name)
      }
    }
    var verifyCreds = Omniauth.TwitterHost / "1/account/verify_credentials.xml" <@ (Omniauth.consumer, Omniauth.currentAccessToken.open_!)
    var tempResponse = Omniauth.http(verifyCreds <> { _ \\ "user" })
    tempResponse
  }

}

object Omniauth {
  val twitterKey = ""
  val twitterSecret = ""
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
}
