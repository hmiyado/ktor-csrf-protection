package io.github.hmiyado.ktor.csrfprotection.sample

import io.github.hmiyado.ktor.csrfprotection.Csrf
import io.github.hmiyado.ktor.csrfprotection.CsrfTokenBoundClient
import io.github.hmiyado.ktor.csrfprotection.CsrfTokenSession
import io.github.hmiyado.ktor.csrfprotection.header
import io.github.hmiyado.ktor.csrfprotection.session
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationReceivePipeline
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.routing
import io.ktor.sessions.SessionStorageMemory
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.get
import io.ktor.sessions.header
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import org.slf4j.LoggerFactory
import java.util.UUID

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
): CsrfTokenBoundClient {
    override val representation: String = token
}
