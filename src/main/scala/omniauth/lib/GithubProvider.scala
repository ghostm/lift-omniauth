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
  def providerName = GithubProvider.providerName
  def providerPropertyKey = GithubProvider.providerPropertyKey
  def providerPropertySecret = GithubProvider.providerPropertySecret

  private val  githubScope =  Properties.get("omniauth.github.scope") openOr ""
  
  def signIn():NodeSeq = doGithubSignin
  def callback(): NodeSeq = doGithubCallback

  implicit val formats = net.liftweb.json.DefaultFormats

  def doGithubSignin() : NodeSeq = {
    var requestUrl = "https://github.com/login/oauth/authorize?"
    val callbackUrl = Omniauth.siteAuthBaseUrl+"auth/"+providerName+"/callback"
    var urlParameters = Map[String, String]()
    urlParameters += ("client_id" -> clientId)
    urlParameters += ("redirect_uri" -> callbackUrl)
    urlParameters += ("state" -> csrf)
    urlParameters += ("scope" -> githubScope)        
    requestUrl += Omniauth.q_str(urlParameters)
    logger.info("Redirecting user to: "+requestUrl)
    S.redirectTo(requestUrl)
  }

  def doGithubCallback () : NodeSeq = {
    execWithStateValidation {
      val ghCode = S.param("code") openOr S.redirectTo("/")
      val callbackUrl = Omniauth.siteAuthBaseUrl+"auth/"+providerName+"/callback"
      var urlParameters = Map[String, String]()
      urlParameters += ("client_id" -> clientId)
      urlParameters += ("redirect_uri" -> callbackUrl)
      urlParameters += ("client_secret" -> secret)
      urlParameters += ("code" -> ghCode.toString)
      val tempRequest = :/("github.com").secure / "login/oauth/access_token" <<? urlParameters

      val accessToken = extractToken(Omniauth.http(tempRequest as_str))

      if(validateToken(accessToken)){
        S.redirectTo(Omniauth.successRedirect)
      }else{
        S.redirectTo(Omniauth.failureRedirect)
      }
    }
  }

  def validateToken(accessToken:AuthToken): Boolean = {
    val userReq = GithubProvider.makeApiRequest("user", "access_token" -> accessToken.token)
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
      case _ : Throwable => false
    }
  }

  def tokenToId(accessToken:AuthToken): Box[String] = {
    val userReq = GithubProvider.makeApiRequest("user", "access_token" -> accessToken.token)
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

