package com.github.hmiyado.csrfprotection

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.sessions.clear
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set

typealias CsrfOnFailFunction = suspend ApplicationCall.(CsrfTokenSession?) -> Unit

class SessionCsrfProvider private constructor(
    config: Configuration,
) : CsrfProvider(config) {
    val onFail: CsrfOnFailFunction = config.onFail

    class Configuration : CsrfProvider.Configuration() {
        var onFail: CsrfOnFailFunction = {}

        fun onFail(block: CsrfOnFailFunction) {
            onFail = block
        }

        fun buildProvider(): SessionCsrfProvider {
            return SessionCsrfProvider(this)
        }
    }
}

inline fun <reified Client : CsrfTokenBoundClient> Csrf.Configuration.session(
    configure: SessionCsrfProvider.Configuration.() -> Unit,
) {
    val provider = SessionCsrfProvider
        .Configuration()
        .apply(configure)
        .buildProvider()
    val logger = provider.logger

    provider.pipeline.intercept(CsrfPipeline.CheckCsrfToken) { context ->
        val clientSession = call.sessions.get<Client>()
        val tokenSession = call.sessions.get<CsrfTokenSession>()
        logger.debug("CheckCsrfToken clientRepresentation={}", clientSession?.representation)
        logger.debug("CheckCsrfToken TokenSession={}", tokenSession)

        if (clientSession == null) {
            context.isValid = false
            provider.onFail(call, null)
            return@intercept
        }
        if (tokenSession?.associatedClientRepresentation == clientSession.representation) {
            return@intercept
        }
        val newTokenSession = CsrfTokenSession(clientSession)
        logger.debug("CheckCsrfToken newToken={}", newTokenSession)
        call.sessions.clear<CsrfTokenSession>()
        call.sessions.set(newTokenSession)
        context.isValid = false
        provider.onFail(call, newTokenSession)
    }

    register(provider)
}

interface CsrfTokenBoundClient {
    val representation: String
}

open class CsrfTokenSession(
    val associatedClientRepresentation: String,
) {
    constructor(client: CsrfTokenBoundClient) : this(client.representation)

    override fun toString(): String {
        return "CsrfTokenSession(associatedClient=$associatedClientRepresentation)"
    }
}
