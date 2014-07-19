package omniauth.lib

import scala.xml.NodeSeq
import net.liftweb.http.S
import net.liftweb.common.Box
import net.liftweb.json.JsonParser
import net.liftweb.util.Helpers.tryo
import dispatch.classic.:/
import omniauth.Omniauth
import omniauth.AuthInfo

class VKProvider(appId: String, secret: String) extends OmniauthProvider{

  val API_VERSION = "5.23"

  override def providerName = VKProvider.providerName
  implicit val formats = net.liftweb.json.DefaultFormats

  override def signIn(): NodeSeq = {
    var requestUrl = "https://oauth.vk.com/oauth/authorize?"
    var urlParams = Map.empty[String, String]
    urlParams += "client_id" ->  appId
    urlParams += "response_code" -> "code"
    urlParams += "redirect_uri" -> (Omniauth.siteAuthBaseUrl + "auth/" + providerName + "/callback")
    urlParams += "v" -> API_VERSION
    urlParams += "scope" -> permissions
    requestUrl += Omniauth.q_str(urlParams)
    S.redirectTo(requestUrl)
  }

  override def callback(): NodeSeq = {
    val code = S.param("code") openOr S.redirectTo("/")
    logger.debug("GOT CODE" + code)
    val callbackUrl = Omniauth.siteAuthBaseUrl + "auth/" + providerName + "/callback"
    var urlParams = Map.empty[String, String]
    urlParams += "client_id" ->  appId
    urlParams += "client_secret" -> secret
    urlParams += "redirect_uri" -> (Omniauth.siteAuthBaseUrl + "auth/" + providerName + "/callback")
    urlParams += "code" -> code
    val tmpRequest = (:/("oauth.vk.com").secure / "access_token").POST <:<
      Map("Content-Type" -> "application/x-www-form-urlencoded") << urlParams

    val json = Omniauth.http(tmpRequest >- JsonParser.parse)

    val accessToken = tryo {
      AuthToken(
          (json \ "access_token").extract[String],
          (json \ "expires_in").extract[Option[Long]],
          (json \ "user_id").extract[Option[String]],
          (json \ "email").extract[Option[String]]
      )
    }

    S.redirectTo((for {
      t <- accessToken
      if (validateToken(t))
    } yield {
      Omniauth.successRedirect
    }) openOr Omniauth.failureRedirect)
  }

  override def validateToken(token: AuthToken): Boolean = {
    token.refreshToken.map { uid =>
      val email = token.secret.getOrElse("")
      var urlParams = Map.empty[String, String]
      urlParams += "user_id" -> token.refreshToken.getOrElse("")
      urlParams += "v" -> API_VERSION
      urlParams += "access_token" -> token.token
      val tmpRequest = (:/("api.vk.com").secure / "method" / "users.get") <:<
          Map("Content-Type" -> "application/x-www-form-urlencoded") << urlParams

      try {
        val json = Omniauth.http(tmpRequest >- JsonParser.parse)
        val user = (json \ "response")(0)
        val firstName = (user \ "first_name").extract[String]
        val lastName = (user \ "last_name").extract[String]
        val name = (firstName + " " + lastName).trim()
        val ai = AuthInfo(providerName, uid, name, token, None, Some(name), Some(email), Some(firstName), Some(lastName))
        Omniauth.setAuthInfo(ai)
        logger.debug(ai)
        true
      } catch {
        case e: Throwable => false
      }
    } getOrElse false
  }

  override def tokenToId(token: AuthToken): Box[String] = {
    token.refreshToken
  }

  def permissions = Properties.get(VKProvider.providerPropertyPermissions) openOr ""
}

object VKProvider {
  val providerName = "vk"
  val providerPropertyKey = "omniauth.vkkey"
  val providerPropertySecret = "omniauth.vksecret"
  val providerPropertyPermissions = "omniauth.vkpermissions"
}
