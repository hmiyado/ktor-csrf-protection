package com.github.hmiyado.csrfprotection

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase

class Csrf(configuration: Configuration) {
    private val providers: List<CsrfProvider> = configuration.providers.toList()
    private val requestFilter: CsrfRequestFilterFunction = configuration.requestFilter

    fun intercept(
        pipeline: ApplicationCallPipeline,
    ) {
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Features, CsrfPhase)

        pipeline.intercept(CsrfPhase) {
            val context = CsrfContext(call)
            val shouldCheckCsrfToken = call.request.let { requestFilter(it.httpMethod, it.path()) }
            if (shouldCheckCsrfToken) {
                for (provider in providers) {
                    provider.pipeline.execute(call, context)
                    if (context.isValid == false) {
                        this.finish()
                        break
                    }
                }
            }
        }
    }

    class Configuration {
        var providers: MutableList<CsrfProvider> = mutableListOf()

        var requestFilter: CsrfRequestFilterFunction = { _, _ -> false }

        fun register(provider: CsrfProvider) {
            providers.add(provider)
        }

        fun requestFilter(filter: CsrfRequestFilterFunction) {
            requestFilter = filter
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, Csrf> {
        val CsrfPhase = PipelinePhase("Csrf")

        override val key: AttributeKey<Csrf>
            get() = AttributeKey("Csrf")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Csrf {
            val configuration = Configuration().apply(configure)
            val feature = Csrf(configuration)

            feature.intercept(pipeline)

            return feature
        }

    }
}

private typealias CsrfRequestFilterFunction = (httpMethod: HttpMethod, path: String) -> Boolean
