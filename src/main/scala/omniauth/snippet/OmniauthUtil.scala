package omniauth.snippet

import _root_.omniauth.lib.{OmniauthLib}
import xml.NodeSeq
import net.liftweb.common.{Failure, Empty, Full}

class OmniauthUtil {
  def info(xhtml: NodeSeq) = {
    OmniauthLib.currentAuthMap match {
      case Full(omni) => omni.map { s => <p>{s}</p> } toSeq
      case Empty => NodeSeq.Empty
      case Failure(_,_,_) => NodeSeq.Empty
    }  
  }
}