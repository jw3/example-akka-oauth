Example OAuth with Akka
====

Playing around with OAuth providers and clients using Akka


#### Simple test

1. Start `com.github.jw3.oauth.Boot`
2. Obtain a new token from the `Provider`
  `curl -XPOST -F grant_type=client_credentials -F client_id=foo -F client_secret=bar localhost:8080/oauth/access_token`
3. Use the token via the `Client`
  `curl -H 'Authorization: Bearer 0efd95f' localhost:8081/closed`
