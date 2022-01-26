package com.github.hmiyado.csrfprotection

import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest

fun TestApplicationEngine.get(
    uri: String,
    setup: TestApplicationRequest.() -> Unit = {},
) = handleRequest(HttpMethod.Get, uri, setup)

fun TestApplicationEngine.post(
    uri: String,
    setup: TestApplicationRequest.() -> Unit = {},
) = handleRequest(HttpMethod.Post, uri, setup)
