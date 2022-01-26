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
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.sessions.SessionStorage
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.header
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify

class SessionCsrfTest : DescribeSpec() {

    lateinit var testApplicationEngine: TestApplicationEngine

    @MockK
    lateinit var onFailFunction: (CsrfTokenSession?) -> Unit

    @MockK
    lateinit var sessionStorage: SessionStorage

    override fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        MockKAnnotations.init()
        testApplicationEngine = TestApplicationEngine().apply {
            start()
            application.install(Routing) {
                get("/") { call.respond(HttpStatusCode.OK) }
                get("/not-csrf") { call.respond(HttpStatusCode.OK) }
                post("/") { call.respond(HttpStatusCode.OK) }
                post("/not-csrf") { call.respond(HttpStatusCode.OK) }
            }
            application.install(Sessions) {
                cookie<ClientSession>("client_session", storage = sessionStorage) {
                }
                header<CsrfTokenSession>("X-CSRF-TOKEN", storage = sessionStorage) {
                }
            }
            application.install(Csrf) {
                requestFilter { httpMethod, path ->
                    httpMethod == HttpMethod.Post && path == "/"
                }
                session<ClientSession> {
                    onFail { token ->
                        onFailFunction(token)
                    }
                }
            }
        }
    }

    override fun afterTest(testCase: TestCase, result: TestResult) {
        super.afterTest(testCase, result)
        clearAllMocks()
        testApplicationEngine.stop(0, 0)
    }

    init {
        MockKAnnotations.init(this)

        describe("request filter") {
            it("should not check csrf request that doesn't match HttpMethod") {
                testApplicationEngine.get("/").run {
                    response shouldHaveStatus HttpStatusCode.OK
                }
            }
            it("should not check csrf request that doesn't match path") {
                testApplicationEngine.post("/not-csrf").run {
                    response shouldHaveStatus HttpStatusCode.OK
                }
            }
            it("should not check csrf request that doesn't match path and method") {
                testApplicationEngine.get("/not-csrf").run {
                    response shouldHaveStatus HttpStatusCode.OK
                }
            }
        }
        describe("no or invalid client session, no or invalid csrf token") {
            it("should fail with no client session") {
                coEvery {
                    sessionStorage.read<ClientSession>(
                        any(),
                        any()
                    )
                } throws NoSuchElementException("client_session")
                coEvery {
                    sessionStorage.read<CsrfTokenSession>(
                        any(),
                        any()
                    )
                } throws NoSuchElementException("X-CSRF-TOKEN")
                every { onFailFunction(any()) } just Runs

                testApplicationEngine.post("/")
                verify { onFailFunction.invoke(null) }
            }
            it("should fail with no client session but only empty cookie") {
                coEvery {
                    sessionStorage.read<ClientSession>(
                        any(),
                        any()
                    )
                } throws NoSuchElementException("client_session")
                coEvery {
                    sessionStorage.read<CsrfTokenSession>(
                        any(),
                        any()
                    )
                } throws NoSuchElementException("X-CSRF-TOKEN")
                every { onFailFunction(any()) } just Runs

                testApplicationEngine.post("/") {
                    addHeader("Cookie", "client_session=")
                }
                verify { onFailFunction.invoke(null) }
            }
        }
        describe("valid client session, no or invalid csrf token") {
            it("should fail with valid client session but no csrf token") {
                val clientSession = ClientSession("session")
                coEvery { sessionStorage.read<ClientSession>(clientSession.value, any()) } returns clientSession
                coEvery {
                    sessionStorage.read<CsrfTokenSession>(
                        not(clientSession.value),
                        any()
                    )
                } throws NoSuchElementException("X-CSRF-TOKEN")
                coEvery { sessionStorage.write(any(), any()) } just Runs
                every { onFailFunction(any()) } just Runs

                testApplicationEngine.post("/") {
                    addHeader("Cookie", "client_session=${clientSession.value}")
                }
                verify { onFailFunction.invoke(ofType()) }
            }
            it("should fail with valid client session but invalid csrf token") {
                val clientSession = ClientSession("session")
                val csrfToken = "invalid_csrf_token"
                val csrfTokenSession = CsrfTokenSession(ClientSession("invalid_session"))
                coEvery { sessionStorage.read<ClientSession>(clientSession.value, any()) } returns clientSession
                coEvery {
                    sessionStorage.read<CsrfTokenSession>(
                        csrfToken,
                        any()
                    )
                } returns csrfTokenSession
                coEvery { sessionStorage.write(any(), any()) } just Runs
                every { onFailFunction(any()) } just Runs

                testApplicationEngine.post("/") {
                    addHeader("Cookie", "client_session=${clientSession.value}")
                    addHeader("X-CSRF-Token", csrfToken)
                }
                verify { onFailFunction.invoke(ofType()) }
            }
        }
        describe("valid client session, valid csrf token") {
            it("should be success with valid csrf token session, valid client session") {
                val clientSession = ClientSession("session")
                val csrfToken = "csrf-token"
                val expected = CsrfTokenSession(clientSession)
                coEvery { sessionStorage.read<ClientSession>(clientSession.value, any()) } returns clientSession
                coEvery { sessionStorage.read<CsrfTokenSession>(csrfToken, any()) } returns expected
                coEvery { sessionStorage.write(any(), any()) } just Runs
                every { onFailFunction(any()) } just Runs

                testApplicationEngine.post("/") {
                    addHeader("Cookie", "client_session=${clientSession.value}")
                    addHeader("X-CSRF-TOKEN", csrfToken)
                }.run {
                    response shouldHaveStatus HttpStatusCode.OK
                }
                verify(exactly = 0) { onFailFunction.invoke(any()) }
            }
        }
    }

    data class ClientSession(
        val value: String,
    ) : CsrfTokenBoundClient {
        override val representation: String
            get() = value
    }

}
