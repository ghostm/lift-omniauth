/*
 * Copyright 2010-2014 Matthew Henderson
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

import java.util.UUID

import xml.NodeSeq

import dispatch.classic._
import net.liftweb.common.{Failure, Full, Empty, Box}
import net.liftweb.util.Helpers._
import net.liftweb.json._
import net.liftweb.http._
import net.liftweb.util.Props

import omniauth.Omniauth
import omniauth.AuthInfo

class GithubProvider(val clientId:String, val secret:String) extends OmniauthProvider{
  private val state = UUID.randomUUID().toString
  
  def providerName = GithubProvider.providerName
  def providerPropertyKey = GithubProvider.providerPropertyKey
  def providerPropertySecret = GithubProvider.providerPropertySecret

  private val githubScope = Props.get("omniauth.github.scope") openOr ""
  
  def signIn():NodeSeq = doGithubSignin
  def callback(): NodeSeq = doGithubCallback

  implicit val formats = net.liftweb.json.DefaultFormats

  def doGithubSignin() : NodeSeq = {
    var requestUrl = "https://github.com/login/oauth/authorize?"
    val callbackUrl = Omniauth.siteAuthBaseUrl+"auth/"+providerName+"/callback"
    var urlParameters = Map[String, String]()
    urlParameters += ("client_id" -> clientId)
    urlParameters += ("redirect_uri" -> callbackUrl)
    urlParameters += ("state" -> state)    
    urlParameters += ("scope" -> githubScope)        
    requestUrl += Omniauth.q_str(urlParameters)
    logger.info("Redirecting user to: "+requestUrl)
    S.redirectTo(requestUrl)
  }

  def doGithubCallback () : NodeSeq = {
    logger.info("Handling Github Callback")

    // Ensures UUID state value matches, for use in for comprehension.
    def MatchingState(value: String): Box[Boolean] =
      if (value == state) Full(true) else Failure("Expected state %s does not match value from Github %s" format (state,value))

    val tokenRequest : Box[Request] =
      for {
        suppliedState <- (S param "state") ?~ "Missing state"
        _             <- MatchingState(suppliedState)
        ghCode        <- (S param "code") ?~ "Missing code"
      } yield {
        val callbackUrl = Omniauth.siteAuthBaseUrl+"auth/"+providerName+"/callback"
        val urlParameters = Map[String, String](
          "client_id"     -> clientId,
          "redirect_uri"  -> callbackUrl,
          "client_secret" -> secret,
          "code"          -> ghCode.toString)
        :/("github.com").secure / "login/oauth/access_token" <<? urlParameters
    }

    tokenRequest match {
      case Full(req) =>
        requestAccessToken(req) match {
          case Full(tok) if validateToken(tok) =>
            S.redirectTo(Omniauth.successRedirect)
          case tok =>
            logger.error("Missing or invalid token: "+tok)
            S.redirectTo(Omniauth.failureRedirect)
         }

      case f =>
        logger.error(f)
        S.redirectTo("/")
    }

  }

  private def requestAccessToken(req: Request) : Box[String] = {
    var accessTokenString = Omniauth.http(req as_str)
    logger.info("accessTokenString = "+accessTokenString)
    if (accessTokenString.startsWith("access_token=")) {
      accessTokenString = accessTokenString.stripPrefix("access_token=")
      val ampIndex = accessTokenString.indexOf("&")
      if(ampIndex >= 0){
        accessTokenString = accessTokenString.take(ampIndex)
      }
      Full(accessTokenString)
  } else Empty
  }

  /**
   * Validates the given token, and on success, sets the `Omniauth.setAuthInfo` state.
   * @param accessToken the token to validate
   * @return true if validation success; false otherwise.
   */
  def validateToken(accessToken:String): Boolean = {
    val userReq = GithubProvider.makeApiRequest("user", "access_token" -> accessToken)
    try{
      val json = Omniauth.http(userReq >- JsonParser.parse)
      
      val uid =  (json \ "id").extract[String]
      val name =  (json \ "name").extract[String]
      val _email = json \ "email"
      val email =   ( _email == JNull ) ? None |  _email.extractOpt[String] //To avoid getting email = Some(null)
      val username =  (json \ "login").extractOpt[String]

      val ai =
        AuthInfo(
          provider = providerName,
          uid = uid,
          name = name,
          email = email,
          nickName = username,
          token = accessToken
        )
      Omniauth.setAuthInfo(ai)
      logger.debug(ai)
      true
    } catch {
      case ex : Throwable =>
        logger.error("Failed to validate token", ex)
        false
    }
  }

  def tokenToId(accessToken:String): Box[String] = {
    val userReq = GithubProvider.makeApiRequest("user", "access_token" -> accessToken)
    try{
      val json = Omniauth.http(userReq >- JsonParser.parse)
      Full((json \ "id").extract[String])
    } catch {
      case _ : Throwable => Empty
    }
  }

}

object GithubProvider {
  val providerName = "github"
  val providerPropertyKey = "omniauth.githubkey"
  val providerPropertySecret = "omniauth.githubsecret"

  /**
   * The Github API requires certain values to be supplied in the header, and this
   * method adds them. See: http://developer.github.com/v3/#user-agent-required
   * @param githubPath the path into the API, such as "user".
   * @param params option list of "key -> value" pairs to send as parameters.
   * @return the prepared Request object ready to call.
   */
  def makeApiRequest(githubPath: String, params: (String,String)*) : Request = {
    val headers = Map(
      "User-Agent" -> "Lift Omniauth",
      "Accept"     -> "application/vnd.github.v3")
    :/("api.github.com").secure / githubPath <<? Map(params:_*) <:< headers
  }

}

