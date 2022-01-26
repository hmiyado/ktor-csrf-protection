package com.github.hmiyado.csrfprotection

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.Headers

private typealias CsrfHeaderValidatorFunction = (headers: Headers) -> Boolean
private typealias HeaderCsrfOnFailFunction = suspend ApplicationCall.() -> Unit


class HeaderCsrfProvider private constructor(
    config: Configuration,
) : CsrfProvider(config) {
    val onFail: HeaderCsrfOnFailFunction = config.onFail
    val validator: CsrfHeaderValidatorFunction = config.validator

    class Configuration : CsrfProvider.Configuration() {
        var onFail: HeaderCsrfOnFailFunction = {}

        var validator: CsrfHeaderValidatorFunction = { _ -> false }

        fun validator(validator: CsrfHeaderValidatorFunction) {
            this.validator = validator
        }

        fun onFail(block: HeaderCsrfOnFailFunction) {
            onFail = block
        }

        fun buildProvider(): HeaderCsrfProvider {
            return HeaderCsrfProvider(this)
        }
    }
}

inline fun Csrf.Configuration.header(
    configure: HeaderCsrfProvider.Configuration.() -> Unit,
) {
    val provider = HeaderCsrfProvider
        .Configuration()
        .apply(configure)
        .buildProvider()

    provider.pipeline.intercept(CsrfPipeline.CheckCsrfToken) { context ->
        val isValid = provider.validator(call.request.headers)
        if (!isValid) {
            context.isValid = false
            provider.onFail(call)
        }
    }

    register(provider)
}
