# Lift-Omniauth Module
## Usage
in `Boot.scala`

```scala
import omniauth.lib._
...
//Add Omniauth to the sitemap
Omniauth.sitemap
//init
//Supply a list of providers
Omniauth.initWithProviders(List(new FacebookProvider("key", "secret")))
//or init with providers in properties
Omniauth.init
```


define your client id (key) and secret for each provider you will use in your props file(s) or as JVM system properties

```
omniauth.facebooksecret=...
omniauth.facebookkey=...
```

set the base URL for your application

```
omniauth.baseurl=http://localhost:8080/
```

set the success and failure URLs

```
omniauth.successurl=/
omniauth.failureurl=/error
```

For Facebook provider you can set permissions. For example:

```
omniauth.facebookpermissions=email,read_stream
```
    
Redirect the user to the auth URL in your application:

```scala
S.redirectTo("/auth/facebook/signin")
```
    
You can optionally specify where the user should return to after successful authentication:

```scala
S.redirectTo("/auth/facebook/signin?returnTo=%2Ftimeline%3FshowComments%3Dtrue")
```
    
After a user has logged into an auth provider you can access data through the session var Omniauth.currentAuth

```scala
Omniauth.currentAuth match {
  case Full(auth:AuthInfo) => 
  case _ =>
}
```

You can also use obtain a user's unique ID from a provider without using sessions using Omniauth.tokenToId(provider:String, token:String): Box[String]

```scala
Omniauth.tokenToId("facebook", token) match {
  case Full(uid) => user.doSomethingWithFacebookId(uid)
}
```

Finally redirect the user back to the appropriate place in the application, using the optional `returnTo` variable:

```scala
val url = Omniauth.returnTo.openOr("/")
S.redirectTo(url)
```

## Installation

A big thank you to [jonoabroad](https://github.com/jonoabroad) for [hosting builds](https://liftmodules.ci.cloudbees.com/job/Omniauth%20Lift%20Module/) to make using much easier.

To include this module in your Lift project, update your `libraryDependencies` in `build.sbt` to include:

```scala
libraryDependencies ++= {
  val liftEdition = "2.5" // Also supported: "2.6" and "3.0"

  Seq(
    // Other dependencies ...
    "net.liftmodules" %% ("omniauth_"+liftEdition) % "0.13" % "compile"
  )
}
```

## Supported Versions

**Lift-OmniAuth** is built and released to support Lift edition 2.5 with Scala versions 2.9.1, 2.9.1-1, 2.9.2, and 2.10; Lift edition 2.6 with Scala versions 2.9.1, 2.9.1-1, 2.9.2, 2.10, 2.11; and Lift edition 3.0 with Scala version 2.10.  This project's scala version is purposefully set at the lowest common denominator to ensure each version compiles.

    
## Providers

**Lift-OmniAuth** currently supports the following external providers:

* via OAuth
  * Facebook
  * Twitter
  * Github
  * MSLive
  * Dropbox
  * Linkedin
  * Google+
  * Instagram
  * VK

## Tutorials
[Joe Barnes](https://github.com/joescii) has written [Dropbox tutorial](http://proseand.co.nz/2014/01/20/integrating-dropbox-into-a-lift-app/).
