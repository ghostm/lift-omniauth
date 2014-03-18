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


define your client id (key) and secret for each provider you will use in your props file(s) or as JVM system properties

    omniauth.facebooksecret=...
    omniauth.facebookkey=...
    
set the base URL for your application

    omniauth.baseurl=http://localhost:8080/

set the success and failure URLs

    omniauth.successurl=/
    omniauth.failureurl=/error

For Facebook provider you can set permissions. For example:

    omniauth.facebookpermissions=email,read_stream
    
Redirect the user to the auth URL in your application:

    S.redirectTo("/auth/facebook/signin")
    
You can optionally specify where the user should return to after successful authentication:

    S.redirectTo("/auth/facebook/signin?returnTo=%2Ftimeline%3FshowComments%3Dtrue")
    
After a user has logged into an auth provider you can access data through the session var Omniauth.currentAuth

    Omniauth.currentAuth match {
      case Full(auth:AuthInfo) => 
      case _ =>
    }

You can also use obtain a user's unique ID from a provider without using sessions using Omniauth.tokenToId(provider:String, token:String): Box[String]

    Omniauth.tokenToId("facebook", token) match {
      case Full(uid) => user.doSomethingWithFacebookId(uid)
    }

Finally redirect the user back to the appropriate place in the application, using the optional `returnTo` variable:

    val url = Omniauth.returnTo.openOr("/")
    S.redirectTo(url)

## Installation

A big thank you to [jonoabroad](https://github.com/jonoabroad) for [hosting builds](https://liftmodules.ci.cloudbees.com/job/Omniauth%20Lift%20Module/) to make using much easier.

To include this module in your Lift project, update your `libraryDependencies` in `build.sbt` to include:

For *Lift 2.5.x* (Scala 2.9 and 2.10):

    "net.liftmodules" %% "omniauth_2.5" % "0.10"

For *Lift 3.0.x* (Scala 2.10):

    "net.liftmodules" %% "omniauth_3.0" % "0.10"

    
## Providers

Lift-OmniAuth currently supports the following external providers:

* via OAuth
  * Facebook
  * Twitter
  * Github
  * MSLive
  * Dropbox
  * Linkedin
  * Google+
  * Instagram

## Tutorials
[Joe Barnes](https://github.com/barnesjd) has written [Dropbox tutorial](http://proseand.co.nz/2014/01/20/integrating-dropbox-into-a-lift-app/).