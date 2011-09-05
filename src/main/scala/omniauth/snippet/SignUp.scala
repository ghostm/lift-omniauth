package omniauth.snippet
import net.liftweb.http.S
import net.liftweb.http.LiftRules

object SignUp {
  
  val path = "/auth/twitter/signin"
  
  val altText = "Sign in with Twitter"
    
  private def twitter(img:String) =  <a href={path} ><img src={S.contextPath + "/" + LiftRules.resourceServerPath + "/img/" + img} alt={altText} /></a> 
    
  def TwitterLightBackground = twitter("sign-in-with-twitter-l.png")
  
  def TwitterDarkBackground = twitter("sign-in-with-twitter-d.png")

  def TwitterTransparentLightIcon = twitter("sign-in-with-twitter-l-sm.png")
  
  def TwitterTransparentDarkIcon =twitter("sign-in-with-twitter-d-sm.png") 

  
  

}