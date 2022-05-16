package io.github.hmiyado.ktor.csrfprotection.sample

import io.github.hmiyado.ktor.csrfprotection.Csrf
import io.github.hmiyado.ktor.csrfprotection.CsrfTokenBoundClient
import io.github.hmiyado.ktor.csrfprotection.CsrfTokenSession
import io.github.hmiyado.ktor.csrfprotection.header
import io.github.hmiyado.ktor.csrfprotection.session
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.header
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

fun Application.module() {
    install(Sessions) {
        val storage = SessionStorageMemory()
        cookie<ClientSession>("client_session", storage = storage)
        header<CsrfTokenSession>("X-CSRF-TOKEN", storage = storage)
    }

    install(Csrf) {
        requestFilter { httpMethod, path ->
            path == "/protected" && httpMethod in listOf(HttpMethod.Post)
        }
        session<ClientSession> {
            onFail {
                val clientSession = sessions.get<ClientSession>()
                if (clientSession == null) {
                    sessions.set(ClientSession("client"))
                }
                respond(HttpStatusCode.Forbidden)
            }
        }
        header {
            validator { headers ->
                headers.entries().any { (k, _) -> k.uppercase() == "X-CSRF-TOKEN" }
            }
            onFail {
                val clientSession = sessions.get<ClientSession>()
                if (clientSession == null) {
                    sessions.set(ClientSession("client"))
                }
                respond(HttpStatusCode.Forbidden)
            }
        }
    }

    routing {
        // required csrf protection
        post("/protected") { call.respond(HttpStatusCode.OK) }

        // not required csrf protection because requestFilter doesn't match
        put("/protected") { call.respond(HttpStatusCode.OK) }
        post("/not-protected") { call.respond(HttpStatusCode.OK) }
    }
}

data class ClientSession(
    val token: String
) : CsrfTokenBoundClient {
    override val representation: String = token
}
