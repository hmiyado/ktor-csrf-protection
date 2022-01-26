package com.github.hmiyado.csrfprotection

import io.ktor.application.ApplicationCall
import io.ktor.util.pipeline.Pipeline
import io.ktor.util.pipeline.PipelinePhase

class CsrfPipeline(
    override val developmentMode: Boolean = false,
) : Pipeline<CsrfContext, ApplicationCall>(CheckCsrfToken, RequestCsrfToken) {
    companion object {
        val CheckCsrfToken: PipelinePhase = PipelinePhase("CheckCsrfToken")

        val RequestCsrfToken: PipelinePhase = PipelinePhase("RequestCsrfToken")
    }
}
