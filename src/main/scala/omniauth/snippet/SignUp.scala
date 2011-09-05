package omniauth.snippet
import net.liftweb.http.S
import net.liftweb.http.LiftRules

object SignUp {
  
  val twitterPath = "/auth/twitter/signin"
  val facebookPath = "/auth/facebook/signin"
  
  val twitterAltText = "Sign in with Twitter"
  val facebookAltText = "Sign in with facebook"
    
  private def twitter(img:String) =  <a href={twitterPath} ><img src={S.contextPath + "/" + LiftRules.resourceServerPath + "/img/" + img + ".png"} alt={twitterAltText} /></a> 
  private def facebook(img:String) = <a href={facebookPath} ><img src={S.contextPath + "/" + LiftRules.resourceServerPath + "/img/" + img + ".png"} alt={facebookAltText} /></a>
  
  
  def TwitterLightBackground = twitter("sign-in-with-twitter-l")
  def TwitterDarkBackground = twitter("sign-in-with-twitter-d")
  def TwitterTransparentLightIcon = twitter("sign-in-with-twitter-l-sm")
  def TwitterTransparentDarkIcon =twitter("sign-in-with-twitter-d-sm") 
  
  def FacebookNormal = facebook("LoginWithFacebookNormal")
  def FacebookNormalDark = facebook("LoginWithFacebookNormalDark")
  def FacebookLarge = facebook("LoginWithFacebookLarge")
  def FacebookLargeDark = facebook("LoginWithFacebookLargeDark")
   
  
}