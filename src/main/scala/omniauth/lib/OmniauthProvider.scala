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

import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import sun.misc.BASE64Encoder

import xml.NodeSeq
import net.liftweb.common.{Box,Loggable}
import net.liftweb.http.S
import omniauth.Omniauth
import java.security.SecureRandom

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

  def csrf = {
    val bs: Array[Byte] = new Array(16)
    val r = SecureRandom.getInstance("SHA1PRNG", "SUN")
    r.nextBytes(bs)
    bs.map(Integer.toHexString(_)).reduce(_ + _)
  }

  // HMAC-SHA1 method
  private def sign_hmacsha1(signature_baseString:String, secret:String):String = {
    val mac = Mac.getInstance("HmacSHA1")
    val spec = new SecretKeySpec(secret.getBytes(), "HmacSHA1")
    mac.init(spec)
    val byteHMAC = mac.doFinal(signature_baseString.getBytes())
    new BASE64Encoder().encode(byteHMAC)
  }

  protected def generateSignature(request_method:String, base_url:String, secret:String, params:Map[String, String]):String = {
    val signature_baseString = request_method + "&" +
      URLEncoder.encode(base_url, "UTF-8") + "&" +
      URLEncoder.encode(
        params.toList.sortBy(_._1).map(p => p._1 + "=" + URLEncoder.encode(p._2, "UTF-8")).mkString("&"), "UTF-8")
    sign_hmacsha1(signature_baseString, secret)
  }

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

  protected def execWithStateValidation(f: () => Unit) = {
    val state = S.param("state") openOr ""

    if(csrf == state) {
      f()
    } else {
      logger.debug("state did not match")
      S.redirectTo(Omniauth.failureRedirect)
    }

    NodeSeq.Empty
  }
}
