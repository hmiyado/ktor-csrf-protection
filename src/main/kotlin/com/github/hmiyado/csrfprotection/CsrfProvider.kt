package com.github.hmiyado.csrfprotection

import org.slf4j.Logger
import org.slf4j.LoggerFactory


open class CsrfProvider(
    configuration: Configuration,
) {
    val pipeline: CsrfPipeline = CsrfPipeline(developmentMode = configuration.pipeline.developmentMode)
    val logger: Logger = configuration.logger

    open class Configuration {
        val pipeline: CsrfPipeline = CsrfPipeline(developmentMode = false)
        var logger: Logger = LoggerFactory.getLogger("Application")
    }
}
