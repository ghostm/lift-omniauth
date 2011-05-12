# Lift-Omniauth Module
## Usage
in Boot.scala

    import omniauth.lib._
    ...
    //Add Omniauth to the sitemap
    Omniauth.sitemap
    //Omniauth init
    OmniauthLib.init

in your properties file define your client id (key) and secret for each provider you will use

    omniauth.facebooksecret=...
    omniauth.facebookkey=...

After a user has logged into an auth provider you can access data through the session var OmniauthLib.currentAuthMap

    OmniauthLib.currentAuthMap match {
      case Full(omni) => ({
        println(omni.get(OmniauthLib.Provider))
        println(omni.get(OmniauthLib.UID))
        println(omni.get(OmniauthLib.UserInfo))
      })
    }

## Installation

To install Lift-OmniAuth, simply add the Lift-Omniauth.jar to the build path and add Dispatch's Twitter as a dependancy.
Using SBT:
Add Lift-Omniauth.jar to the ./lib folder
Add the following to your SBT project file.

    val databinder_net = "databinder.net repository" at "http://databinder.net/repo"
    val dispatch = "net.databinder" %% "dispatch-twitter" % "0.7.7"

    
## Providers

Lift-OmniAuth currently supports the following external providers:

* via OAuth
  * Facebook
  * Twitter
  * Github
