package com.github.hmiyado.csrfprotection

import io.kotest.assertions.ktor.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify

class HeaderCsrfProviderTest : DescribeSpec() {
    lateinit var testApplicationEngine: TestApplicationEngine

    @MockK
    lateinit var onFailFunction: () -> Unit

    override fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        MockKAnnotations.init()
        testApplicationEngine = TestApplicationEngine().apply {
            start()
            application.routing {
                post("/") { call.respond(HttpStatusCode.OK) }
            }
            application.install(Csrf) {
                requestFilter { httpMethod, _ -> listOf(HttpMethod.Post).contains(httpMethod) }
                header {
                    validator { headers -> headers.contains("X-CSRF-TOKEN") }
                    onFail {
                        onFailFunction()
                        respond(HttpStatusCode.Forbidden)
                    }
                }
            }
        }

    }

    override fun afterTest(testCase: TestCase, result: TestResult) {
        super.afterTest(testCase, result)
        clearAllMocks()
    }

    init {
        MockKAnnotations.init(this)

        describe("HeaderCsrf") {
            it("should succeed with valid csrf header") {
                testApplicationEngine.post("/") {
                    addHeader("X-CSRF-Token", "")
                }.run {
                    response shouldHaveStatus HttpStatusCode.OK
                }
            }
            it("should fail without valid csrf header") {
                every { onFailFunction.invoke() } just Runs
                testApplicationEngine.post("/").run {
                    response shouldHaveStatus HttpStatusCode.Forbidden
                    verify { onFailFunction.invoke() }
                }
            }
        }
    }

}
