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

package omniauth.view
import omniauth.lib.OmniauthLib
import dispatch._
import oauth.{Token, Consumer}
import json._
import JsHttp._
import oauth._
import oauth.OAuth._
import xml.{Text, NodeSeq}
import net.liftweb.common.{Full, Empty, Box}
import net.liftweb.json.JsonParser
import net.liftweb.json.JsonAST._
import net.liftweb.http._
import net.liftweb.sitemap.{Menu, Loc, SiteMap}
import Loc._


class Omniauth extends LiftView {

  override def dispatch = {
    case "signin" => doAuthSignin _
    case "callback" => doAuthCallback _
  }

  def doAuthSignin : NodeSeq = {
    println("doAuthSignin")
    var provider = S.param("provider") openOr S.redirectTo(OmniauthLib.failureRedirect)
    OmniauthLib.providers.foreach(p => {
      if(p.provider.equalsIgnoreCase(provider)){
        println("provider match")
        p.signIn
      }
    })
    S.redirectTo(OmniauthLib.failureRedirect)
  }

  def doAuthCallback () : NodeSeq = {
    var provider = S.param("provider") openOr S.redirectTo(OmniauthLib.failureRedirect)
    OmniauthLib.providers.foreach(p => {
      if(p.provider.equalsIgnoreCase(provider)){
        p.callback
      }
    })
    S.redirectTo(OmniauthLib.failureRedirect)
  }

}





