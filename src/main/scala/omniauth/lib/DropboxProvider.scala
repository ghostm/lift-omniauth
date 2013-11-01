package omniauth.lib

import omniauth.Omniauth

import net.liftweb._
import common._
import http.S
import json.JsonParser

import scala.xml.NodeSeq

import java.security.SecureRandom 

import dispatch.classic._

class DropboxProvider (val key:String, val secret:String) extends OmniauthProvider{
  implicit val formats = net.liftweb.json.DefaultFormats
  def providerName = DropboxProvider.providerName
  
  val csrf = {
    val bs:Array[Byte] = (1 to 16).map(_.asInstanceOf[Byte]).toArray
    val r = SecureRandom.getInstance("SHA1PRNG", "SUN")
    r.nextBytes(bs)
    bs.map(Integer.toHexString(_)).reduce(_ + _)
  }
  
  def callbackUrl = Omniauth.siteAuthBaseUrl+"auth/"+providerName+"/callback"
  
  def signIn() = {
    val baseReqUrl = "https://www.dropbox.com/1/oauth2/authorize?"
    val params = Map(
      "client_id" -> key,
      "response_type" -> "code",
      "redirect_uri" -> callbackUrl,
      "state" -> csrf
    )
    S.redirectTo(baseReqUrl + Omniauth.q_str(params))
  }
  
  def callback() = {
    val state = S.param("state") openOr ("")
    
    if(csrf == state) {
      S.param("code") match {
        case Full(code) => {
          val req = :/("api.dropbox.com").secure / "1/oauth2/token" << Map(
            "code" -> code,
            "grant_type" -> "authorization_code",
            "redirect_uri" -> callbackUrl,
            "client_id" -> key,
            "client_secret" -> secret
          )
          
          val res = Omniauth.http(req >- JsonParser.parse)
          val token     = (res \ "access_token").extract[String]
          val tokenType = (res \ "token_type").extract[String]
          val uid       = (res \ "uid").extract[String]
          
          if(validateToken(token)) S.redirectTo(Omniauth.successRedirect)
          else S.redirectTo(Omniauth.failureRedirect)
        }
        case _ => S.redirectTo(Omniauth.failureRedirect)

      }
    } else S.redirectTo(Omniauth.failureRedirect)
    
    NodeSeq.Empty
  }
  
  def validateToken(token:String) = {
    try {
      val (res, name, uid) = accountInfo(token)

      Omniauth.setAuthInfo(omniauth.AuthInfo(
        providerName,
        uid,
        name,
        token,
        Some(secret),
        Some(name)))
      
      true
    } catch {
      case e: Exception => false
    }
  }
  
  def tokenToId(token:String) = accountInfo(token) match {
    case (res, name, uid) => Full(uid)
    case _ => Empty
  }
  
  
  def accountInfo(token:String) = {
    val req = :/("api.dropbox.com").secure / "1/account/info" << Map(
      "locale" -> S.locale.getLanguage()) <:< Map(
      "Authorization" -> ("Bearer "+token))

    val res = Omniauth.http(req >- JsonParser.parse)
    val name = (res \ "display_name").extract[String]
    val uid = (res \ "uid").extract[String]
    
    (res, name, uid)
  }
}

object DropboxProvider {
  val providerName = "dropbox"
  val providerPropertyKey = "omniauth.dropboxkey"
  val providerPropertySecret = "omniauth.dropboxsecret"
}