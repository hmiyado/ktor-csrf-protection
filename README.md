# ktor-csrf-protection

ktor-csrf-protection provides a Ktor plugin for server application to protect csrf by

- using CSRF Token
- verifying custom header

## Install

In build.gradle.kts

```kotlin
repositories {
    mavenCentral()
}
dependencies {
    implementation("io.github.hmiyado:ktor-csrf-protection:2.0.1")

    // if you want to use CSRF Token with Ktor Sessions plugin ( https://ktor.io/docs/sessions.html )
    implementation("io.ktor:ktor-server-sessions:$version")
}
```

## Example

```kotlin
data class ClientSession(
    val token: String
): CsrfTokenBoundClient {
    override val representation: String = token
}

install(Sessions) {
    val storage = SessionStorageMemory()
    // CSRF Token is bound to the client session.
    cookie<ClientSession>("client_session", storage = storage)
    // CSRF Token is handled as session.
    header<CsrfTokenSession>("X-CSRF-TOKEN", storage = storage)
}

install(Csrf) {
    requestFilter { httpMethod, path ->
        // specify request that csrf protection should be valid
        path == "/protected" && httpMethod in listOf(HttpMethod.Post)
    }
    // use csrf token with Ktor Sessions plugin
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
    post("/protected") { call.respond(HttpStatusCode.OK) }

    // CSRF Protection is invalid in below since it doesn't match requestFilter
    put("/protected") { call.respond(HttpStatusCode.OK) }
    post("/not-protected") { call.respond(HttpStatusCode.OK) }
}
```

In this example, `POST /protected` with HTTP Header `X-CSRF-TOKEN: {token}` is accepted.
CSRF Token is returned in first request to `POST /protected`.

The full process is below.

1. Request `POST /protected` without CSRF Token because client doesn't know it.
2. Respond 403 Forbidden with `X-CSRF-TOKEN: {token}` because client doesn't send valid csrf token.
3. Request `POST /protected` with `X-CSRF-TOKEN: {token}`. The token is get from the server response.
4. The request is accepted. 

See [sample application](./sample/README.md) for more detail.

## License

[MIT](./LICENSE)

## Acknowledgments

This library is created with reference to

- https://github.com/soywiz-archive/marshallpierce-ktor-csrf
- https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html
