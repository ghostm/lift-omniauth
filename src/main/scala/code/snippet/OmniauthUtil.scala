package code.snippet
import code.view.Omniauth
import xml.NodeSeq
import net.liftweb.common.{Failure, Empty, Full}
import collection.breakOut

class OmniauthUtil {
  def info(xhtml: NodeSeq) = {
    Omniauth.currentAuthMap match {
      case Full(omni) => omni.map { s => <p>{s}</p> } toSeq
      case Empty => NodeSeq.Empty
      case Failure(_,_,_) => NodeSeq.Empty
    }  
  }
}