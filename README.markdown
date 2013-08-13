# Lift-Omniauth Module
## Usage
in Boot.scala

    import omniauth.lib._
    ...
    //Add Omniauth to the sitemap
    Omniauth.sitemap
    //init
    //Supply a list of providers
    Omniauth.initWithProviders(List(new FacebookProvider("key", "secret")))
    //or init with providers in properties
    Omniauth.init


in your properties file define your client id (key) and secret for each provider you will use

    omniauth.facebooksecret=...
    omniauth.facebookkey=...

For Facebook provider you can set permissions. For example:

    omniauth.facebookpermissions=email,read_stream

After a user has logged into an auth provider you can access data through the session var OmniauthLib.currentAuthMap

    OmniauthLib.currentAuthMap match {
      case Full(omni) => ({
        println(omni.get(OmniauthLib.Provider))
        println(omni.get(OmniauthLib.UID))
        println(omni.get(OmniauthLib.UserInfo))
      })
    }

You can also use obtain a user's unique ID from a provider without using sessions using Omniauth.tokenToId(provider:String, token:String): Box[String]

    Omniauth.tokenToId("facebook", token) match {
      case Full(uid) => user.doSomethingWithFacebookId(uid)
    }

## Installation

A big thank you to [jonoabroad](https://github.com/jonoabroad) for [hosting builds](https://liftmodules.ci.cloudbees.com/job/Omniauth%20Lift%20Module/) to make using much easier.

To include this module in your Lift project, update your `libraryDependencies` in `build.sbt` to include:

For *Lift 2.5.x* (Scala 2.9 and 2.10):

    "net.liftmodules" %% "omniauth_2.5" % "0.7"

For *Lift 3.0.x* (Scala 2.10):

    "net.liftmodules" %% "omniauth_3.0" % "0.7-SNAPSHOT"


## Providers

Lift-OmniAuth currently supports the following external providers:

* via OAuth
  * Facebook
  * Twitter
  * Github
