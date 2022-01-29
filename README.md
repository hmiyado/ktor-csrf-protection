# ktor-csrf-protection

ktor-csrf-protection provides a Ktor feature for server application to protect csrf by

- using CSRF Token
- verifying custom header

## Install

In build.gradle.kts

```kotlin
repositories {
    mavenCentral()
}
dependencies {
    implementation("io.github.hmiyado:ktor-csrf-protection:{version}")

    // if you want to use CSRF Token with Ktor Sessions feature ( https://ktor.io/docs/sessions.html )
    implementation("io.ktor:ktor-server-sessions:{version}")
}
```

## Example

```kotlin
install(Sessions) {
    // you can use any session, cookie or header and so on.
    header<CsrfTokenSession>("X-CSRF-TOKEN", storage = SessionStorage())
}

install(Csrf) {
    requestFilter { httpMethod, path ->
        // specify request that csrf protection should be valid
        path == "/protected" && httpMethod in listOf(HttpMethod.Post)
    }
    // use csrf token with Ktor Sessions feature
    session<ClientSession> {
        onFail { respond(HttpStatusCode.Forbidden) }
    }
    // verify custom header
    header {
        validator { headers ->
            // validate custom HTTP Header 
            headers.entries().any { (k, _) -> k.uppercase() == "X-CSRF-TOKEN" }
        }
        onFail { respond(HttpStatusCode.Forbidden) }
    }
}

routing {
    // CSRF Protection is valid in POST /protected since it matches requestFilter 
    post("/protected")

    // CSRF Protection is invalid in below since it doesn't match requestFilter
    put("/protected")
    post("/not-protected")
}
```

In this example, `POST /protected` with HTTP Header `X-CSRF-TOKEN: {token}` is accepted.
CSRF Token is returned in first request to `POST /protected`.

The full process is below.

1. Request `POST /protected` without CSRF Token because client doesn't know it.
2. Respond 403 Forbidden with `X-CSRF-Token: {token}` because client doesn't send valid csrf token.
3. Request `POST /protected` without `X-CSRF-Token: {token}`. The token is get from the server response.
4. The request is accepted. 

## License

[MIT](./LICENSE)

## Acknowledgments

This library is created with reference to

- https://github.com/soywiz-archive/marshallpierce-ktor-csrf
- https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html
