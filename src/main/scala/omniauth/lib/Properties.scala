package omniauth.lib

import net.liftweb.util.Props
import scala.sys.SystemProperties
import net.liftweb.common.{Full, Box}

/**
 * Property access utility which favors SystemProperties over the Lift Props.  The rationale is that system
 * properties are often set via the command-line or the cloud container, and hence are per-deployment specific
 * in those cases.  Therefore, they are regarded as having higher precedence than the Lift properties.
 *
 * Created by barnesjd on 1/25/14.
 */
object Properties {
  private val sysProps = new SystemProperties()

  def get(key:String):Box[String] = sysProps.get(key) match {
    case Some(v) => Full(v)
    case _ => Props.get(key)
  }
}
