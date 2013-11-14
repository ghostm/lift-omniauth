/*
 * Copyright 2010-2011 Matthew Henderson
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

import xml.NodeSeq
import net.liftweb.common.{Box,Loggable}
import net.liftweb.http.S
import omniauth.Omniauth

case class AuthToken(token: String,
                     expiresIn: Option[Long],
                     refreshToken: Option[String],
                     secret: Option[String])

abstract class OmniauthProvider extends Loggable {
  def providerName: String
  def signIn(): NodeSeq
  def callback(): NodeSeq
  def validateToken(token: AuthToken): Boolean
  def tokenToId(token:AuthToken): Box[String]

  protected def extractToken(resp: String) = {
    if (resp.startsWith("access_token=")) {
      var accessToken = resp.stripPrefix("access_token=")
      val ampIndex = accessToken.indexOf("&")

      if (ampIndex >= 0) {
        accessToken = accessToken.take(ampIndex)
      }

      AuthToken(accessToken, None, None, None)
    } else {
      logger.debug("didn't find access token")
      S.redirectTo(Omniauth.failureRedirect)
    }
  }
}
