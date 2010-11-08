package omniauth.lib

import xml.NodeSeq


abstract class OmniauthProvider {
  def provider(): String
  def signIn(): NodeSeq
  def callback(): NodeSeq
}