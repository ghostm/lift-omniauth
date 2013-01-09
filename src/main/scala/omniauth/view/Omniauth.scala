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

import omniauth.lib._
import dispatch.classic._
import oauth.{ Token, Consumer }
import json._
import JsHttp._
import oauth._
import oauth.OAuth._
import xml.{ Text, NodeSeq }
import net.liftweb.common.{ Loggable, Full, Empty, Box }
import net.liftweb.json.JsonParser
import net.liftweb.json.JsonAST._
import net.liftweb.http._
import net.liftweb.sitemap.{ Menu, Loc, SiteMap }
import Loc._
import net.liftweb.util.LiftFlowOfControlException

class Omniauth extends LiftView with Loggable {

  override def dispatch = {
    case "signin" => doAuthSignin _
    case "callback" => doAuthCallback _
  }

  def doAuthSignin: NodeSeq = {
    logger.debug("doAuthSignin")
    val provider = S.param("provider") openOr S.redirectTo(omniauth.Omniauth.failureRedirect)
    omniauth.Omniauth.providers.foreach(p => {
      if (p.providerName.equalsIgnoreCase(provider)) {
        logger.debug("provider match")
        try  { p.signIn } catch  {
        	//This is what we expect to happen, p.signIn should have a S.redirectTo(....) which will
            //throw the following exception if the URL is not local.
        	case rse: LiftFlowOfControlException => throw rse
        	case kaboom: Exception => logger.error("attempting auth sign in ",kaboom) 
        }                
        
      }
    })
    S.redirectTo(omniauth.Omniauth.failureRedirect)
  }

  def doAuthCallback(): NodeSeq = {
    logger.debug("doAuthCallback")
    val provider = S.param("provider") openOr S.redirectTo(omniauth.Omniauth.failureRedirect)
    omniauth.Omniauth.providers.foreach(p => {
      if (p.providerName.equalsIgnoreCase(provider)) {
        logger.debug("provider match")
        p.callback 
      }
    })
    S.redirectTo(omniauth.Omniauth.failureRedirect)
  }

}





