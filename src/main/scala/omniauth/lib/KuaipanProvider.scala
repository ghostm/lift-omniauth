package omniauth.lib

import java.util.Date

import net.liftweb._
import net.liftweb.common.Full
import net.liftweb.http.S
import omniauth.Omniauth
import json.JsonParser

import dispatch.classic._

/**
 * Created by twang on 12/8/2014.
 */
class KuaipanProvider (val key:String, val secret:String) extends OmniauthProvider {
  implicit val formats = net.liftweb.json.DefaultFormats
  def providerName = KuaipanProvider.providerName

  def callbackUrl = Omniauth.siteAuthBaseUrl+"auth/"+providerName+"/callback"

  var temporaryToken = ""
  var temporaryTokenSecret = ""

  // Step 1: get temporary token
  private def getTemporaryToken() = {
    val params = Map(
      "oauth_consumer_key" -> key,
      "oauth_nonce" -> csrf,
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_timestamp" -> (new Date().getTime()+"").substring(0, 10),
      "oauth_version" -> "1.0",
      "oauth_callback" -> callbackUrl
    )
    val oauth_signature = generateSignature("GET", "https://openapi.kuaipan.cn/open/requestToken", secret+"&", params)
    val req = :/("openapi.kuaipan.cn").secure / "open/requestToken" <<? (params ++ Map("oauth_signature" -> oauth_signature))
    Omniauth.http(req >- JsonParser.parse)
  }

  // Step 2: sign in
  def signIn() = {
    val temporaryTokenResponse = getTemporaryToken()
    temporaryToken = (temporaryTokenResponse \ "oauth_token").extract[String]
    temporaryTokenSecret = (temporaryTokenResponse \ "oauth_token_secret").extract[String]
    val authorizeRequestUrl = "https://www.kuaipan.cn/api.php?ac=open&op=authorise&oauth_token=" + temporaryToken
    S.redirectTo(authorizeRequestUrl)
  }

  // Step 3: get token
  def callback() = {
    execWithStateValidation {
      S.param("oauth_verifier") match {
        case Full(oauth_verifier) => {
          val params = Map(
            "oauth_consumer_key" -> key,
            "oauth_nonce" -> csrf,
            "oauth_signature_method" -> "HMAC-SHA1",
            "oauth_timestamp" -> (new Date().getTime()+"").substring(0, 10),
            "oauth_token" -> temporaryToken,
            "oauth_version" -> "1.0"
          )
          val oauth_signature = generateSignature("GET", "https://openapi.kuaipan.cn/open/accessToken", secret+"&"+temporaryTokenSecret, params)
          val req = :/("openapi.kuaipan.cn").secure / "open/accessToken" <<? (params ++ Map("oauth_signature" -> oauth_signature))
          val json = Omniauth.http(req >- JsonParser.parse)
          Omniauth.setAuthInfo(omniauth.AuthInfo(
            providerName,
            (json \ "user_id").extract[String],
            "",
            AuthToken((json \ "oauth_token").extract[String], None, None, Some((json \ "oauth_token_secret").extract[String]))
          ))

          logger.debug("token validated")
          S.redirectTo(Omniauth.successRedirect)
        }
        case _ => {
          logger.debug("code was not returned from Kuaipan")
          S.redirectTo(Omniauth.failureRedirect)
        }
      }
    }
  }

  def validateToken(token:AuthToken) = true // For Kuaipan, don't need to do this step.
  def tokenToId(token:AuthToken) = None // For Kuaipan, don't need to do this step.
}

object KuaipanProvider {
  val providerName = "kuaipan"
  val providerPropertyKey = "omniauth.kuaipankey"
  val providerPropertySecret = "omniauth.kuaipansecret"
}