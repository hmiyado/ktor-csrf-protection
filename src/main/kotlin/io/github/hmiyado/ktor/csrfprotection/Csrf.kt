package io.github.hmiyado.ktor.csrfprotection

import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase

class Csrf(configuration: Configuration) {
    private val providers: List<CsrfProvider> = configuration.providers.toList()
    private val requestFilter: CsrfRequestFilterFunction = configuration.requestFilter

    fun intercept(
        pipeline: ApplicationCallPipeline,
    ) {
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Plugins, CsrfPhase)

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

    companion object Plugin : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, Csrf> {
        val CsrfPhase = PipelinePhase("Csrf")

        override val key: AttributeKey<Csrf>
            get() = AttributeKey("Csrf")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Csrf {
            val configuration = Configuration().apply(configure)
            val plugin = Csrf(configuration)

            plugin.intercept(pipeline)

            return plugin
        }

    }
}

private typealias CsrfRequestFilterFunction = (httpMethod: HttpMethod, path: String) -> Boolean
