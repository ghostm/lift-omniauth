package omniauth.lib

import omniauth.Omniauth
import dispatch.classic._
import xml.NodeSeq
import net.liftweb.common.{Full, Empty, Box}
import net.liftweb.json.JsonParser
import net.liftweb.http._
import omniauth.AuthInfo


class MSLiveProvider(val clientId:String, val secret:String) extends OmniauthProvider {
  def providerName = MSLiveProvider.providerName
  def providerPropertyKey = MSLiveProvider.providerPropertyKey
  def providerPropertySecret = MSLiveProvider.providerPropertySecret

  def mslivePermissions =
    Properties.get("omniauth.mslivepermissions") openOr ""

  def signIn():NodeSeq = doMSLiveSignin
  def callback(): NodeSeq = doMSLiveCallback
  implicit val formats = net.liftweb.json.DefaultFormats

  def doMSLiveSignin() : NodeSeq = {
    var requestUrl = "https://login.live.com/oauth20_authorize.srf?"
    val callbackUrl = Omniauth.siteAuthBaseUrl+"auth/"+providerName+"/callback"
    var urlParameters = Map[String, String]()
    urlParameters += ("client_id" -> clientId)
    urlParameters += ("redirect_uri" -> callbackUrl)
    urlParameters += ("scope" -> mslivePermissions)
    urlParameters += ("locale" -> S.locale.getLanguage)
    urlParameters += ("response_type" -> "code")
    requestUrl += Omniauth.q_str(urlParameters)
    S.redirectTo(requestUrl)
  }

  def doMSLiveCallback () : NodeSeq = {
    val fbCode = S.param("code") openOr S.redirectTo("/")
    val callbackUrl = Omniauth.siteAuthBaseUrl+"auth/"+providerName+"/callback"
    var urlParameters = Map[String, String]()
    urlParameters += ("client_id" -> clientId)
    urlParameters += ("redirect_uri" -> callbackUrl)
    urlParameters += ("client_secret" -> secret)
    urlParameters += ("code" -> fbCode.toString)
    urlParameters += ("grant_type" -> "authorization_code")
    val tempRequest = :/("login.live.com").secure / "oauth20_token.srf" <<? urlParameters

    val accessToken = try{
      val json = Omniauth.http(tempRequest >- JsonParser.parse)
      json.extract[AuthToken]
    } catch {
      case _ : Throwable =>
        logger.debug("didn't find access tokenss")
        S.redirectTo(Omniauth.failureRedirect)
    }
    if(validateToken(accessToken)){
      S.redirectTo(Omniauth.successRedirect)
    }else{
      S.redirectTo(Omniauth.failureRedirect)
    }

  }

  def validateToken(accessToken:AuthToken): Boolean = {
    val tempRequest = :/("apis.live.net").secure / "v5.0/me" <<? Map("access_token" -> accessToken.token)
    try{
      val json = Omniauth.http(tempRequest >- JsonParser.parse)

      val uid =  (json \ "id").extract[String]
      val name =  (json \ "name").extract[String]
      val firstName = (json \ "first_name").extract[String]
      val lastName = (json \ "last_name").extract[String]
      val email = (json \ "emails" \ "preferred").extract[String]
      val ai = AuthInfo(providerName,uid,name,accessToken,Some(secret),
        Some(name), Some(email), Some(firstName), Some(lastName))
      Omniauth.setAuthInfo(ai)
      logger.debug(ai)

      true
    } catch {
      case _ : Throwable => false
    }
  }

  def tokenToId(accessToken:AuthToken): Box[String] = {
    val tempRequest = :/("apis.live.net/v5.0/").secure / "me" <<? Map("access_token" -> accessToken.token)
    try{
      val json = Omniauth.http(tempRequest >- JsonParser.parse)
      Full((json \ "id").extract[String])
    } catch {
      case _ : Throwable => Empty
    }
  }

}


object MSLiveProvider {
  val providerName = "mslive"
  val providerPropertyKey = "omniauth.mslivekey"
  val providerPropertySecret = "omniauth.mslivesecret"
}
